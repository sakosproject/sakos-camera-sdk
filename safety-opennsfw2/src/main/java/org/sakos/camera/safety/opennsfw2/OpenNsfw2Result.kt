package org.sakos.camera.safety.opennsfw2

/**
 * Model-specific result metadata used by spatial policy before the model runtime is available.
 * It deliberately has no host-trust or capture-save semantics; those belong to safety-core and
 * the managed capture modules.
 */
data class OpenNsfw2CheckResult(
    val isSafe: Boolean,
    val confidence: Float,
    val reason: String,
    val modelId: String,
) {
    val nsfwProbability: Float
        get() = confidence

    companion object {
        fun safe(confidence: Float, reason: String, modelId: String) = OpenNsfw2CheckResult(
            isSafe = true,
            confidence = confidence,
            reason = reason,
            modelId = modelId,
        )

        fun blocked(confidence: Float, reason: String, modelId: String) = OpenNsfw2CheckResult(
            isSafe = false,
            confidence = confidence,
            reason = reason,
            modelId = modelId,
        )
    }
}

/** Stable identity for the cleared policy code; it does not imply a bundled model asset. */
object OpenNsfw2ModelContract {
    const val modelId: String = "opennsfw2_resnet50_v1"
}
