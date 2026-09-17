/* Sanitized regression logic selectively extracted from user-authorized source commit 843d93f. */
package org.sakos.camera.safety.opennsfw2

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SakosCompatibleMultiCropEvaluationTest {
    @Test
    fun samplingProfileDetectionUsesExactSupportedRatioBands() {
        val s23Fhd = GateSamplingProfile.detect(width = 886, height = 1920)
        assertEquals(GateSamplingRatioFamily.TallPhone, s23Fhd.family)
        assertEquals(GateSamplingOrientation.Portrait, s23Fhd.orientation)
        assertEquals("tall-phone-portrait", s23Fhd.id)
        assertTrue(s23Fhd.selectorEligible)

        val standardLandscape = GateSamplingProfile.detect(width = 1920, height = 1080)
        assertEquals(GateSamplingRatioFamily.Standard16x9, standardLandscape.family)
        assertEquals(GateSamplingOrientation.Landscape, standardLandscape.orientation)

        val classicPortrait = GateSamplingProfile.detect(width = 3024, height = 4032)
        assertEquals(GateSamplingRatioFamily.Classic4x3, classicPortrait.family)
        assertEquals(GateSamplingOrientation.Portrait, classicPortrait.orientation)

        val square = GateSamplingProfile.detect(width = 1000, height = 1000)
        assertEquals(GateSamplingRatioFamily.Square, square.family)
        assertEquals(GateSamplingOrientation.Square, square.orientation)

        val unsupportedGap = GateSamplingProfile.detect(width = 1000, height = 1950)
        assertEquals(GateSamplingRatioFamily.UnsupportedRatio, unsupportedGap.family)
        assertFalse(unsupportedGap.selectorEligible)
    }

    @Test
    fun knownSamplingProfilesReturnBoundedContextAndDetectionWindows() {
        val inputs = listOf(
            GateSamplingProfile.detect(width = 886, height = 1920),
            GateSamplingProfile.detect(width = 1080, height = 1920),
            GateSamplingProfile.detect(width = 3024, height = 4032),
            GateSamplingProfile.detect(width = 1000, height = 1000),
        )

        inputs.forEach { profile ->
            val windows = SakosCompatibleMultiCropStrategy.fixed14Windows(
                sourceWidth = profile.sourceWidth,
                sourceHeight = profile.sourceHeight,
                samplingProfile = profile,
            )

            assertEquals(14, windows.size)
            assertTrue(windows.any { window -> window.role == SakosCompatibleCropRole.Context })
            assertTrue(windows.any { window -> window.role == SakosCompatibleCropRole.Detection })
            assertTrue(windows.all { window ->
                window.left >= 0 &&
                    window.top >= 0 &&
                    window.width > 0 &&
                    window.height > 0 &&
                    window.left + window.width <= profile.sourceWidth &&
                    window.top + window.height <= profile.sourceHeight
            })
        }
    }

    @Test
    fun unsupportedSamplingProfileUsesConservativeFallbackGrid() {
        val profile = GateSamplingProfile.detect(width = 1000, height = 1950)
        val windows = SakosCompatibleMultiCropStrategy.fixed14Windows(
            sourceWidth = profile.sourceWidth,
            sourceHeight = profile.sourceHeight,
            samplingProfile = profile,
        )
        val stages = SakosCompatibleMultiCropStrategy.adaptive14Stages(
            sourceWidth = profile.sourceWidth,
            sourceHeight = profile.sourceHeight,
            samplingProfile = profile,
        )

        assertEquals(GateSamplingRatioFamily.UnsupportedRatio, profile.family)
        assertEquals(11, windows.size)
        assertEquals(listOf(3, 5, 3), stages.map { stage -> stage.windows.size })
        assertTrue(windows.any { window -> window.role == SakosCompatibleCropRole.Context })
        assertTrue(windows.any { window -> window.label == "Middle-Center grid" })
    }

    @Test
    fun sixViewWindowsIncludeContextCenterAndFourQuadrants() {
        val windows = SakosCompatibleMultiCropStrategy.sixViewWindows(
            sourceWidth = 1080,
            sourceHeight = 1920,
        )

        assertEquals(6, windows.size)
        assertEquals("Full frame", windows.first().label)
        assertTrue(windows.any { it.label == "Center crop" })
        assertTrue(windows.any { it.label == "Top-Left quadrant" })
        assertTrue(windows.any { it.label == "Top-Right quadrant" })
        assertTrue(windows.any { it.label == "Bottom-Left quadrant" })
        assertTrue(windows.any { it.label == "Bottom-Right quadrant" })
        assertEquals(2, windows.count { it.role == SakosCompatibleCropRole.Context })
        assertEquals(4, windows.count { it.role == SakosCompatibleCropRole.Detection })
    }

    @Test
    fun defaultWindowsIncludeContextAndOverlappingHalfTiles() {
        val windows = SakosCompatibleMultiCropStrategy.defaultWindows(
            sourceWidth = 1080,
            sourceHeight = 1920,
        )

        assertEquals(14, windows.size)
        assertEquals("Full frame", windows.first().label)
        assertTrue(windows.any { it.label == "Center crop" })
        assertTrue(windows.any { it.label == "Upper-Center torso" })
        assertTrue(windows.any { it.label == "Upper-Right torso" })
        assertTrue(windows.any { it.label == "Upper-Right chest" })
        assertTrue(windows.any { it.label == "Top-Left half-tile" })
        assertTrue(windows.any { it.label == "Middle-Center half-tile" })
        assertTrue(windows.any { it.label == "Bottom-Right half-tile" })
        assertEquals(2, windows.count { it.role == SakosCompatibleCropRole.Context })
    }

    @Test
    fun adaptiveStagesKeepThreeFiveAndSixViewBucketsAndUseReorderedStageTwo() {
        val stages = SakosCompatibleMultiCropStrategy.adaptive14Stages(
            sourceWidth = 1080,
            sourceHeight = 1920,
        )

        assertEquals(3, stages.size)
        assertEquals(listOf(3, 5, 6), stages.map { it.windows.size })
        assertEquals("Stage 1 context sweep", stages[0].label)
        assertEquals("Stage 2 targeted escalation", stages[1].label)
        assertEquals("Stage 3 full 14-view fallback", stages[2].label)
        assertEquals(
            listOf(
                "Upper-Right chest",
                "Upper-Right torso",
                "Bottom-Right half-tile",
                "Bottom-Center half-tile",
                "Bottom-Left half-tile",
            ),
            stages[1].windows.map { it.label },
        )
    }

    @Test
    fun adaptiveStageOneSentinelsIncludeSideTorsoChestAndBottomCenterWindows() {
        val windows = SakosCompatibleMultiCropStrategy.adaptiveStage1SentinelWindows(
            sourceWidth = 1080,
            sourceHeight = 1920,
        )

        assertEquals(5, windows.size)
        assertTrue(windows.any { it.label == "Mid-Left torso sentinel" })
        assertTrue(windows.any { it.label == "Mid-Right torso sentinel" })
        assertTrue(windows.any { it.label == "Mid-Left chest sentinel" })
        assertTrue(windows.any { it.label == "Mid-Right chest sentinel" })
        assertTrue(windows.any { it.label == "Bottom-Center half-tile" })
    }

    @Test
    fun adaptivePortraitAmbiguityPackIncludesTightChestAndLowerLateralBustWindows() {
        val windows = SakosCompatibleMultiCropStrategy.adaptivePortraitAmbiguityPackWindows(
            sourceWidth = 1080,
            sourceHeight = 1920,
        )

        assertEquals(4, windows.size)
        assertEquals(
            listOf(
                "Upper-Right chest tight",
                "Lower-Right lateral bust",
                "Upper-Left chest tight",
                "Lower-Left lateral bust",
            ),
            windows.map { it.label },
        )
    }

    @Test
    fun adaptiveStageOneSentinelPassRunsOnlyForPortraitInputs() {
        assertTrue(
            SakosCompatibleMultiCropStrategy.shouldRunAdaptiveStage1SentinelPass(
                sourceWidth = 1080,
                sourceHeight = 1920,
            ),
        )
        assertTrue(
            !SakosCompatibleMultiCropStrategy.shouldRunAdaptiveStage1SentinelPass(
                sourceWidth = 1920,
                sourceHeight = 1080,
            ),
        )
    }

    @Test
    fun strongestViewUsesHighestNsfwScore() {
        val fullFrame = LiveSakosRuntimeViewEvaluation(
            cropWindow = SakosCompatibleCropWindow(
                label = "Full frame",
                left = 0,
                top = 0,
                width = 1080,
                height = 1920,
            ),
            scores = SakosCompatibleModelScores(
                sfwProbability = 0.82f,
                nsfwProbability = 0.18f,
            ),
            inferenceMillis = 8L,
        )
        val bottomRight = LiveSakosRuntimeViewEvaluation(
            cropWindow = SakosCompatibleCropWindow(
                label = "Bottom-right quadrant",
                left = 540,
                top = 960,
                width = 540,
                height = 960,
            ),
            scores = SakosCompatibleModelScores(
                sfwProbability = 0.43f,
                nsfwProbability = 0.57f,
            ),
            inferenceMillis = 9L,
        )

        val strongest = SakosCompatibleMultiCropStrategy.strongestView(
            listOf(fullFrame, bottomRight),
        )

        assertEquals("Bottom-right quadrant", strongest.cropWindow.label)
        assertEquals(0.57f, strongest.scores.nsfwProbability, 0.0001f)
    }

    @Test
    fun policySignalCapsSingleModerateTileBelowStrictFloor() {
        val fullFrame = view(
            label = "Full frame",
            role = SakosCompatibleCropRole.Context,
            nsfw = 0.12f,
        )
        val centerCrop = view(
            label = "Center crop",
            role = SakosCompatibleCropRole.Context,
            nsfw = 0.09f,
        )
        val singleDetection = view(
            label = "Bottom-Left half-tile",
            role = SakosCompatibleCropRole.Detection,
            nsfw = 0.42f,
        )

        val signal = SakosCompatibleMultiCropStrategy.policySignal(
            listOf(fullFrame, centerCrop, singleDetection),
        )

        assertEquals("Bottom-Left half-tile", signal.representativeView.cropWindow.label)
        assertEquals(0.42f, signal.policyNsfwEvidence, 0.0001f)
        assertTrue(signal.safeLeading)
    }

    @Test
    fun policySignalPromotesCorroboratedDetectionTilesToStrictFloor() {
        val fullFrame = view(
            label = "Full frame",
            role = SakosCompatibleCropRole.Context,
            nsfw = 0.11f,
        )
        val centerCrop = view(
            label = "Center crop",
            role = SakosCompatibleCropRole.Context,
            nsfw = 0.13f,
        )
        val tileOne = view(
            label = "Upper-Right torso",
            role = SakosCompatibleCropRole.Detection,
            nsfw = 0.45f,
        )
        val tileTwo = view(
            label = "Middle-Right half-tile",
            role = SakosCompatibleCropRole.Detection,
            nsfw = 0.34f,
        )

        val signal = SakosCompatibleMultiCropStrategy.policySignal(
            listOf(fullFrame, centerCrop, tileOne, tileTwo),
        )

        assertEquals("Upper-Right torso", signal.representativeView.cropWindow.label)
        assertEquals(0.45f, signal.policyNsfwEvidence, 0.0001f)
        assertTrue(!signal.safeLeading)
        assertEquals(2, signal.supportiveDetectionCount)
    }

    @Test
    fun selfieSensitiveWindowsCarryStructuredTagsForLateralPolicy() {
        val fixedWindows = SakosCompatibleMultiCropStrategy.fixed14Windows(
            sourceWidth = 1080,
            sourceHeight = 1920,
        ).associateBy { it.label }
        val sentinelWindows = SakosCompatibleMultiCropStrategy.adaptiveStage1SentinelWindows(
            sourceWidth = 1080,
            sourceHeight = 1920,
        ).associateBy { it.label }

        assertTrue(
            fixedWindows.getValue("Upper-Right chest").tags.containsAll(
                setOf(
                    SakosCompatibleCropTag.SelfieSensitive,
                    SakosCompatibleCropTag.Chest,
                    SakosCompatibleCropTag.Lateral,
                ),
            ),
        )
        assertTrue(
            fixedWindows.getValue("Bottom-Right half-tile").tags.containsAll(
                setOf(
                    SakosCompatibleCropTag.SelfieSensitive,
                    SakosCompatibleCropTag.LowerLateral,
                ),
            ),
        )
        assertTrue(
            sentinelWindows.getValue("Mid-Left torso sentinel").tags.containsAll(
                setOf(
                    SakosCompatibleCropTag.SelfieSensitive,
                    SakosCompatibleCropTag.Torso,
                    SakosCompatibleCropTag.Lateral,
                ),
            ),
        )
    }

    @Test
    fun policySignalBlocksNearFloorLateralEvidenceWhenCorroborated() {
        val signal = SakosCompatibleMultiCropStrategy.policySignal(
            listOf(
                view(label = "Full frame", role = SakosCompatibleCropRole.Context, nsfw = 0.18f),
                view(label = "Center crop", role = SakosCompatibleCropRole.Context, nsfw = 0.14f),
                view(
                    label = "Lower-Right lateral bust",
                    role = SakosCompatibleCropRole.Detection,
                    nsfw = 0.44f,
                    tags = setOf(
                        SakosCompatibleCropTag.SelfieSensitive,
                        SakosCompatibleCropTag.Chest,
                        SakosCompatibleCropTag.LowerLateral,
                    ),
                ),
                view(
                    label = "Upper-Right chest",
                    role = SakosCompatibleCropRole.Detection,
                    nsfw = 0.27f,
                    tags = setOf(
                        SakosCompatibleCropTag.SelfieSensitive,
                        SakosCompatibleCropTag.Chest,
                        SakosCompatibleCropTag.Lateral,
                    ),
                ),
            ),
        )

        assertEquals("Lower-Right lateral bust", signal.representativeView.cropWindow.label)
        assertEquals(0.45f, signal.policyNsfwEvidence, 0.0001f)
        assertTrue(!signal.safeLeading)
    }

    @Test
    fun policySignalDoesNotBlockNearFloorLateralEvidenceWithoutCorroboration() {
        val signal = SakosCompatibleMultiCropStrategy.policySignal(
            listOf(
                view(label = "Full frame", role = SakosCompatibleCropRole.Context, nsfw = 0.18f),
                view(label = "Center crop", role = SakosCompatibleCropRole.Context, nsfw = 0.14f),
                view(
                    label = "Lower-Right lateral bust",
                    role = SakosCompatibleCropRole.Detection,
                    nsfw = 0.47f,
                    tags = setOf(
                        SakosCompatibleCropTag.SelfieSensitive,
                        SakosCompatibleCropTag.Chest,
                        SakosCompatibleCropTag.LowerLateral,
                    ),
                ),
                view(
                    label = "Upper-Right torso",
                    role = SakosCompatibleCropRole.Detection,
                    nsfw = 0.23f,
                    tags = setOf(
                        SakosCompatibleCropTag.SelfieSensitive,
                        SakosCompatibleCropTag.Torso,
                        SakosCompatibleCropTag.Lateral,
                    ),
                ),
            ),
        )

        assertEquals("Lower-Right lateral bust", signal.representativeView.cropWindow.label)
        assertEquals(0.44f, signal.policyNsfwEvidence, 0.0001f)
        assertTrue(signal.safeLeading)
    }

    @Test
    fun earlyBlockShortCircuitTriggersWhenContextCrossesStrictFloor() {
        val fullFrame = view(
            label = "Full frame",
            role = SakosCompatibleCropRole.Context,
            nsfw = 0.51f,
        )

        assertTrue(SakosCompatibleMultiCropStrategy.shouldShortCircuitBlock(listOf(fullFrame)))
    }

    @Test
    fun earlyBlockShortCircuitTriggersWhenDetectionEvidenceIsCorroborated() {
        val fullFrame = view(
            label = "Full frame",
            role = SakosCompatibleCropRole.Context,
            nsfw = 0.12f,
        )
        val centerCrop = view(
            label = "Center crop",
            role = SakosCompatibleCropRole.Context,
            nsfw = 0.10f,
        )
        val tileOne = view(
            label = "Upper-Right torso",
            role = SakosCompatibleCropRole.Detection,
            nsfw = 0.52f,
        )
        val tileTwo = view(
            label = "Middle-Right half-tile",
            role = SakosCompatibleCropRole.Detection,
            nsfw = 0.33f,
        )

        assertTrue(
            SakosCompatibleMultiCropStrategy.shouldShortCircuitBlock(
                listOf(fullFrame, centerCrop, tileOne, tileTwo),
            ),
        )
    }

    @Test
    fun earlyBlockShortCircuitDoesNotTriggerForSingleModerateDetectionTile() {
        val fullFrame = view(
            label = "Full frame",
            role = SakosCompatibleCropRole.Context,
            nsfw = 0.12f,
        )
        val centerCrop = view(
            label = "Center crop",
            role = SakosCompatibleCropRole.Context,
            nsfw = 0.10f,
        )
        val singleTile = view(
            label = "Top-Left half-tile",
            role = SakosCompatibleCropRole.Detection,
            nsfw = 0.26f,
        )

        assertTrue(
            !SakosCompatibleMultiCropStrategy.shouldShortCircuitBlock(
                listOf(fullFrame, centerCrop, singleTile),
            ),
        )
    }

    @Test
    fun earlyBlockShortCircuitDoesNotTriggerForMultipleWeakDetectionTiles() {
        val fullFrame = view(
            label = "Full frame",
            role = SakosCompatibleCropRole.Context,
            nsfw = 0.14f,
        )
        val centerCrop = view(
            label = "Center crop",
            role = SakosCompatibleCropRole.Context,
            nsfw = 0.12f,
        )
        val tileOne = view(
            label = "Top-Center half-tile",
            role = SakosCompatibleCropRole.Detection,
            nsfw = 0.29f,
        )
        val tileTwo = view(
            label = "Middle-Center half-tile",
            role = SakosCompatibleCropRole.Detection,
            nsfw = 0.31f,
        )
        val tileThree = view(
            label = "Bottom-Center half-tile",
            role = SakosCompatibleCropRole.Detection,
            nsfw = 0.28f,
        )

        assertTrue(
            !SakosCompatibleMultiCropStrategy.shouldShortCircuitBlock(
                listOf(fullFrame, centerCrop, tileOne, tileTwo, tileThree),
            ),
        )
    }

    @Test
    fun adaptiveStageOneAllowsWhenAllThreeViewsStayBelowBand() {
        val stageOneViews = listOf(
            view(label = "Full frame", role = SakosCompatibleCropRole.Context, nsfw = 0.12f),
            view(label = "Center crop", role = SakosCompatibleCropRole.Context, nsfw = 0.19f),
            view(label = "Upper-Center torso", role = SakosCompatibleCropRole.Detection, nsfw = 0.08f),
        )

        assertTrue(SakosCompatibleMultiCropStrategy.shouldAdaptiveStage1Allow(stageOneViews))
    }

    @Test
    fun adaptiveStageOneEasyAllowUsesLowerPortraitBandThanRegularAllow() {
        val easyAllowViews = listOf(
            view(label = "Full frame", role = SakosCompatibleCropRole.Context, nsfw = 0.09f),
            view(label = "Center crop", role = SakosCompatibleCropRole.Context, nsfw = 0.11f),
            view(label = "Upper-Center torso", role = SakosCompatibleCropRole.Detection, nsfw = 0.08f),
        )
        val sentinelStillRequiredViews = listOf(
            view(label = "Full frame", role = SakosCompatibleCropRole.Context, nsfw = 0.13f),
            view(label = "Center crop", role = SakosCompatibleCropRole.Context, nsfw = 0.14f),
            view(label = "Upper-Center torso", role = SakosCompatibleCropRole.Detection, nsfw = 0.11f),
        )

        assertTrue(SakosCompatibleMultiCropStrategy.shouldAdaptiveStage1EasyAllow(easyAllowViews))
        assertTrue(SakosCompatibleMultiCropStrategy.shouldAdaptiveStage1Allow(easyAllowViews))
        assertTrue(!SakosCompatibleMultiCropStrategy.shouldAdaptiveStage1EasyAllow(sentinelStillRequiredViews))
        assertTrue(SakosCompatibleMultiCropStrategy.shouldAdaptiveStage1Allow(sentinelStillRequiredViews))
    }

    @Test
    fun adaptiveStageOneDoesNotAllowWhenPortraitSentinelExceedsBand() {
        val stageOneViews = listOf(
            view(label = "Full frame", role = SakosCompatibleCropRole.Context, nsfw = 0.12f),
            view(label = "Center crop", role = SakosCompatibleCropRole.Context, nsfw = 0.19f),
            view(label = "Upper-Center torso", role = SakosCompatibleCropRole.Detection, nsfw = 0.08f),
            view(label = "Mid-Right chest sentinel", role = SakosCompatibleCropRole.Detection, nsfw = 0.24f),
        )

        assertTrue(!SakosCompatibleMultiCropStrategy.shouldAdaptiveStage1Allow(stageOneViews))
    }

    @Test
    fun adaptiveStageTwoAllowsOnlyWithoutTwoDetectionViewsAtOrAboveQuarterBand() {
        val accumulatedViews = listOf(
            view(label = "Full frame", role = SakosCompatibleCropRole.Context, nsfw = 0.18f),
            view(label = "Center crop", role = SakosCompatibleCropRole.Context, nsfw = 0.16f),
            view(label = "Upper-Center torso", role = SakosCompatibleCropRole.Detection, nsfw = 0.21f),
            view(label = "Upper-Right torso", role = SakosCompatibleCropRole.Detection, nsfw = 0.24f),
            view(label = "Upper-Right chest", role = SakosCompatibleCropRole.Detection, nsfw = 0.19f),
            view(label = "Bottom-Left half-tile", role = SakosCompatibleCropRole.Detection, nsfw = 0.23f),
            view(label = "Bottom-Right half-tile", role = SakosCompatibleCropRole.Detection, nsfw = 0.29f),
        )

        assertTrue(SakosCompatibleMultiCropStrategy.shouldAdaptiveStage2Allow(accumulatedViews))
    }

    @Test
    fun adaptiveStageTwoEscalatesWhenTwoDetectionViewsReachQuarterBand() {
        val accumulatedViews = listOf(
            view(label = "Full frame", role = SakosCompatibleCropRole.Context, nsfw = 0.18f),
            view(label = "Center crop", role = SakosCompatibleCropRole.Context, nsfw = 0.16f),
            view(label = "Upper-Center torso", role = SakosCompatibleCropRole.Detection, nsfw = 0.26f),
            view(label = "Upper-Right torso", role = SakosCompatibleCropRole.Detection, nsfw = 0.27f),
            view(label = "Upper-Right chest", role = SakosCompatibleCropRole.Detection, nsfw = 0.18f),
            view(label = "Bottom-Left half-tile", role = SakosCompatibleCropRole.Detection, nsfw = 0.22f),
            view(label = "Bottom-Right half-tile", role = SakosCompatibleCropRole.Detection, nsfw = 0.28f),
        )

        assertTrue(!SakosCompatibleMultiCropStrategy.shouldAdaptiveStage2Allow(accumulatedViews))
    }

    @Test
    fun strongestNearFloorLowerLateralViewRequiresTaggedNearFloorWindow() {
        val candidate = SakosCompatibleMultiCropStrategy.strongestNearFloorLowerLateralView(
            listOf(
                view(label = "Full frame", role = SakosCompatibleCropRole.Context, nsfw = 0.18f),
                view(
                    label = "Bottom-Right half-tile",
                    role = SakosCompatibleCropRole.Detection,
                    nsfw = 0.44f,
                    tags = setOf(
                        SakosCompatibleCropTag.SelfieSensitive,
                        SakosCompatibleCropTag.LowerLateral,
                    ),
                ),
                view(
                    label = "Upper-Right chest",
                    role = SakosCompatibleCropRole.Detection,
                    nsfw = 0.43f,
                    tags = setOf(
                        SakosCompatibleCropTag.SelfieSensitive,
                        SakosCompatibleCropTag.Chest,
                        SakosCompatibleCropTag.Lateral,
                    ),
                ),
                view(
                    label = "Bottom-Left half-tile",
                    role = SakosCompatibleCropRole.Detection,
                    nsfw = 0.42f,
                    tags = setOf(
                        SakosCompatibleCropTag.SelfieSensitive,
                        SakosCompatibleCropTag.LowerLateral,
                    ),
                ),
            ),
        )

        assertEquals("Bottom-Right half-tile", candidate?.cropWindow?.label)
        assertEquals(0.44f, candidate?.scores?.nsfwProbability ?: 0f, 0.0001f)
    }

    @Test
    fun strongestCropRefinementWindowsStayTightAroundLowerLateralAnchor() {
        val windows = SakosCompatibleMultiCropStrategy.adaptiveStrongestCropRefinementWindows(
            sourceWidth = 1080,
            sourceHeight = 1920,
            anchorWindow = SakosCompatibleCropWindow(
                label = "Bottom-Right half-tile",
                left = 540,
                top = 960,
                width = 540,
                height = 960,
                role = SakosCompatibleCropRole.Detection,
                tags = setOf(
                    SakosCompatibleCropTag.SelfieSensitive,
                    SakosCompatibleCropTag.LowerLateral,
                ),
            ),
        )

        assertEquals(
            listOf(
                "Lower-Right bust refine",
                "Right-edge chest refine",
                "Lower-Right inward refine",
            ),
            windows.map { it.label },
        )
        assertEquals(3, windows.size)
        assertTrue(windows.all { it.width < 540 && it.height < 960 })
        assertTrue(
            windows.all {
                it.tags.contains(SakosCompatibleCropTag.SelfieSensitive) &&
                    it.tags.contains(SakosCompatibleCropTag.LowerLateral) &&
                    it.tags.contains(SakosCompatibleCropTag.Chest)
            },
        )
    }

    private fun view(
        label: String,
        role: SakosCompatibleCropRole,
        nsfw: Float,
        tags: Set<SakosCompatibleCropTag> = emptySet(),
    ): LiveSakosRuntimeViewEvaluation = LiveSakosRuntimeViewEvaluation(
        cropWindow = SakosCompatibleCropWindow(
            label = label,
            left = 0,
            top = 0,
            width = 540,
            height = 960,
            role = role,
            tags = tags,
        ),
        scores = SakosCompatibleModelScores(
            sfwProbability = 1.0f - nsfw,
            nsfwProbability = nsfw,
        ),
        inferenceMillis = 8L,
    )
}
