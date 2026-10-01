package org.sakos.camera.safety.opennsfw2

/**
 * Model-specific result metadata used by spatial policy and the bundled Bitmap runtime.
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

/** Stable policy/model identity; it does not establish legal redistribution clearance. */
object OpenNsfw2ModelContract {
    const val modelId: String = "opennsfw2_resnet50_v1"
}
