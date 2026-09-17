/*
 * Selectively extracted from the user-authorized camera/gallery application source
 * at historical source revision omitted. See docs/EXTRACTION_MANIFEST.md.
 */
package org.sakos.camera.safety.opennsfw2

import android.graphics.Bitmap
import java.util.Locale
import kotlin.math.roundToInt

enum class SakosCompatibleCropRole {
    Context,
    Detection,
}

enum class SakosCompatibleCropTag {
    SelfieSensitive,
    Chest,
    Torso,
    Lateral,
    LowerLateral,
}

data class SakosCompatibleCropWindow(
    val label: String,
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
    val role: SakosCompatibleCropRole = SakosCompatibleCropRole.Detection,
    val tags: Set<SakosCompatibleCropTag> = emptySet(),
) {
    fun boundsLabel(): String = "$left,$top ${width}x$height"

    fun hasTag(tag: SakosCompatibleCropTag): Boolean = tags.contains(tag)

    fun hasAnyTag(vararg expected: SakosCompatibleCropTag): Boolean = expected.any(tags::contains)

    fun extract(bitmap: Bitmap): Bitmap {
        return if (left == 0 && top == 0 && width == bitmap.width && height == bitmap.height) {
            bitmap
        } else {
            Bitmap.createBitmap(bitmap, left, top, width, height)
        }
    }
}

data class SakosCompatibleStageDefinition(
    val id: String,
    val label: String,
    val windows: List<SakosCompatibleCropWindow>,
)

data class LiveSakosRuntimeViewEvaluation(
    val cropWindow: SakosCompatibleCropWindow,
    val scores: SakosCompatibleModelScores,
    val inferenceMillis: Long,
) {
    fun summaryLine(): String = buildString {
        append("View ")
        append(cropWindow.label)
        append(" (")
        append(cropWindow.boundsLabel())
        append("): sfw=")
        append(formatScore(scores.sfwProbability))
        append(", nsfw=")
        append(formatScore(scores.nsfwProbability))
        append(", ")
        append(inferenceMillis)
        append(" ms")
    }
}

data class LiveSakosRuntimeStageEvaluation(
    val stageId: String,
    val stageLabel: String,
    val decisionLabel: String,
    val elapsedMillis: Long,
    val cumulativeEvaluatedViews: Int,
) {
    fun summaryLine(): String = "$stageLabel: $decisionLabel, ${elapsedMillis} ms, $cumulativeEvaluatedViews view(s)"
}

data class SakosCompatiblePolicySignal(
    val representativeView: LiveSakosRuntimeViewEvaluation,
    val strongestView: LiveSakosRuntimeViewEvaluation,
    val policyNsfwEvidence: Float,
    val safeLeading: Boolean,
    val triggerLabel: String,
    val supportiveDetectionCount: Int,
    val elevatedDetectionCount: Int,
)

object SakosCompatibleMultiCropStrategy {
    fun defaultWindows(
        sourceWidth: Int,
        sourceHeight: Int,
    ): List<SakosCompatibleCropWindow> = fixed14Windows(
        sourceWidth = sourceWidth,
        sourceHeight = sourceHeight,
    )

    fun fixed14Windows(
        sourceWidth: Int,
        sourceHeight: Int,
    ): List<SakosCompatibleCropWindow> = fixed14Windows(
        sourceWidth = sourceWidth,
        sourceHeight = sourceHeight,
        samplingProfile = GateSamplingProfile.detect(
            width = sourceWidth,
            height = sourceHeight,
        ),
    )

    fun fixed14Windows(
        sourceWidth: Int,
        sourceHeight: Int,
        samplingProfile: GateSamplingProfile,
    ): List<SakosCompatibleCropWindow> = when (samplingProfile.family) {
        GateSamplingRatioFamily.TallPhone -> tallPhone14Windows(
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
        )

        GateSamplingRatioFamily.Standard16x9,
        GateSamplingRatioFamily.Classic4x3,
        GateSamplingRatioFamily.Square,
        -> profileAware14Windows(
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            samplingProfile = samplingProfile,
        )

        GateSamplingRatioFamily.UnsupportedRatio -> genericFallbackWindows(
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
        )
    }

    private fun tallPhone14Windows(
        sourceWidth: Int,
        sourceHeight: Int,
    ): List<SakosCompatibleCropWindow> {
        val fullWidth = sourceWidth.coerceAtLeast(1)
        val fullHeight = sourceHeight.coerceAtLeast(1)
        val centerInsetX = ((fullWidth * 0.125f).toInt()).coerceAtLeast(0)
        val centerInsetY = ((fullHeight * 0.125f).toInt()).coerceAtLeast(0)
        val torsoWidth = ((fullWidth * 0.55f).toInt()).coerceAtLeast(1)
        val torsoHeight = ((fullHeight * 0.45f).toInt()).coerceAtLeast(1)
        val torsoTop = ((fullHeight * 0.10f).toInt()).coerceIn(0, (fullHeight - torsoHeight).coerceAtLeast(0))
        val torsoCenterLeft = ((fullWidth - torsoWidth) / 2).coerceAtLeast(0)
        val torsoRightLeft = (fullWidth - torsoWidth).coerceAtLeast(0)
        val chestWidth = ((fullWidth * 0.36f).toInt()).coerceAtLeast(1)
        val chestHeight = ((fullHeight * 0.40f).toInt()).coerceAtLeast(1)
        val chestTop = ((fullHeight * 0.14f).toInt()).coerceIn(0, (fullHeight - chestHeight).coerceAtLeast(0))
        val chestRightLeft = (fullWidth - chestWidth).coerceAtLeast(0)
        val tileWidth = ((fullWidth * 0.5f).toInt()).coerceAtLeast(1)
        val tileHeight = ((fullHeight * 0.5f).toInt()).coerceAtLeast(1)
        val horizontalStarts = listOf(
            0,
            ((fullWidth - tileWidth) / 2).coerceAtLeast(0),
            (fullWidth - tileWidth).coerceAtLeast(0),
        ).distinct()
        val verticalStarts = listOf(
            0,
            ((fullHeight - tileHeight) / 2).coerceAtLeast(0),
            (fullHeight - tileHeight).coerceAtLeast(0),
        ).distinct()
        val horizontalLabels = listOf("Left", "Center", "Right")
        val verticalLabels = listOf("Top", "Middle", "Bottom")

        return buildList {
            add(
                SakosCompatibleCropWindow(
                    label = "Full frame",
                    left = 0,
                    top = 0,
                    width = fullWidth,
                    height = fullHeight,
                    role = SakosCompatibleCropRole.Context,
                ),
            )
            add(
                SakosCompatibleCropWindow(
                    label = "Center crop",
                    left = centerInsetX,
                    top = centerInsetY,
                    width = (fullWidth - centerInsetX * 2).coerceAtLeast(1),
                    height = (fullHeight - centerInsetY * 2).coerceAtLeast(1),
                    role = SakosCompatibleCropRole.Context,
                ),
            )
            add(
                SakosCompatibleCropWindow(
                    label = "Upper-Center torso",
                    left = torsoCenterLeft,
                    top = torsoTop,
                    width = torsoWidth,
                    height = torsoHeight,
                    role = SakosCompatibleCropRole.Detection,
                    tags = setOf(SakosCompatibleCropTag.Torso),
                ),
            )
            add(
                SakosCompatibleCropWindow(
                    label = "Upper-Right torso",
                    left = torsoRightLeft,
                    top = torsoTop,
                    width = torsoWidth,
                    height = torsoHeight,
                    role = SakosCompatibleCropRole.Detection,
                    tags = setOf(
                        SakosCompatibleCropTag.SelfieSensitive,
                        SakosCompatibleCropTag.Torso,
                        SakosCompatibleCropTag.Lateral,
                    ),
                ),
            )
            add(
                SakosCompatibleCropWindow(
                    label = "Upper-Right chest",
                    left = chestRightLeft,
                    top = chestTop,
                    width = chestWidth,
                    height = chestHeight,
                    role = SakosCompatibleCropRole.Detection,
                    tags = setOf(
                        SakosCompatibleCropTag.SelfieSensitive,
                        SakosCompatibleCropTag.Chest,
                        SakosCompatibleCropTag.Lateral,
                    ),
                ),
            )
            verticalStarts.forEachIndexed { rowIndex, top ->
                horizontalStarts.forEachIndexed { columnIndex, left ->
                    val tags = buildSet {
                        if (rowIndex == verticalStarts.lastIndex && columnIndex != 1) {
                            add(SakosCompatibleCropTag.SelfieSensitive)
                            add(SakosCompatibleCropTag.LowerLateral)
                        }
                    }
                    add(
                        SakosCompatibleCropWindow(
                            label = "${verticalLabels[rowIndex]}-${horizontalLabels[columnIndex]} half-tile",
                            left = left,
                            top = top,
                            width = tileWidth,
                            height = tileHeight,
                            role = SakosCompatibleCropRole.Detection,
                            tags = tags,
                        ),
                    )
                }
            }
        }.distinctBy { window ->
            listOf(window.left, window.top, window.width, window.height)
        }
    }

    private data class SamplingProfileCropFractions(
        val centerInsetX: Float,
        val centerInsetY: Float,
        val torsoWidth: Float,
        val torsoHeight: Float,
        val torsoTop: Float,
        val chestWidth: Float,
        val chestHeight: Float,
        val chestTop: Float,
    )

    private fun profileAware14Windows(
        sourceWidth: Int,
        sourceHeight: Int,
        samplingProfile: GateSamplingProfile,
    ): List<SakosCompatibleCropWindow> {
        val fullWidth = sourceWidth.coerceAtLeast(1)
        val fullHeight = sourceHeight.coerceAtLeast(1)
        val fractions = cropFractionsFor(samplingProfile)
        val centerInsetX = scaledDimension(fullWidth, fractions.centerInsetX).coerceAtLeast(0)
        val centerInsetY = scaledDimension(fullHeight, fractions.centerInsetY).coerceAtLeast(0)
        val torsoWidth = scaledDimension(fullWidth, fractions.torsoWidth)
        val torsoHeight = scaledDimension(fullHeight, fractions.torsoHeight)
        val torsoTop = scaledInset(fullHeight, fractions.torsoTop, torsoHeight)
        val torsoCenterLeft = ((fullWidth - torsoWidth) / 2).coerceAtLeast(0)
        val torsoRightLeft = (fullWidth - torsoWidth).coerceAtLeast(0)
        val chestWidth = scaledDimension(fullWidth, fractions.chestWidth)
        val chestHeight = scaledDimension(fullHeight, fractions.chestHeight)
        val chestTop = scaledInset(fullHeight, fractions.chestTop, chestHeight)
        val chestRightLeft = (fullWidth - chestWidth).coerceAtLeast(0)
        val tileWidth = scaledDimension(fullWidth, 0.5f)
        val tileHeight = scaledDimension(fullHeight, 0.5f)
        val horizontalStarts = listOf(
            0,
            ((fullWidth - tileWidth) / 2).coerceAtLeast(0),
            (fullWidth - tileWidth).coerceAtLeast(0),
        ).distinct()
        val verticalStarts = listOf(
            0,
            ((fullHeight - tileHeight) / 2).coerceAtLeast(0),
            (fullHeight - tileHeight).coerceAtLeast(0),
        ).distinct()
        val horizontalLabels = listOf("Left", "Center", "Right")
        val verticalLabels = listOf("Top", "Middle", "Bottom")

        return buildList {
            add(
                boundedCropWindow(
                    label = "Full frame",
                    sourceWidth = fullWidth,
                    sourceHeight = fullHeight,
                    requestedLeft = 0,
                    requestedTop = 0,
                    requestedWidth = fullWidth,
                    requestedHeight = fullHeight,
                    role = SakosCompatibleCropRole.Context,
                ),
            )
            add(
                boundedCropWindow(
                    label = "Center crop",
                    sourceWidth = fullWidth,
                    sourceHeight = fullHeight,
                    requestedLeft = centerInsetX,
                    requestedTop = centerInsetY,
                    requestedWidth = fullWidth - centerInsetX * 2,
                    requestedHeight = fullHeight - centerInsetY * 2,
                    role = SakosCompatibleCropRole.Context,
                ),
            )
            add(
                boundedCropWindow(
                    label = "Upper-Center torso",
                    sourceWidth = fullWidth,
                    sourceHeight = fullHeight,
                    requestedLeft = torsoCenterLeft,
                    requestedTop = torsoTop,
                    requestedWidth = torsoWidth,
                    requestedHeight = torsoHeight,
                    tags = setOf(SakosCompatibleCropTag.Torso),
                ),
            )
            add(
                boundedCropWindow(
                    label = "Upper-Right torso",
                    sourceWidth = fullWidth,
                    sourceHeight = fullHeight,
                    requestedLeft = torsoRightLeft,
                    requestedTop = torsoTop,
                    requestedWidth = torsoWidth,
                    requestedHeight = torsoHeight,
                    tags = setOf(
                        SakosCompatibleCropTag.SelfieSensitive,
                        SakosCompatibleCropTag.Torso,
                        SakosCompatibleCropTag.Lateral,
                    ),
                ),
            )
            add(
                boundedCropWindow(
                    label = "Upper-Right chest",
                    sourceWidth = fullWidth,
                    sourceHeight = fullHeight,
                    requestedLeft = chestRightLeft,
                    requestedTop = chestTop,
                    requestedWidth = chestWidth,
                    requestedHeight = chestHeight,
                    tags = setOf(
                        SakosCompatibleCropTag.SelfieSensitive,
                        SakosCompatibleCropTag.Chest,
                        SakosCompatibleCropTag.Lateral,
                    ),
                ),
            )
            verticalStarts.forEachIndexed { rowIndex, top ->
                horizontalStarts.forEachIndexed { columnIndex, left ->
                    val tags = buildSet {
                        if (rowIndex == verticalStarts.lastIndex && columnIndex != 1) {
                            add(SakosCompatibleCropTag.SelfieSensitive)
                            add(SakosCompatibleCropTag.LowerLateral)
                        }
                    }
                    add(
                        boundedCropWindow(
                            label = "${verticalLabels[rowIndex]}-${horizontalLabels[columnIndex]} half-tile",
                            sourceWidth = fullWidth,
                            sourceHeight = fullHeight,
                            requestedLeft = left,
                            requestedTop = top,
                            requestedWidth = tileWidth,
                            requestedHeight = tileHeight,
                            tags = tags,
                        ),
                    )
                }
            }
        }.distinctBy { window ->
            listOf(window.left, window.top, window.width, window.height)
        }
    }

    private fun cropFractionsFor(samplingProfile: GateSamplingProfile): SamplingProfileCropFractions =
        when (samplingProfile.family) {
            GateSamplingRatioFamily.Standard16x9 -> if (samplingProfile.orientation == GateSamplingOrientation.Landscape) {
                SamplingProfileCropFractions(0.10f, 0.12f, 0.46f, 0.58f, 0.12f, 0.30f, 0.46f, 0.18f)
            } else {
                SamplingProfileCropFractions(0.11f, 0.10f, 0.58f, 0.42f, 0.12f, 0.40f, 0.36f, 0.18f)
            }

            GateSamplingRatioFamily.Classic4x3 -> if (samplingProfile.orientation == GateSamplingOrientation.Landscape) {
                SamplingProfileCropFractions(0.10f, 0.10f, 0.48f, 0.58f, 0.12f, 0.34f, 0.46f, 0.18f)
            } else {
                SamplingProfileCropFractions(0.10f, 0.10f, 0.60f, 0.46f, 0.12f, 0.42f, 0.40f, 0.16f)
            }

            GateSamplingRatioFamily.Square ->
                SamplingProfileCropFractions(0.10f, 0.10f, 0.62f, 0.44f, 0.12f, 0.42f, 0.38f, 0.18f)

            GateSamplingRatioFamily.TallPhone,
            GateSamplingRatioFamily.UnsupportedRatio,
            -> SamplingProfileCropFractions(0.125f, 0.125f, 0.55f, 0.45f, 0.10f, 0.36f, 0.40f, 0.14f)
        }

    private fun genericFallbackWindows(
        sourceWidth: Int,
        sourceHeight: Int,
    ): List<SakosCompatibleCropWindow> {
        val fullWidth = sourceWidth.coerceAtLeast(1)
        val fullHeight = sourceHeight.coerceAtLeast(1)
        val centerInsetX = scaledDimension(fullWidth, 0.10f).coerceAtLeast(0)
        val centerInsetY = scaledDimension(fullHeight, 0.10f).coerceAtLeast(0)
        val columnWidth = scaledDimension(fullWidth, 1f / 3f)
        val rowHeight = scaledDimension(fullHeight, 1f / 3f)
        val horizontalStarts = listOf(
            0,
            ((fullWidth - columnWidth) / 2).coerceAtLeast(0),
            (fullWidth - columnWidth).coerceAtLeast(0),
        ).distinct()
        val verticalStarts = listOf(
            0,
            ((fullHeight - rowHeight) / 2).coerceAtLeast(0),
            (fullHeight - rowHeight).coerceAtLeast(0),
        ).distinct()
        val horizontalLabels = listOf("Left", "Center", "Right")
        val verticalLabels = listOf("Top", "Middle", "Bottom")

        return buildList {
            add(
                boundedCropWindow(
                    label = "Full frame",
                    sourceWidth = fullWidth,
                    sourceHeight = fullHeight,
                    requestedLeft = 0,
                    requestedTop = 0,
                    requestedWidth = fullWidth,
                    requestedHeight = fullHeight,
                    role = SakosCompatibleCropRole.Context,
                ),
            )
            add(
                boundedCropWindow(
                    label = "Center crop",
                    sourceWidth = fullWidth,
                    sourceHeight = fullHeight,
                    requestedLeft = centerInsetX,
                    requestedTop = centerInsetY,
                    requestedWidth = fullWidth - centerInsetX * 2,
                    requestedHeight = fullHeight - centerInsetY * 2,
                    role = SakosCompatibleCropRole.Context,
                ),
            )
            verticalStarts.forEachIndexed { rowIndex, top ->
                horizontalStarts.forEachIndexed { columnIndex, left ->
                    add(
                        boundedCropWindow(
                            label = "${verticalLabels[rowIndex]}-${horizontalLabels[columnIndex]} grid",
                            sourceWidth = fullWidth,
                            sourceHeight = fullHeight,
                            requestedLeft = left,
                            requestedTop = top,
                            requestedWidth = columnWidth,
                            requestedHeight = rowHeight,
                        ),
                    )
                }
            }
        }.distinctBy { window ->
            listOf(window.left, window.top, window.width, window.height)
        }
    }

    private fun scaledDimension(
        source: Int,
        fraction: Float,
    ): Int = (source * fraction).roundToInt().coerceIn(1, source.coerceAtLeast(1))

    private fun scaledInset(
        source: Int,
        fraction: Float,
        cropSize: Int,
    ): Int = (source * fraction).roundToInt().coerceIn(0, (source - cropSize).coerceAtLeast(0))

    private fun boundedCropWindow(
        label: String,
        sourceWidth: Int,
        sourceHeight: Int,
        requestedLeft: Int,
        requestedTop: Int,
        requestedWidth: Int,
        requestedHeight: Int,
        role: SakosCompatibleCropRole = SakosCompatibleCropRole.Detection,
        tags: Set<SakosCompatibleCropTag> = emptySet(),
    ): SakosCompatibleCropWindow {
        val width = requestedWidth.coerceIn(1, sourceWidth.coerceAtLeast(1))
        val height = requestedHeight.coerceIn(1, sourceHeight.coerceAtLeast(1))
        val left = requestedLeft.coerceIn(0, (sourceWidth - width).coerceAtLeast(0))
        val top = requestedTop.coerceIn(0, (sourceHeight - height).coerceAtLeast(0))
        return SakosCompatibleCropWindow(
            label = label,
            left = left,
            top = top,
            width = width,
            height = height,
            role = role,
            tags = tags,
        )
    }

    fun adaptive14Stages(
        sourceWidth: Int,
        sourceHeight: Int,
    ): List<SakosCompatibleStageDefinition> = adaptive14Stages(
        sourceWidth = sourceWidth,
        sourceHeight = sourceHeight,
        samplingProfile = GateSamplingProfile.detect(
            width = sourceWidth,
            height = sourceHeight,
        ),
    )

    fun adaptive14Stages(
        sourceWidth: Int,
        sourceHeight: Int,
        samplingProfile: GateSamplingProfile,
    ): List<SakosCompatibleStageDefinition> {
        val windows = fixed14Windows(
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            samplingProfile = samplingProfile,
        )
        val selectedLabels = linkedSetOf<String>()
        val windowsByLabel = windows.associateBy { it.label }

        fun selectWindows(
            preferredLabels: List<String>,
            targetCount: Int,
        ): List<SakosCompatibleCropWindow> {
            val preferred = preferredLabels.mapNotNull(windowsByLabel::get)
                .filterNot { window -> window.label in selectedLabels }
            val fill = windows
                .filterNot { window -> window.label in selectedLabels || window in preferred }
                .take((targetCount - preferred.size).coerceAtLeast(0))
            return (preferred + fill)
                .take(targetCount)
                .onEach { window -> selectedLabels += window.label }
        }

        val stageOneWindows = selectWindows(
            preferredLabels = listOf(
                "Full frame",
                "Center crop",
                "Upper-Center torso",
                "Middle-Center grid",
            ),
            targetCount = 3,
        )
        val stageTwoWindows = selectWindows(
            preferredLabels = listOf(
                "Upper-Right chest",
                "Upper-Right torso",
                "Bottom-Right half-tile",
                "Bottom-Center half-tile",
                "Bottom-Left half-tile",
                "Middle-Right grid",
                "Bottom-Right grid",
                "Bottom-Center grid",
                "Bottom-Left grid",
            ),
            targetCount = 5,
        )
        val stageThreeWindows = windows.filterNot { window -> window.label in selectedLabels }

        return listOf(
            SakosCompatibleStageDefinition(
                id = "stage1",
                label = "Stage 1 context sweep",
                windows = stageOneWindows,
            ),
            SakosCompatibleStageDefinition(
                id = "stage2",
                label = "Stage 2 targeted escalation",
                windows = stageTwoWindows,
            ),
            SakosCompatibleStageDefinition(
                id = "stage3",
                label = "Stage 3 full 14-view fallback",
                windows = stageThreeWindows,
            ),
        )
    }

    fun adaptiveStage1SentinelWindows(
        sourceWidth: Int,
        sourceHeight: Int,
    ): List<SakosCompatibleCropWindow> = adaptiveStage1SentinelWindows(
        sourceWidth = sourceWidth,
        sourceHeight = sourceHeight,
        samplingProfile = GateSamplingProfile.detect(
            width = sourceWidth,
            height = sourceHeight,
        ),
    )

    fun adaptiveStage1SentinelWindows(
        sourceWidth: Int,
        sourceHeight: Int,
        samplingProfile: GateSamplingProfile,
    ): List<SakosCompatibleCropWindow> {
        if (!samplingProfile.supportsPortraitFocusedWindows()) {
            return emptyList()
        }
        val fullWidth = sourceWidth.coerceAtLeast(1)
        val fullHeight = sourceHeight.coerceAtLeast(1)
        val torsoWidth = ((fullWidth * 0.48f).toInt()).coerceAtLeast(1)
        val torsoHeight = ((fullHeight * 0.46f).toInt()).coerceAtLeast(1)
        val torsoTop = ((fullHeight * 0.20f).toInt()).coerceIn(0, (fullHeight - torsoHeight).coerceAtLeast(0))
        val rightTorsoLeft = (fullWidth - torsoWidth).coerceAtLeast(0)
        val chestWidth = ((fullWidth * 0.34f).toInt()).coerceAtLeast(1)
        val chestHeight = ((fullHeight * 0.30f).toInt()).coerceAtLeast(1)
        val chestTop = ((fullHeight * 0.26f).toInt()).coerceIn(0, (fullHeight - chestHeight).coerceAtLeast(0))
        val chestInset = ((fullWidth * 0.05f).toInt()).coerceAtLeast(0)
        val leftChestLeft = chestInset.coerceIn(0, (fullWidth - chestWidth).coerceAtLeast(0))
        val rightChestLeft = (fullWidth - chestWidth - chestInset).coerceAtLeast(0)
        val fixedWindowsByLabel = fixed14Windows(
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            samplingProfile = samplingProfile,
        ).associateBy { it.label }

        fun requireFixedWindow(label: String): SakosCompatibleCropWindow =
            fixedWindowsByLabel[label] ?: error("Missing expected fixed crop window: $label")

        return buildList {
            add(
                SakosCompatibleCropWindow(
                    label = "Mid-Left torso sentinel",
                    left = 0,
                    top = torsoTop,
                    width = torsoWidth,
                    height = torsoHeight,
                    role = SakosCompatibleCropRole.Detection,
                    tags = setOf(
                        SakosCompatibleCropTag.SelfieSensitive,
                        SakosCompatibleCropTag.Torso,
                        SakosCompatibleCropTag.Lateral,
                    ),
                ),
            )
            add(
                SakosCompatibleCropWindow(
                    label = "Mid-Right torso sentinel",
                    left = rightTorsoLeft,
                    top = torsoTop,
                    width = torsoWidth,
                    height = torsoHeight,
                    role = SakosCompatibleCropRole.Detection,
                    tags = setOf(
                        SakosCompatibleCropTag.SelfieSensitive,
                        SakosCompatibleCropTag.Torso,
                        SakosCompatibleCropTag.Lateral,
                    ),
                ),
            )
            add(
                SakosCompatibleCropWindow(
                    label = "Mid-Left chest sentinel",
                    left = leftChestLeft,
                    top = chestTop,
                    width = chestWidth,
                    height = chestHeight,
                    role = SakosCompatibleCropRole.Detection,
                    tags = setOf(
                        SakosCompatibleCropTag.SelfieSensitive,
                        SakosCompatibleCropTag.Chest,
                        SakosCompatibleCropTag.Lateral,
                    ),
                ),
            )
            add(
                SakosCompatibleCropWindow(
                    label = "Mid-Right chest sentinel",
                    left = rightChestLeft,
                    top = chestTop,
                    width = chestWidth,
                    height = chestHeight,
                    role = SakosCompatibleCropRole.Detection,
                    tags = setOf(
                        SakosCompatibleCropTag.SelfieSensitive,
                        SakosCompatibleCropTag.Chest,
                        SakosCompatibleCropTag.Lateral,
                    ),
                ),
            )
            add(requireFixedWindow("Bottom-Center half-tile"))
        }.distinctBy { window ->
            listOf(window.left, window.top, window.width, window.height)
        }
    }

    fun adaptivePortraitAmbiguityPackWindows(
        sourceWidth: Int,
        sourceHeight: Int,
    ): List<SakosCompatibleCropWindow> = adaptivePortraitAmbiguityPackWindows(
        sourceWidth = sourceWidth,
        sourceHeight = sourceHeight,
        samplingProfile = GateSamplingProfile.detect(
            width = sourceWidth,
            height = sourceHeight,
        ),
    )

    fun adaptivePortraitAmbiguityPackWindows(
        sourceWidth: Int,
        sourceHeight: Int,
        samplingProfile: GateSamplingProfile,
    ): List<SakosCompatibleCropWindow> {
        if (!samplingProfile.supportsPortraitFocusedWindows()) {
            return emptyList()
        }
        val fullWidth = sourceWidth.coerceAtLeast(1)
        val fullHeight = sourceHeight.coerceAtLeast(1)
        val tightChestWidth = ((fullWidth * 0.30f).toInt()).coerceAtLeast(1)
        val tightChestHeight = ((fullHeight * 0.28f).toInt()).coerceAtLeast(1)
        val tightChestTop = ((fullHeight * 0.18f).toInt()).coerceIn(0, (fullHeight - tightChestHeight).coerceAtLeast(0))
        val lowerLateralWidth = ((fullWidth * 0.34f).toInt()).coerceAtLeast(1)
        val lowerLateralHeight = ((fullHeight * 0.34f).toInt()).coerceAtLeast(1)
        val lowerLateralTop = ((fullHeight * 0.34f).toInt()).coerceIn(0, (fullHeight - lowerLateralHeight).coerceAtLeast(0))
        val tightRightLeft = (fullWidth - tightChestWidth).coerceAtLeast(0)
        val lowerRightLeft = (fullWidth - lowerLateralWidth).coerceAtLeast(0)

        return buildList {
            add(
                SakosCompatibleCropWindow(
                    label = "Upper-Right chest tight",
                    left = tightRightLeft,
                    top = tightChestTop,
                    width = tightChestWidth,
                    height = tightChestHeight,
                    role = SakosCompatibleCropRole.Detection,
                    tags = setOf(
                        SakosCompatibleCropTag.SelfieSensitive,
                        SakosCompatibleCropTag.Chest,
                        SakosCompatibleCropTag.Lateral,
                    ),
                ),
            )
            add(
                SakosCompatibleCropWindow(
                    label = "Lower-Right lateral bust",
                    left = lowerRightLeft,
                    top = lowerLateralTop,
                    width = lowerLateralWidth,
                    height = lowerLateralHeight,
                    role = SakosCompatibleCropRole.Detection,
                    tags = setOf(
                        SakosCompatibleCropTag.SelfieSensitive,
                        SakosCompatibleCropTag.Chest,
                        SakosCompatibleCropTag.LowerLateral,
                    ),
                ),
            )
            add(
                SakosCompatibleCropWindow(
                    label = "Upper-Left chest tight",
                    left = 0,
                    top = tightChestTop,
                    width = tightChestWidth,
                    height = tightChestHeight,
                    role = SakosCompatibleCropRole.Detection,
                    tags = setOf(
                        SakosCompatibleCropTag.SelfieSensitive,
                        SakosCompatibleCropTag.Chest,
                        SakosCompatibleCropTag.Lateral,
                    ),
                ),
            )
            add(
                SakosCompatibleCropWindow(
                    label = "Lower-Left lateral bust",
                    left = 0,
                    top = lowerLateralTop,
                    width = lowerLateralWidth,
                    height = lowerLateralHeight,
                    role = SakosCompatibleCropRole.Detection,
                    tags = setOf(
                        SakosCompatibleCropTag.SelfieSensitive,
                        SakosCompatibleCropTag.Chest,
                        SakosCompatibleCropTag.LowerLateral,
                    ),
                ),
            )
        }.distinctBy { window ->
            listOf(window.left, window.top, window.width, window.height)
        }
    }

    fun adaptiveStrongestCropRefinementWindows(
        sourceWidth: Int,
        sourceHeight: Int,
        anchorWindow: SakosCompatibleCropWindow,
    ): List<SakosCompatibleCropWindow> {
        if (!anchorWindow.hasTag(SakosCompatibleCropTag.SelfieSensitive) ||
            !anchorWindow.hasTag(SakosCompatibleCropTag.LowerLateral)
        ) {
            return emptyList()
        }

        val fullWidth = sourceWidth.coerceAtLeast(1)
        val fullHeight = sourceHeight.coerceAtLeast(1)
        val isRightSide = anchorWindow.left + anchorWindow.width / 2 >= fullWidth / 2
        val sideLabel = if (isRightSide) "Right" else "Left"
        val edgeInset = (anchorWindow.width * 0.12f).roundToInt().coerceAtLeast(0)
        val verticalInset = (anchorWindow.height * 0.08f).roundToInt().coerceAtLeast(0)
        val tightWidth = (anchorWindow.width * 0.72f).roundToInt().coerceAtLeast(1)
        val tightHeight = (anchorWindow.height * 0.74f).roundToInt().coerceAtLeast(1)
        val edgeWidth = (anchorWindow.width * 0.56f).roundToInt().coerceAtLeast(1)
        val edgeHeight = (anchorWindow.height * 0.62f).roundToInt().coerceAtLeast(1)

        fun boundedWindow(
            label: String,
            requestedLeft: Int,
            requestedTop: Int,
            requestedWidth: Int,
            requestedHeight: Int,
            tags: Set<SakosCompatibleCropTag>,
        ): SakosCompatibleCropWindow {
            val width = requestedWidth.coerceIn(1, fullWidth)
            val height = requestedHeight.coerceIn(1, fullHeight)
            val left = requestedLeft.coerceIn(0, (fullWidth - width).coerceAtLeast(0))
            val top = requestedTop.coerceIn(0, (fullHeight - height).coerceAtLeast(0))
            return SakosCompatibleCropWindow(
                label = label,
                left = left,
                top = top,
                width = width,
                height = height,
                role = SakosCompatibleCropRole.Detection,
                tags = tags,
            )
        }

        val sharedTags = buildSet {
            addAll(anchorWindow.tags)
            add(SakosCompatibleCropTag.SelfieSensitive)
            add(SakosCompatibleCropTag.Chest)
            add(SakosCompatibleCropTag.Lateral)
            add(SakosCompatibleCropTag.LowerLateral)
        }
        val tightLeft = if (isRightSide) {
            anchorWindow.left + anchorWindow.width - tightWidth
        } else {
            anchorWindow.left
        }
        val edgeLeft = if (isRightSide) {
            anchorWindow.left + anchorWindow.width - edgeWidth
        } else {
            anchorWindow.left
        }
        val inwardShiftLeft = if (isRightSide) {
            tightLeft - edgeInset
        } else {
            tightLeft + edgeInset
        }
        val tightTop = anchorWindow.top + verticalInset
        val edgeTop = anchorWindow.top + (anchorWindow.height * 0.04f).roundToInt()
        val inwardTop = tightTop + (anchorWindow.height * 0.06f).roundToInt()

        return buildList {
            add(
                boundedWindow(
                    label = "Lower-$sideLabel bust refine",
                    requestedLeft = tightLeft,
                    requestedTop = tightTop,
                    requestedWidth = tightWidth,
                    requestedHeight = tightHeight,
                    tags = sharedTags,
                ),
            )
            add(
                boundedWindow(
                    label = "$sideLabel-edge chest refine",
                    requestedLeft = edgeLeft,
                    requestedTop = edgeTop,
                    requestedWidth = edgeWidth,
                    requestedHeight = edgeHeight,
                    tags = sharedTags,
                ),
            )
            add(
                boundedWindow(
                    label = "Lower-$sideLabel inward refine",
                    requestedLeft = inwardShiftLeft,
                    requestedTop = inwardTop,
                    requestedWidth = tightWidth,
                    requestedHeight = tightHeight,
                    tags = sharedTags,
                ),
            )
        }.distinctBy { window ->
            listOf(window.left, window.top, window.width, window.height)
        }
    }

    fun shouldRunAdaptiveStage1SentinelPass(
        sourceWidth: Int,
        sourceHeight: Int,
    ): Boolean = shouldRunAdaptiveStage1SentinelPass(
        sourceWidth = sourceWidth,
        sourceHeight = sourceHeight,
        samplingProfile = GateSamplingProfile.detect(
            width = sourceWidth,
            height = sourceHeight,
        ),
    )

    fun shouldRunAdaptiveStage1SentinelPass(
        sourceWidth: Int,
        sourceHeight: Int,
        samplingProfile: GateSamplingProfile,
    ): Boolean = sourceHeight > sourceWidth && samplingProfile.supportsPortraitFocusedWindows()

    private fun GateSamplingProfile.supportsPortraitFocusedWindows(): Boolean =
        selectorEligible && orientation == GateSamplingOrientation.Portrait

    fun sixViewWindows(
        sourceWidth: Int,
        sourceHeight: Int,
    ): List<SakosCompatibleCropWindow> {
        val fullWidth = sourceWidth.coerceAtLeast(1)
        val fullHeight = sourceHeight.coerceAtLeast(1)
        val centerInsetX = ((fullWidth * 0.125f).toInt()).coerceAtLeast(0)
        val centerInsetY = ((fullHeight * 0.125f).toInt()).coerceAtLeast(0)
        val quadrantWidth = ((fullWidth * 0.5f).toInt()).coerceAtLeast(1)
        val quadrantHeight = ((fullHeight * 0.5f).toInt()).coerceAtLeast(1)
        val rightLeft = (fullWidth - quadrantWidth).coerceAtLeast(0)
        val bottomTop = (fullHeight - quadrantHeight).coerceAtLeast(0)

        return buildList {
            add(
                SakosCompatibleCropWindow(
                    label = "Full frame",
                    left = 0,
                    top = 0,
                    width = fullWidth,
                    height = fullHeight,
                    role = SakosCompatibleCropRole.Context,
                ),
            )
            add(
                SakosCompatibleCropWindow(
                    label = "Center crop",
                    left = centerInsetX,
                    top = centerInsetY,
                    width = (fullWidth - centerInsetX * 2).coerceAtLeast(1),
                    height = (fullHeight - centerInsetY * 2).coerceAtLeast(1),
                    role = SakosCompatibleCropRole.Context,
                ),
            )
            add(
                SakosCompatibleCropWindow(
                    label = "Top-Left quadrant",
                    left = 0,
                    top = 0,
                    width = quadrantWidth,
                    height = quadrantHeight,
                    role = SakosCompatibleCropRole.Detection,
                ),
            )
            add(
                SakosCompatibleCropWindow(
                    label = "Top-Right quadrant",
                    left = rightLeft,
                    top = 0,
                    width = quadrantWidth,
                    height = quadrantHeight,
                    role = SakosCompatibleCropRole.Detection,
                ),
            )
            add(
                SakosCompatibleCropWindow(
                    label = "Bottom-Left quadrant",
                    left = 0,
                    top = bottomTop,
                    width = quadrantWidth,
                    height = quadrantHeight,
                    role = SakosCompatibleCropRole.Detection,
                ),
            )
            add(
                SakosCompatibleCropWindow(
                    label = "Bottom-Right quadrant",
                    left = rightLeft,
                    top = bottomTop,
                    width = quadrantWidth,
                    height = quadrantHeight,
                    role = SakosCompatibleCropRole.Detection,
                ),
            )
        }.distinctBy { window ->
            listOf(window.left, window.top, window.width, window.height)
        }
    }

    fun strongestView(views: List<LiveSakosRuntimeViewEvaluation>): LiveSakosRuntimeViewEvaluation {
        require(views.isNotEmpty()) { "At least one evaluated crop view is required." }
        return views.maxWithOrNull(
            compareBy<LiveSakosRuntimeViewEvaluation> { it.scores.nsfwProbability }
                .thenByDescending { it.cropWindow.width * it.cropWindow.height },
        ) ?: error("Unable to select the strongest evaluated crop view.")
    }

    fun maxNsfwProbability(views: List<LiveSakosRuntimeViewEvaluation>): Float {
        require(views.isNotEmpty()) { "At least one evaluated crop view is required." }
        return views.maxOf { it.scores.nsfwProbability }
    }

    fun shouldAdaptiveStage1EasyAllow(
        stageOneViews: List<LiveSakosRuntimeViewEvaluation>,
        sharedConstants: IntegratedStillGatePolicyConstants = IntegratedStillGatePolicyDefaults.fallback,
    ): Boolean {
        require(stageOneViews.isNotEmpty()) { "At least one stage-one view is required." }
        return stageOneViews.all { it.scores.nsfwProbability < sharedConstants.stage1EasyAllowMax }
    }

    fun shouldAdaptiveStage1Allow(
        stageOneViews: List<LiveSakosRuntimeViewEvaluation>,
        sharedConstants: IntegratedStillGatePolicyConstants = IntegratedStillGatePolicyDefaults.fallback,
    ): Boolean {
        require(stageOneViews.isNotEmpty()) { "At least one stage-one view is required." }
        return stageOneViews.all { it.scores.nsfwProbability < sharedConstants.stage1AllowMax }
    }

    fun shouldAdaptiveStage2Allow(
        accumulatedViews: List<LiveSakosRuntimeViewEvaluation>,
        sharedConstants: IntegratedStillGatePolicyConstants = IntegratedStillGatePolicyDefaults.fallback,
    ): Boolean {
        require(accumulatedViews.isNotEmpty()) { "At least one evaluated crop view is required." }
        return maxNsfwProbability(accumulatedViews) < sharedConstants.stage2AllowMax &&
            !hasDetectionViewsAtOrAbove(
                views = accumulatedViews,
                floor = sharedConstants.stage2CorroboratedDetectionFloor,
                minimumCount = 2,
            )
    }

    fun hasDetectionViewsAtOrAbove(
        views: List<LiveSakosRuntimeViewEvaluation>,
        floor: Float,
        minimumCount: Int,
    ): Boolean {
        require(views.isNotEmpty()) { "At least one evaluated crop view is required." }
        return views.count { view ->
            view.cropWindow.role == SakosCompatibleCropRole.Detection &&
                view.scores.nsfwProbability >= floor
        } >= minimumCount
    }

    fun strongestTaggedDetectionView(
        views: List<LiveSakosRuntimeViewEvaluation>,
        vararg tags: SakosCompatibleCropTag,
    ): LiveSakosRuntimeViewEvaluation? {
        require(views.isNotEmpty()) { "At least one evaluated crop view is required." }
        val requiredTags = tags.toSet()
        return views
            .asSequence()
            .filter { view ->
                view.cropWindow.role == SakosCompatibleCropRole.Detection &&
                    requiredTags.all { tag -> view.cropWindow.hasTag(tag) }
            }
            .maxWithOrNull(
                compareBy<LiveSakosRuntimeViewEvaluation> { it.scores.nsfwProbability }
                    .thenByDescending { it.cropWindow.width * it.cropWindow.height },
            )
    }

    fun strongestNearFloorLowerLateralView(
        views: List<LiveSakosRuntimeViewEvaluation>,
        sharedConstants: IntegratedStillGatePolicyConstants = IntegratedStillGatePolicyDefaults.fallback,
    ): LiveSakosRuntimeViewEvaluation? {
        require(views.isNotEmpty()) { "At least one evaluated crop view is required." }
        val minimumScore = sharedConstants.rawNsfwBlockFloor - sharedConstants.nearFloorLateralGap
        return views
            .asSequence()
            .filter { view ->
                view.cropWindow.role == SakosCompatibleCropRole.Detection &&
                    view.cropWindow.hasTag(SakosCompatibleCropTag.SelfieSensitive) &&
                    view.cropWindow.hasTag(SakosCompatibleCropTag.LowerLateral) &&
                    view.scores.nsfwProbability >= minimumScore &&
                    view.scores.nsfwProbability < sharedConstants.rawNsfwBlockFloor
            }
            .maxWithOrNull(
                compareBy<LiveSakosRuntimeViewEvaluation> { it.scores.nsfwProbability }
                    .thenByDescending { it.cropWindow.width * it.cropWindow.height },
            )
    }

    fun hasNearFloorLateralEvidence(
        views: List<LiveSakosRuntimeViewEvaluation>,
        sharedConstants: IntegratedStillGatePolicyConstants = IntegratedStillGatePolicyDefaults.fallback,
    ): Boolean {
        require(views.isNotEmpty()) { "At least one evaluated crop view is required." }
        val strongestSelfieSensitive = strongestTaggedDetectionView(
            views,
            SakosCompatibleCropTag.SelfieSensitive,
        ) ?: return false
        if (strongestSelfieSensitive.scores.nsfwProbability <
            sharedConstants.rawNsfwBlockFloor - sharedConstants.nearFloorLateralGap
        ) {
            return false
        }

        val corroboratingView = views.firstOrNull { view ->
            view !== strongestSelfieSensitive &&
                view.cropWindow.role == SakosCompatibleCropRole.Detection &&
                view.scores.nsfwProbability >= sharedConstants.elevatedDetectionFloor
        } ?: return false

        return strongestSelfieSensitive.cropWindow.hasAnyTag(
            SakosCompatibleCropTag.Chest,
            SakosCompatibleCropTag.LowerLateral,
        ) || corroboratingView.cropWindow.hasAnyTag(
            SakosCompatibleCropTag.Chest,
            SakosCompatibleCropTag.LowerLateral,
        )
    }

    fun shouldShortCircuitBlock(
        views: List<LiveSakosRuntimeViewEvaluation>,
        sharedConstants: IntegratedStillGatePolicyConstants = IntegratedStillGatePolicyDefaults.fallback,
    ): Boolean {
        require(views.isNotEmpty()) { "At least one evaluated crop view is required." }
        return policyWouldBlock(
            signal = policySignal(
                views = views,
                sharedConstants = sharedConstants,
            ),
            sharedConstants = sharedConstants,
        )
    }

    fun policySignal(
        views: List<LiveSakosRuntimeViewEvaluation>,
        sharedConstants: IntegratedStillGatePolicyConstants = IntegratedStillGatePolicyDefaults.fallback,
    ): SakosCompatiblePolicySignal {
        require(views.isNotEmpty()) { "At least one evaluated crop view is required." }
        val strongestOverall = strongestView(views)
        val contextViews = views.filter { it.cropWindow.role == SakosCompatibleCropRole.Context }
        val detectionViews = views.filter { it.cropWindow.role == SakosCompatibleCropRole.Detection }
        val strongestContext = strongestView(contextViews.ifEmpty { views })
        val strongestDetection = strongestView(detectionViews.ifEmpty { views })
        val contextMax = strongestContext.scores.nsfwProbability
        val detectionMax = strongestDetection.scores.nsfwProbability
        val elevatedDetectionCount = detectionViews.count {
            it.scores.nsfwProbability >= sharedConstants.elevatedDetectionFloor
        }
        val supportiveDetectionCount = detectionViews.count {
            it.scores.nsfwProbability >= sharedConstants.supportiveDetectionFloor
        }

        return when {
            contextMax >= sharedConstants.rawNsfwBlockFloor -> {
                SakosCompatiblePolicySignal(
                    representativeView = strongestContext,
                    strongestView = strongestOverall,
                    policyNsfwEvidence = contextMax,
                    safeLeading = strongestContext.scores.nsfwProbability <= strongestContext.scores.sfwProbability,
                    triggerLabel = "A context view crossed the strict child-safe raw floor.",
                    supportiveDetectionCount = supportiveDetectionCount,
                    elevatedDetectionCount = elevatedDetectionCount,
                )
            }

            detectionMax >= sharedConstants.extremeDetectionFloor -> {
                SakosCompatiblePolicySignal(
                    representativeView = strongestDetection,
                    strongestView = strongestOverall,
                    policyNsfwEvidence = detectionMax,
                    safeLeading = strongestDetection.scores.nsfwProbability <= strongestDetection.scores.sfwProbability,
                    triggerLabel = "A single overlapping detection tile was extremely unsafe.",
                    supportiveDetectionCount = supportiveDetectionCount,
                    elevatedDetectionCount = elevatedDetectionCount,
                )
            }

            hasNearFloorLateralEvidence(
                views = views,
                sharedConstants = sharedConstants,
            ) -> {
                val strongestSelfieSensitive = strongestTaggedDetectionView(
                    views,
                    SakosCompatibleCropTag.SelfieSensitive,
                ) ?: strongestDetection
                SakosCompatiblePolicySignal(
                    representativeView = strongestSelfieSensitive,
                    strongestView = strongestOverall,
                    policyNsfwEvidence = sharedConstants.rawNsfwBlockFloor,
                    safeLeading = false,
                    triggerLabel = "Near-floor lateral selfie-sensitive evidence was corroborated by another detection view.",
                    supportiveDetectionCount = supportiveDetectionCount,
                    elevatedDetectionCount = elevatedDetectionCount,
                )
            }

            supportiveDetectionCount >= 2 && detectionMax >= sharedConstants.corroboratedDetectionAnchorFloor -> {
                SakosCompatiblePolicySignal(
                    representativeView = strongestDetection,
                    strongestView = strongestOverall,
                    policyNsfwEvidence = detectionMax.coerceAtLeast(sharedConstants.rawNsfwBlockFloor),
                    safeLeading = detectionMax < sharedConstants.rawNsfwBlockFloor,
                    triggerLabel = "Multiple detection views corroborated a strong NSFW signal.",
                    supportiveDetectionCount = supportiveDetectionCount,
                    elevatedDetectionCount = elevatedDetectionCount,
                )
            }

            else -> {
                val representativeView = if (contextMax >= detectionMax) {
                    strongestContext
                } else {
                    strongestDetection
                }
                val uncappedEvidence = if (representativeView.cropWindow.role == SakosCompatibleCropRole.Context) {
                    contextMax
                } else {
                    detectionMax.coerceAtMost(sharedConstants.rawNsfwBlockFloor - 0.01f)
                }
                SakosCompatiblePolicySignal(
                    representativeView = representativeView,
                    strongestView = strongestOverall,
                    policyNsfwEvidence = uncappedEvidence,
                    safeLeading = representativeView.scores.nsfwProbability <= representativeView.scores.sfwProbability,
                    triggerLabel = if (representativeView.cropWindow.role == SakosCompatibleCropRole.Context) {
                        "No corroborated tile evidence was found; the context view remained below the strict raw floor."
                    } else {
                        "No corroborated tile evidence was found; a lone crop view stayed below the strict raw floor."
                    },
                    supportiveDetectionCount = supportiveDetectionCount,
                    elevatedDetectionCount = elevatedDetectionCount,
                )
            }
        }
    }

    fun policyWouldBlock(
        signal: SakosCompatiblePolicySignal,
        sharedConstants: IntegratedStillGatePolicyConstants = IntegratedStillGatePolicyDefaults.fallback,
    ): Boolean = signal.policyNsfwEvidence >= sharedConstants.rawNsfwBlockFloor

    fun fixed14StrategyLabel(): String =
        "Full frame + center crop + torso and chest-focused portrait crops + overlapping half-frame tiles with early block short-circuit and stronger corroborated crop blocking"

    fun adaptive14StrategyLabel(): String =
        "Adaptive staged OpenNSFW2 still-photo evaluation with Stage 1 easy portrait-safe exits plus portrait-side sentinel probes, Stage 2 portrait ambiguity crops and strongest-crop lateral refinement before targeted torso/lower-corner crops settle, and Stage 3 fallback to the remaining fixed 14-view windows"

    fun strategyLabel(): String = fixed14StrategyLabel()
}

private fun formatScore(value: Float): String = String.format(Locale.US, "%.3f", value)
