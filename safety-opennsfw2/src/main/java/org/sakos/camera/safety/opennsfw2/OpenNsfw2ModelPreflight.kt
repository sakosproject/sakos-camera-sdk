package org.sakos.camera.safety.opennsfw2

import java.io.InputStream
import java.security.MessageDigest
import org.sakos.camera.safety.core.SafetyComponentVersion
import org.sakos.camera.safety.core.SafetyConfigurationVersion
import org.sakos.camera.safety.core.SafetyEvaluationOutcome
import org.sakos.camera.safety.core.SafetyEvaluationRequest
import org.sakos.camera.safety.core.SafetyEvaluator
import org.sakos.camera.safety.core.SafetyFailureReason

/** The expected contract for the bundled model asset. */
object OpenNsfw2ModelPreflight {
    const val assetPath = "model/sakos_nudity_model.tflite"
    const val expectedByteCount = 23_608_404L
    const val expectedSha256 = "BEA35DC93C86F074AE9A047638773AFF9EB84C05E6EAD8D785AF5C8DDDE05518"
    val inputShape = listOf(1, 224, 224, 3)
    val outputShape = listOf(1, 2)
    val bgrMeanSubtraction = listOf(104, 117, 123)

    val configuration: SafetyConfigurationVersion = SafetyConfigurationVersion(
        model = SafetyComponentVersion(OpenNsfw2ModelContract.modelId, "bea35dc93c86f074"),
        preprocessing = SafetyComponentVersion("opennsfw2-bgr-mean-104-117-123", "1"),
        policy = SafetyComponentVersion("opennsfw2-still-policy", "1"),
    )

    /** The original default identity is preserved; alternate execution has its own policy identity. */
    fun configurationFor(strategy: IntegratedOpenNsfw2Strategy): SafetyConfigurationVersion = when (strategy) {
        IntegratedOpenNsfw2Strategy.Fixed14 -> configuration
        IntegratedOpenNsfw2Strategy.Adaptive14 -> configuration.copy(
            policy = SafetyComponentVersion("opennsfw2-still-policy-adaptive14", "1"))
    }

    fun verify(asset: InputStream?): OpenNsfw2ModelAssetStatus {
        if (asset == null) return OpenNsfw2ModelAssetStatus.Missing

        return asset.use { stream ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var byteCount = 0L
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                byteCount += read
                digest.update(buffer, 0, read)
            }

            if (byteCount != expectedByteCount) {
                return@use OpenNsfw2ModelAssetStatus.UnexpectedSize(byteCount)
            }

            val actualSha256 = digest.digest().joinToString(separator = "") { byte ->
                "%02X".format(byte)
            }
            if (!actualSha256.equals(expectedSha256, ignoreCase = true)) {
                OpenNsfw2ModelAssetStatus.HashMismatch(actualSha256)
            } else {
                OpenNsfw2ModelAssetStatus.Verified
            }
        }
    }
}

sealed interface OpenNsfw2ModelAssetStatus {
    data object Missing : OpenNsfw2ModelAssetStatus
    data class UnexpectedSize(val actualByteCount: Long) : OpenNsfw2ModelAssetStatus
    data class HashMismatch(val actualSha256: String) : OpenNsfw2ModelAssetStatus
    data object Verified : OpenNsfw2ModelAssetStatus
}

/**
 * Fail-closed evaluator for callers that need an explicit integrity result before opening the runtime.
 * It is intentionally unable to return an Allow outcome.
 */
class OpenNsfw2ModelPreflightEvaluator<Input : Any>(
    private val assetStatus: () -> OpenNsfw2ModelAssetStatus,
) : SafetyEvaluator<Input> {
    @Volatile
    private var closed = false

    override suspend fun evaluate(
        request: SafetyEvaluationRequest<Input>,
    ): SafetyEvaluationOutcome.Failure {
        val status = assetStatus()
        val reason = when {
            closed -> SafetyFailureReason.EvaluatorClosed
            status == OpenNsfw2ModelAssetStatus.Missing -> SafetyFailureReason.ModelUnavailable
            status == OpenNsfw2ModelAssetStatus.Verified -> SafetyFailureReason.ModelUnavailable
            else -> SafetyFailureReason.ModelIntegrityFailure
        }
        return SafetyEvaluationOutcome.Failure(
            captureId = request.capture.captureId,
            configuration = request.configuration,
            reason = reason,
            detail = when (reason) {
                SafetyFailureReason.ModelUnavailable -> "The verified OpenNSFW2 model asset or its runtime is unavailable."
                SafetyFailureReason.ModelIntegrityFailure -> "The bundled OpenNSFW2 model asset did not pass preflight."
                SafetyFailureReason.EvaluatorClosed -> "The OpenNSFW2 evaluator has been closed."
                else -> error("Unexpected preflight failure reason: $reason")
            },
        )
    }

    override fun close() {
        closed = true
    }
}
