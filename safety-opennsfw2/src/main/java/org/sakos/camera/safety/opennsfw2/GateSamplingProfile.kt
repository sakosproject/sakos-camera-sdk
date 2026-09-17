/*
 * Selectively extracted from the user-authorized camera/gallery application source
 * at historical source revision omitted. See docs/EXTRACTION_MANIFEST.md.
 */
package org.sakos.camera.safety.opennsfw2

import java.util.Locale

enum class GateSamplingOrientation(
    val token: String,
    val displayLabel: String,
) {
    Portrait("portrait", "Portrait"),
    Landscape("landscape", "Landscape"),
    Square("square", "Square"),
}

enum class GateSamplingRatioFamily(
    val token: String,
    val displayLabel: String,
    val minRatioInclusive: Float?,
    val maxRatioInclusive: Float?,
    val selectorEligible: Boolean,
) {
    TallPhone(
        token = "tall-phone",
        displayLabel = "Tall phone",
        minRatioInclusive = 0.42f,
        maxRatioInclusive = 0.50f,
        selectorEligible = true,
    ),
    Standard16x9(
        token = "standard-16x9",
        displayLabel = "Standard 16:9",
        minRatioInclusive = 0.52f,
        maxRatioInclusive = 0.60f,
        selectorEligible = true,
    ),
    Classic4x3(
        token = "classic-4x3",
        displayLabel = "Classic 4:3",
        minRatioInclusive = 0.70f,
        maxRatioInclusive = 0.80f,
        selectorEligible = true,
    ),
    Square(
        token = "square",
        displayLabel = "Square",
        minRatioInclusive = 0.88f,
        maxRatioInclusive = 1.00f,
        selectorEligible = true,
    ),
    UnsupportedRatio(
        token = "unsupported-ratio",
        displayLabel = "Unsupported ratio",
        minRatioInclusive = null,
        maxRatioInclusive = null,
        selectorEligible = false,
    );

    fun contains(normalizedRatio: Float): Boolean {
        val min = minRatioInclusive ?: return false
        val max = maxRatioInclusive ?: return false
        return normalizedRatio >= min && normalizedRatio <= max
    }
}

data class GateSamplingProfile(
    val family: GateSamplingRatioFamily,
    val orientation: GateSamplingOrientation,
    val normalizedRatio: Float,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val metadataRotationDegrees: Int? = null,
) {
    val id: String
        get() = if (family == GateSamplingRatioFamily.UnsupportedRatio) {
            family.token
        } else {
            "${family.token}-${orientation.token}"
        }

    val displayLabel: String
        get() = "${family.displayLabel} ${orientation.displayLabel.lowercase(Locale.US)}"

    val selectorEligible: Boolean
        get() = family.selectorEligible

    val ratioLabel: String
        get() = String.format(Locale.US, "%.3f", normalizedRatio)

    companion object {
        fun detect(
            width: Int,
            height: Int,
            metadataRotationDegrees: Int? = null,
        ): GateSamplingProfile {
            val safeWidth = width.coerceAtLeast(1)
            val safeHeight = height.coerceAtLeast(1)
            val shortEdge = minOf(safeWidth, safeHeight)
            val longEdge = maxOf(safeWidth, safeHeight)
            val normalizedRatio = shortEdge.toFloat() / longEdge.toFloat()
            val family = GateSamplingRatioFamily.entries.firstOrNull { candidate ->
                candidate != GateSamplingRatioFamily.UnsupportedRatio &&
                    candidate.contains(normalizedRatio)
            } ?: GateSamplingRatioFamily.UnsupportedRatio
            val orientation = if (family == GateSamplingRatioFamily.Square || safeWidth == safeHeight) {
                GateSamplingOrientation.Square
            } else if (safeHeight > safeWidth) {
                GateSamplingOrientation.Portrait
            } else {
                GateSamplingOrientation.Landscape
            }

            return GateSamplingProfile(
                family = family,
                orientation = orientation,
                normalizedRatio = normalizedRatio,
                sourceWidth = safeWidth,
                sourceHeight = safeHeight,
                metadataRotationDegrees = metadataRotationDegrees,
            )
        }
    }
}
