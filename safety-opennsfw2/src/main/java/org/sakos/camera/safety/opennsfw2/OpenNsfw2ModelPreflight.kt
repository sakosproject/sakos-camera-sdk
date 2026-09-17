package org.sakos.camera.safety.opennsfw2

import java.io.InputStream
import java.security.MessageDigest
import org.sakos.camera.safety.core.SafetyComponentVersion
import org.sakos.camera.safety.core.SafetyConfigurationVersion
import org.sakos.camera.safety.core.SafetyEvaluationOutcome
import org.sakos.camera.safety.core.SafetyEvaluationRequest
import org.sakos.camera.safety.core.SafetyEvaluator
import org.sakos.camera.safety.core.SafetyFailureReason

/** The expected contract for the model asset; this does not include the asset itself. */
object OpenNsfw2ModelPreflight {
    const val assetPath = "model/sakos_nudity_model.tflite"
    const val expectedByteCount = 6_128_536L
    const val expectedSha256 = "051A21BF697858C1E2537354A99BE09A48D26BBFBA0C35216B340F16DE7528D7"
    val inputShape = listOf(1, 224, 224, 3)
    val outputShape = listOf(1, 2)
    val bgrMeanSubtraction = listOf(104, 117, 123)

    val configuration: SafetyConfigurationVersion = SafetyConfigurationVersion(
        model = SafetyComponentVersion(OpenNsfw2ModelContract.modelId, "unbundled"),
        preprocessing = SafetyComponentVersion("opennsfw2-bgr-mean-104-117-123", "1"),
        policy = SafetyComponentVersion("opennsfw2-still-policy", "1"),
    )

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
 * A temporary fail-closed evaluator used until a verified model can be bundled and executed.
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
