package org.sakos.camera.safety.opennsfw2

import android.graphics.Bitmap
import org.sakos.camera.safety.core.SafetyDecision
import org.sakos.camera.safety.core.SafetyEvaluationOutcome
import org.sakos.camera.safety.core.SafetyEvaluationReceiptId
import org.sakos.camera.safety.core.SafetyEvaluationRequest
import org.sakos.camera.safety.core.SafetyEvaluator
import org.sakos.camera.safety.core.SafetyFailureReason

/** Adapts a verified [OpenNsfw2BitmapRuntime] to the SDK's fail-closed evaluator contract. */
class OpenNsfw2BitmapEvaluator(
    private val runtime: OpenNsfw2BitmapRuntime,
) : SafetyEvaluator<Bitmap> {
    @Volatile
    private var closed = false

    override suspend fun evaluate(request: SafetyEvaluationRequest<Bitmap>): SafetyEvaluationOutcome {
        if (closed) return failure(request, SafetyFailureReason.EvaluatorClosed, "The OpenNSFW2 evaluator has been closed.")
        if (request.configuration != OpenNsfw2ModelPreflight.configuration) {
            return failure(request, SafetyFailureReason.InvalidInput, "The request configuration does not match the bundled model.")
        }

        return try {
            val evaluation = runtime.evaluate(request.input)
            SafetyEvaluationOutcome.Decision(
                captureId = request.capture.captureId,
                receiptId = SafetyEvaluationReceiptId("${request.capture.captureId.value}:opennsfw2"),
                configuration = request.configuration,
                decision = if (evaluation.checkResult.isSafe) SafetyDecision.Allow else SafetyDecision.Block,
                rationale = listOf(evaluation.checkResult.reason),
            )
        } catch (failure: IllegalArgumentException) {
            failure(request, SafetyFailureReason.InvalidInput, failure.message)
        } catch (failure: IllegalStateException) {
            failure(request, SafetyFailureReason.InferenceFailure, failure.message)
        } catch (failure: RuntimeException) {
            failure(request, SafetyFailureReason.InferenceFailure, failure.message)
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        runtime.close()
    }

    private fun failure(
        request: SafetyEvaluationRequest<Bitmap>,
        reason: SafetyFailureReason,
        detail: String?,
    ) = SafetyEvaluationOutcome.Failure(
        captureId = request.capture.captureId,
        configuration = request.configuration,
        reason = reason,
        detail = detail,
    )
}
