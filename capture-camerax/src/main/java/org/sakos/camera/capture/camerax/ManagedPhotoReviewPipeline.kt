package org.sakos.camera.capture.camerax

import androidx.camera.core.ImageProxy
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import org.sakos.camera.safety.core.ManagedCaptureApproval
import org.sakos.camera.safety.core.SafetyCaptureContext
import org.sakos.camera.safety.core.SafetyConfigurationVersion
import org.sakos.camera.safety.core.SafetyEvaluationOutcome
import org.sakos.camera.safety.core.SafetyEvaluationRequest
import org.sakos.camera.safety.core.SafetyEvaluator
import org.sakos.camera.safety.core.approvalForManagedCapture

interface InMemoryPhoto<Input : Any> : AutoCloseable { val input: Input; override fun close() }
interface ApprovedPhotoSink<Input : Any> { suspend fun save(input: Input, capture: SafetyCaptureContext, approval: ManagedCaptureApproval) }
sealed interface ManagedPhotoResult {
    data object AlreadyProcessing : ManagedPhotoResult
    data object AlreadyDelivered : ManagedPhotoResult
    data class NotApproved(val outcome: SafetyEvaluationOutcome) : ManagedPhotoResult
    data class Delivered(val approval: ManagedCaptureApproval) : ManagedPhotoResult
    data class OutputFailure(val error: Throwable) : ManagedPhotoResult
}

/** Evaluates in memory, closes the frame, and writes only after a matching Allow receipt. */
class ManagedPhotoReviewPipeline<Input : Any>(
    private val evaluator: SafetyEvaluator<Input>,
    private val sink: ApprovedPhotoSink<Input>,
    private val configuration: SafetyConfigurationVersion,
) {
    private val inFlight = ConcurrentHashMap.newKeySet<String>()
    private val delivered = ConcurrentHashMap.newKeySet<String>()

    suspend fun review(frame: InMemoryPhoto<Input>, capture: SafetyCaptureContext): ManagedPhotoResult {
        val id = capture.captureId.value
        if (delivered.contains(id)) { frame.close(); return ManagedPhotoResult.AlreadyDelivered }
        if (!inFlight.add(id)) { frame.close(); return ManagedPhotoResult.AlreadyProcessing }
        try {
            if (delivered.contains(id)) return ManagedPhotoResult.AlreadyDelivered
            coroutineContext.ensureActive()
            val outcome = evaluator.evaluate(SafetyEvaluationRequest(frame.input, capture, configuration))
            coroutineContext.ensureActive()
            if (outcome.configuration != configuration) return ManagedPhotoResult.NotApproved(
                SafetyEvaluationOutcome.Failure(capture.captureId, configuration,
                    org.sakos.camera.safety.core.SafetyFailureReason.InvalidModelOutput))
            val approval = outcome.approvalForManagedCapture(capture) ?: return ManagedPhotoResult.NotApproved(outcome)
            return try {
                sink.save(frame.input, capture, approval)
                delivered.add(id)
                ManagedPhotoResult.Delivered(approval)
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { ManagedPhotoResult.OutputFailure(error) }
        } finally {
            inFlight.remove(id)
            frame.close()
        }
    }

    suspend fun reviewImageProxy(image: ImageProxy, capture: SafetyCaptureContext, convert: (ImageProxy) -> Input): ManagedPhotoResult {
        val input = try { convert(image) } catch (error: Throwable) { image.close(); throw error }
        return review(object : InMemoryPhoto<Input> {
            override val input = input
            override fun close() = image.close()
        }, capture)
    }
}
