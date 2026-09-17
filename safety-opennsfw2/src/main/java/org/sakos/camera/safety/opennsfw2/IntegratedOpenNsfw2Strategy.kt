/*
 * Selectively extracted from the user-authorized camera/gallery application source
 * at historical source revision omitted. See docs/EXTRACTION_MANIFEST.md.
 */
package org.sakos.camera.safety.opennsfw2

enum class IntegratedOpenNsfw2Strategy(
    val id: String,
    val displayName: String,
    val strategyLabel: String,
) {
    Fixed14(
        id = "fixed14",
        displayName = "Fixed 14-view",
        strategyLabel = "Full frame + center crop + torso and chest-focused portrait crops + overlapping half-frame tiles with early block short-circuit and stronger corroborated crop blocking",
    ),
    Adaptive14(
        id = "adaptive14",
        displayName = "Adaptive 14-view",
        strategyLabel = "Adaptive staged OpenNSFW2 still-photo evaluation: Stage 1 full/context views plus easy portrait-safe exit and portrait-side sentinel probes, Stage 2 portrait ambiguity crops plus strongest-crop lower-lateral refinement and targeted torso/lower-corner crops, and Stage 3 fallback to the remaining 14-view windows only for ambiguous cases",
    ),
    ;

    companion object {
        fun fromDebugToken(token: String?): IntegratedOpenNsfw2Strategy? {
            val normalized = token?.trim()?.lowercase() ?: return null
            return when (normalized) {
                "fixed14",
                "fixed-14",
                "fixed_14",
                "fixed",
                "baseline",
                "14",
                "14-view",
                "14_view",
                -> Fixed14

                "adaptive14",
                "adaptive-14",
                "adaptive_14",
                "adaptive",
                -> Adaptive14

                else -> null
            }
        }
    }
}


