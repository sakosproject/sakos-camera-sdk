package org.sakos.camera.capture.camerax

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.sakos.camera.safety.core.*

class ManagedPhotoReviewPipelineTest {
    private val config = SafetyConfigurationVersion(SafetyComponentVersion("model", "1"), SafetyComponentVersion("prep", "1"), SafetyComponentVersion("policy", "1"))
    private val capture = SafetyCaptureContext(SafetyCaptureId("photo-1"), 1, 100, 200, 0, false)

    @Test fun nonAllowNeverWritesAndAlwaysCloses() {
        val frame = Frame("pixels")
        var writes = 0
        val pipeline = ManagedPhotoReviewPipeline(Evaluator(SafetyDecision.Block), Sink { writes++ }, config)
        assertIs<ManagedPhotoResult.NotApproved>(run { pipeline.review(frame, capture) })
        assertEquals(0, writes); assertEquals(1, frame.closes)
    }

    @Test fun allowWritesOnceAndDuplicateCannotWriteAgain() {
        var writes = 0
        val pipeline = ManagedPhotoReviewPipeline(Evaluator(SafetyDecision.Allow), Sink { writes++ }, config)
        assertIs<ManagedPhotoResult.Delivered>(run { pipeline.review(Frame("pixels"), capture) })
        assertIs<ManagedPhotoResult.AlreadyDelivered>(run { pipeline.review(Frame("pixels"), capture) })
        assertEquals(1, writes)
    }

    @Test fun sinkFailureIsVisibleAndFrameCloses() {
        val frame = Frame("pixels")
        val pipeline = ManagedPhotoReviewPipeline(Evaluator(SafetyDecision.Allow), Sink { error("disk full") }, config)
        assertIs<ManagedPhotoResult.OutputFailure>(run { pipeline.review(frame, capture) })
        assertEquals(1, frame.closes)
    }

    private class Frame(override val input: String) : InMemoryPhoto<String> { var closes = 0; override fun close() { closes++ } }
    private class Sink(val action: () -> Unit) : ApprovedPhotoSink<String> { override suspend fun save(input: String, capture: SafetyCaptureContext, approval: ManagedCaptureApproval) = action() }
    private class Evaluator(private val decision: SafetyDecision) : SafetyEvaluator<String> {
        override suspend fun evaluate(request: SafetyEvaluationRequest<String>) = SafetyEvaluationOutcome.Decision(request.capture.captureId, SafetyEvaluationReceiptId("r-${decision.name}"), request.configuration, decision)
        override fun close() = Unit
    }
    private fun <T> run(block: suspend () -> T): T { var value: Result<T>? = null; block.startCoroutine(object : Continuation<T> { override val context = EmptyCoroutineContext; override fun resumeWith(result: Result<T>) { value = result } }); return requireNotNull(value).getOrThrow() }
}
