package org.sakos.camera.capture.camerax

import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageInfo
import androidx.camera.core.ImageProxy
import android.graphics.Rect
import android.media.Image
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.sakos.camera.safety.core.SafetyCaptureContext
import org.sakos.camera.safety.core.SafetyCaptureId
import org.sakos.camera.safety.core.SafetyComponentVersion
import org.sakos.camera.safety.core.SafetyConfigurationVersion
import org.sakos.camera.safety.core.SafetyDecision
import org.sakos.camera.safety.core.SafetyEvaluationOutcome
import org.sakos.camera.safety.core.SafetyEvaluationReceiptId
import org.sakos.camera.safety.core.SafetyEvaluationRequest
import org.sakos.camera.safety.core.SafetyEvaluator

class ManagedPhotoCaptureCallbackTest {
    private val configuration = SafetyConfigurationVersion(
        SafetyComponentVersion("model", "1"),
        SafetyComponentVersion("prep", "1"),
        SafetyComponentVersion("policy", "1"),
    )
    private val capture = SafetyCaptureContext(SafetyCaptureId("callback-1"), 1, 100, 200, 0, false)

    @Test
    fun cameraErrorIsReportedWithoutCallingEvaluatorOrSink() {
        var evaluations = 0
        var saves = 0
        var received: ManagedPhotoCaptureCallbackResult? = null
        val callback = ManagedPhotoCaptureCallback(
            pipeline = ManagedPhotoReviewPipeline(
                evaluator = object : SafetyEvaluator<String> {
                    override suspend fun evaluate(request: SafetyEvaluationRequest<String>): SafetyEvaluationOutcome {
                        evaluations += 1
                        return SafetyEvaluationOutcome.Decision(request.capture.captureId, SafetyEvaluationReceiptId("receipt"), request.configuration, SafetyDecision.Allow)
                    }

                    override fun close() = Unit
                },
                sink = object : ApprovedPhotoSink<String> {
                    override suspend fun save(input: String, capture: SafetyCaptureContext, approval: org.sakos.camera.safety.core.ManagedCaptureApproval) {
                        saves += 1
                    }
                },
                configuration = configuration,
            ),
            capture = capture,
            convert = { "unused" },
            listener = ManagedPhotoCaptureListener { received = it },
        )

        callback.onError(ImageCaptureException(ImageCapture.ERROR_UNKNOWN, "camera failed", null))

        val failure = assertIs<ManagedPhotoCaptureCallbackResult.CaptureFailure>(received)
        assertEquals("camera failed", failure.error.message)
        assertEquals(0, evaluations)
        assertEquals(0, saves)
    }

    @Test
    fun successDelegatesToPipelineAndClosesProxyOnce() {
        var saves = 0
        var received: ManagedPhotoCaptureCallbackResult? = null
        val callback = ManagedPhotoCaptureCallback(
            pipeline = ManagedPhotoReviewPipeline(
                evaluator = allowEvaluator(),
                sink = object : ApprovedPhotoSink<String> {
                    override suspend fun save(input: String, capture: SafetyCaptureContext, approval: org.sakos.camera.safety.core.ManagedCaptureApproval) {
                        saves += 1
                    }
                },
                configuration = configuration,
            ),
            capture = capture,
            convert = { "pixels" },
            listener = ManagedPhotoCaptureListener { received = it },
        )
        val image = TestImageProxy()

        callback.onCaptureSuccess(image)

        assertIs<ManagedPhotoCaptureCallbackResult.Reviewed>(received)
        assertEquals(1, saves)
        assertEquals(1, image.closes)
    }

    @Test
    fun conversionAndEvaluatorFailuresEachCloseProxyExactlyOnce() {
        for (conversionFailure in listOf(true, false)) {
            val image = TestImageProxy()
            var saves = 0
            var received: ManagedPhotoCaptureCallbackResult? = null
            val evaluator = object : SafetyEvaluator<String> {
                override suspend fun evaluate(request: SafetyEvaluationRequest<String>): SafetyEvaluationOutcome = error("synthetic evaluator failure")
                override fun close() = Unit
            }
            val callback = ManagedPhotoCaptureCallback(
                ManagedPhotoReviewPipeline(evaluator, object : ApprovedPhotoSink<String> {
                    override suspend fun save(input: String, capture: SafetyCaptureContext, approval: org.sakos.camera.safety.core.ManagedCaptureApproval) { saves++ }
                }, configuration), capture, { if (conversionFailure) error("synthetic conversion failure") else "synthetic pixels" },
                ManagedPhotoCaptureListener { received = it })
            callback.onCaptureSuccess(image)
            assertIs<ManagedPhotoCaptureCallbackResult.PipelineFailure>(received)
            assertEquals(0, saves)
            assertEquals(1, image.closes)
        }
    }

    private fun allowEvaluator() = object : SafetyEvaluator<String> {
        override suspend fun evaluate(request: SafetyEvaluationRequest<String>) = SafetyEvaluationOutcome.Decision(
            request.capture.captureId,
            SafetyEvaluationReceiptId("receipt"),
            request.configuration,
            SafetyDecision.Allow,
        )

        override fun close() = Unit
    }

    private class TestImageProxy : ImageProxy {
        var closes = 0
        override fun close() { closes += 1 }
        override fun getCropRect(): Rect = error("not used")
        override fun setCropRect(rect: Rect?) {
            error("not used")
        }
        override fun getFormat(): Int = error("not used")
        override fun getHeight(): Int = error("not used")
        override fun getWidth(): Int = error("not used")
        override fun getPlanes(): Array<ImageProxy.PlaneProxy> = error("not used")
        override fun getImageInfo(): ImageInfo = error("not used")
        override fun getImage(): Image? = error("not used")
    }
}
