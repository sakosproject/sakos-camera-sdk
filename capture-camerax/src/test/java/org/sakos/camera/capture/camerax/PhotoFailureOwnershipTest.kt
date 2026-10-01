package org.sakos.camera.capture.camerax

import kotlin.test.*
import kotlinx.coroutines.*
import org.sakos.camera.safety.core.*

class PhotoFailureOwnershipTest {
    private val config = SafetyConfigurationVersion(SafetyComponentVersion("m", "1"), SafetyComponentVersion("p", "1"), SafetyComponentVersion("g", "1"))
    private val capture = SafetyCaptureContext(SafetyCaptureId("synthetic-photo"), 0, 32, 32, 0, false)
    private class Frame : InMemoryPhoto<String> {
        override val input = "synthetic-pattern"
        var closes = 0
        override fun close() { closes++ }
    }

    @Test fun mismatchedReceiptOrConfigurationCannotWrite() = runBlocking {
        for (wrongId in listOf(true, false)) {
            var writes = 0
            val frame = Frame()
            val evaluator = object : SafetyEvaluator<String> {
                override suspend fun evaluate(request: SafetyEvaluationRequest<String>) = SafetyEvaluationOutcome.Decision(
                    if (wrongId) SafetyCaptureId("other") else capture.captureId, SafetyEvaluationReceiptId("mock"),
                    if (wrongId) config else config.copy(model = SafetyComponentVersion("other", "2")), SafetyDecision.Allow)
                override fun close() = Unit
            }
            val pipeline = ManagedPhotoReviewPipeline(evaluator, object : ApprovedPhotoSink<String> {
                override suspend fun save(input: String, capture: SafetyCaptureContext, approval: ManagedCaptureApproval) { writes++ }
            }, config)
            assertIs<ManagedPhotoResult.NotApproved>(pipeline.review(frame, capture))
            assertEquals(0, writes)
            assertEquals(1, frame.closes)
        }
    }

    @Test fun cancellationAfterSimulatedAllowClosesWithoutWriting() = runBlocking {
        var writes = 0
        val frame = Frame()
        val job = Job()
        val evaluator = object : SafetyEvaluator<String> {
            override suspend fun evaluate(request: SafetyEvaluationRequest<String>): SafetyEvaluationOutcome {
                job.cancel()
                return SafetyEvaluationOutcome.Decision(capture.captureId, SafetyEvaluationReceiptId("mock"), config, SafetyDecision.Allow)
            }
            override fun close() = Unit
        }
        val pipeline = ManagedPhotoReviewPipeline(evaluator, object : ApprovedPhotoSink<String> {
            override suspend fun save(input: String, capture: SafetyCaptureContext, approval: ManagedCaptureApproval) { writes++ }
        }, config)
        assertFailsWith<CancellationException> { withContext(job) { pipeline.review(frame, capture) } }
        assertEquals(0, writes)
        assertEquals(1, frame.closes)
    }
}
