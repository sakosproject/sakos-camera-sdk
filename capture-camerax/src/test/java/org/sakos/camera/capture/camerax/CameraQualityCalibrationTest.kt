package org.sakos.camera.capture.camerax

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import java.io.File
import java.security.MessageDigest
import java.util.Properties

class CameraQualityCalibrationTest {
    @Test
    fun debugTierParserMapsProductLabelsToSharedDefaultTier() {
        assertEquals(CameraCalibratedQualityTier.Low, CameraCalibratedQualityTier.fromDebugToken("low"))
        assertEquals(CameraCalibratedQualityTier.Default, CameraCalibratedQualityTier.fromDebugToken("normal"))
        assertEquals(CameraCalibratedQualityTier.Default, CameraCalibratedQualityTier.fromDebugToken("medium"))
        assertEquals(CameraCalibratedQualityTier.High, CameraCalibratedQualityTier.fromDebugToken("high"))
    }

    @Test
    fun profileHidesDuplicateVideoTiersFromVisibleSelector() {
        val profile = CameraQualityCalibrationProfile(
            deviceKey = "test-device",
            updatedAtMillis = 2L,
            records = listOf(
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Video,
                    tier = CameraCalibratedQualityTier.Default,
                    width = 590,
                    height = 1280,
                ),
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Video,
                    tier = CameraCalibratedQualityTier.High,
                    width = 590,
                    height = 1280,
                ),
            ),
        )

        assertEquals(
            listOf(CameraCalibratedQualityTier.Default),
            profile.visibleTiersFor(CameraQualityCalibrationMode.Video, "Back"),
        )
    }

    @Test
    fun calibratedSelectorFallsBackWhenRequestedTierDuplicatesDefault() {
        val profile = CameraQualityCalibrationProfile(
            deviceKey = "test-device",
            updatedAtMillis = 2L,
            records = listOf(
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Video,
                    tier = CameraCalibratedQualityTier.Default,
                    width = 590,
                    height = 1280,
                ),
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Video,
                    tier = CameraCalibratedQualityTier.High,
                    width = 1280,
                    height = 590,
                ),
            ),
        )

        assertEquals(
            CameraCalibratedQualityTier.Default,
            profile.calibratedTierFor(
                mode = CameraQualityCalibrationMode.Video,
                lensLabel = "Back",
                requestedTier = CameraCalibratedQualityTier.High,
            ),
        )
        kotlin.test.assertContains(
            profile.selectorDecisionLine(
                mode = CameraQualityCalibrationMode.Video,
                lensLabel = "Back",
                requestedTier = CameraCalibratedQualityTier.High,
            ),
            "active Medium because the requested tier is duplicate default",
        )
    }

    @Test
    fun graphPresetSelectionMapsRequestedTierToProbeGraph() {
        assertEquals(
            CameraQualityGraphPreset.PhotoLow,
            CameraQualityGraphPreset.selectorPresetFor(
                mode = CameraQualityCalibrationMode.Photo,
                tier = CameraCalibratedQualityTier.Low,
            ),
        )
        assertEquals(
            CameraQualityGraphPreset.PhotoStable,
            CameraQualityGraphPreset.selectorPresetFor(
                mode = CameraQualityCalibrationMode.Photo,
                tier = CameraCalibratedQualityTier.Default,
            ),
        )
        assertEquals(
            CameraQualityGraphPreset.PhotoHighStillProbeV2,
            CameraQualityGraphPreset.selectorPresetFor(
                mode = CameraQualityCalibrationMode.Photo,
                tier = CameraCalibratedQualityTier.High,
            ),
        )
        assertFalse(CameraQualityGraphPreset.PhotoHighProbe.selectorCandidate)
        assertEquals(
            CameraQualityGraphPreset.VideoLow,
            CameraQualityGraphPreset.selectorPresetFor(
                mode = CameraQualityCalibrationMode.Video,
                tier = CameraCalibratedQualityTier.Low,
            ),
        )
        assertEquals(
            CameraQualityGraphPreset.VideoStable,
            CameraQualityGraphPreset.selectorPresetFor(
                mode = CameraQualityCalibrationMode.Video,
                tier = CameraCalibratedQualityTier.Default,
            ),
        )
        assertEquals(
            CameraQualityGraphPreset.VideoHighFhdProbe,
            CameraQualityGraphPreset.selectorPresetFor(
                mode = CameraQualityCalibrationMode.Video,
                tier = CameraCalibratedQualityTier.High,
            ),
        )
    }

    @Test
    fun productVideoQualityClampsToDefaultWithoutDebugOverride() {
        assertEquals(
            CameraCalibratedQualityTier.Default,
            cameraProductEffectiveQualityTier(
                mode = CameraQualityCalibrationMode.Video,
                requestedTier = CameraCalibratedQualityTier.High,
                debugQualityOverrideActive = false,
            ),
        )
        assertEquals(
            CameraCalibratedQualityTier.Default,
            cameraProductEffectiveQualityTier(
                mode = CameraQualityCalibrationMode.Video,
                requestedTier = CameraCalibratedQualityTier.Low,
                debugQualityOverrideActive = false,
            ),
        )
        assertEquals(
            CameraCalibratedQualityTier.High,
            cameraProductEffectiveQualityTier(
                mode = CameraQualityCalibrationMode.Video,
                requestedTier = CameraCalibratedQualityTier.High,
                debugQualityOverrideActive = true,
            ),
        )
        assertEquals(
            CameraCalibratedQualityTier.High,
            cameraProductEffectiveQualityTier(
                mode = CameraQualityCalibrationMode.Photo,
                requestedTier = CameraCalibratedQualityTier.High,
                debugQualityOverrideActive = false,
            ),
        )
    }

    @Test
    fun v10MandatoryPlanUsesFiveActiveCapturesOneDerivedDecisionAndBackZoomProbes() {
        assertEquals(5, cameraQualityCalibrationRequiredActiveCaptureCount())
        assertEquals(1, cameraQualityCalibrationRequiredDerivedDecisionCount())
        assertEquals(6, cameraQualityCalibrationRequiredStepCount())
        assertEquals(8, cameraZoomCalibrationRequiredStepCount())
        assertEquals(14, cameraQualityCalibrationTotalRequiredStepCount())
    }

    @Test
    fun mandatoryReadinessDropsBackVideoLowAndHighButStillRequiresFrontVideoDefault() {
        val environment = CameraQualityCalibrationEnvironment.test()
        val profile = readyMandatoryProfile(environment)

        assertFalse(profile.records.any(::isBackVideoLowOrHighProbe))
        assertTrue(profile.mandatoryReadiness(environment).ready)

        val withoutFrontVideo = profile.copy(
            records = profile.records.filterNot { record ->
                record.mode == CameraQualityCalibrationMode.Video &&
                    record.lensLabel.equals("Front", ignoreCase = true) &&
                    record.graphPreset == CameraQualityGraphPreset.VideoStable
            },
        )
        val readiness = withoutFrontVideo.mandatoryReadiness(environment)

        assertFalse(readiness.ready)
        kotlin.test.assertContains(readiness.reason, "5 of 6 required graph probes")
    }

    @Test
    fun productZoomStopsAreFilteredByCameraXRangeAndCappedAtFiveX() {
        assertEquals(listOf(1f), cameraZoomCalibrationStopsForRange(minZoomRatio = 1f, maxZoomRatio = 1f))
        assertEquals(listOf(0.6f, 1f, 2f, 5f), cameraZoomCalibrationStopsForRange(minZoomRatio = 0.6f, maxZoomRatio = 5f))
        assertEquals(listOf(1f, 2f, 5f), cameraZoomCalibrationStopsForRange(minZoomRatio = 1f, maxZoomRatio = 10f))
        assertFalse(cameraZoomCalibrationProductStops().contains(10f))
    }

    @Test
    fun calibratedZoomStopsHideUnsupportedStopsAndTenXDiagnostics() {
        val profile = CameraQualityCalibrationProfile(
            deviceKey = "test-device",
            updatedAtMillis = 2L,
            records = emptyList(),
            zoomRecords = listOf(
                verifiedZoomRecord(CameraQualityCalibrationMode.Photo, requestedZoomRatio = 0.6f).copy(
                    status = CameraZoomCalibrationStatus.Unsupported,
                    appliedZoomRatio = 1f,
                    observedZoomRatio = 1f,
                    minZoomRatio = 1f,
                    maxZoomRatio = 10f,
                    failureReason = "outside CameraX zoom range",
                ),
                verifiedZoomRecord(CameraQualityCalibrationMode.Photo, requestedZoomRatio = 1f, minZoomRatio = 1f, maxZoomRatio = 10f),
                verifiedZoomRecord(CameraQualityCalibrationMode.Photo, requestedZoomRatio = 2f, minZoomRatio = 1f, maxZoomRatio = 10f),
                verifiedZoomRecord(CameraQualityCalibrationMode.Photo, requestedZoomRatio = 5f, minZoomRatio = 1f, maxZoomRatio = 10f),
                verifiedZoomRecord(
                    CameraQualityCalibrationMode.Photo,
                    requestedZoomRatio = 10f,
                    minZoomRatio = 1f,
                    maxZoomRatio = 10f,
                    diagnosticsOnly = true,
                ),
            ),
        ).withInferredSelectorEligibility()

        assertEquals(
            listOf(1f, 2f, 5f),
            profile.visibleZoomStopsFor(
                mode = CameraQualityCalibrationMode.Photo,
                lensLabel = "Back",
                currentMinZoomRatio = 1f,
                currentMaxZoomRatio = 10f,
            ),
        )
        assertFalse(checkNotNull(profile.zoomRecordFor(CameraQualityCalibrationMode.Photo, "Back", 10f)).productEligibility().eligible)
    }

    @Test
    fun selfieZoomStopsAreHiddenEvenWhenFrontRecordsExist() {
        val profile = CameraQualityCalibrationProfile(
            deviceKey = "test-device",
            updatedAtMillis = 2L,
            records = emptyList(),
            zoomRecords = cameraZoomCalibrationProductStops().map { zoomRatio ->
                verifiedZoomRecord(
                    mode = CameraQualityCalibrationMode.Photo,
                    lensLabel = "Front",
                    requestedZoomRatio = zoomRatio,
                    minZoomRatio = 1f,
                    maxZoomRatio = 8f,
                )
            },
        ).withInferredSelectorEligibility()

        assertEquals(
            emptyList(),
            profile.visibleZoomStopsFor(
                mode = CameraQualityCalibrationMode.Photo,
                lensLabel = "Front",
                currentMinZoomRatio = 1f,
                currentMaxZoomRatio = 8f,
            ),
        )
        assertEquals(
            emptyList(),
            profile.childFacingVisibleZoomStopsFor(
                mode = CameraQualityCalibrationMode.Photo,
                lensLabel = "Front",
                currentMinZoomRatio = 1f,
                currentMaxZoomRatio = 8f,
            ),
        )
    }

    @Test
    fun derivedPhotoLowDecisionIsEligibleOnlyWhenDefaultStillIsLargeAndProfileSupported() {
        val defaultStill = verifiedRecord(
            mode = CameraQualityCalibrationMode.Photo,
            tier = CameraCalibratedQualityTier.Default,
            width = 1884,
            height = 4080,
            graphPreset = CameraQualityGraphPreset.PhotoStable,
        )
        val derivedLow = cameraQualityCalibrationDerivedPhotoLowDecision(defaultStill, updatedAtMillis = 5L)

        assertEquals(CameraQualityCalibrationStatus.Verified, derivedLow.status)
        assertEquals(CameraQualityCalibrationProbePurpose.DerivedSelectorDecision, derivedLow.probePurpose)
        assertEquals(CameraQualityCalibrationGateCoverage.DerivedFromDefault, derivedLow.gateCoverage)
        assertEquals(defaultStill.recordKey(), derivedLow.derivedFromRecordKey)
        assertTrue((derivedLow.finalWidth ?: 0).toLong() * (derivedLow.finalHeight ?: 0).toLong() <= CAMERA_QUALITY_CALIBRATION_LOW_PHOTO_MAX_PIXELS)
        assertTrue(derivedLow.selectorEligibility(defaultStill).eligible)

        val unsupportedDefault = defaultStill.copy(gateSamplingProfileSelectorEligible = false)
        val unsupportedLow = cameraQualityCalibrationDerivedPhotoLowDecision(unsupportedDefault, updatedAtMillis = 6L)

        assertEquals(CameraQualityCalibrationStatus.Failed, unsupportedLow.status)
        assertNotNull(unsupportedLow.failureReason)
        assertFalse(unsupportedLow.selectorEligibility(unsupportedDefault).eligible)
    }

    @Test
    fun diagnosticsOnlyUhdProbeIsHiddenEvenWhenDistinct() {
        val profile = CameraQualityCalibrationProfile(
            deviceKey = "test-device",
            updatedAtMillis = 2L,
            records = listOf(
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Video,
                    tier = CameraCalibratedQualityTier.Default,
                    width = 720,
                    height = 1280,
                    graphPreset = CameraQualityGraphPreset.VideoStable,
                ),
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Video,
                    tier = CameraCalibratedQualityTier.High,
                    width = 2160,
                    height = 3840,
                    graphPreset = CameraQualityGraphPreset.VideoUhdProbe,
                ),
            ),
        )

        assertEquals(
            listOf(CameraCalibratedQualityTier.Default),
            profile.visibleTiersFor(CameraQualityCalibrationMode.Video, "Back"),
        )
    }

    @Test
    fun optionalDiagnosticsRecordsAreNotReadinessRequirementsOrVisibleTiers() {
        val environment = CameraQualityCalibrationEnvironment.test()
        val readyProfile = readyMandatoryProfile(environment)
        val profile = readyProfile.copy(
            records = readyProfile.records + optionalBackVideoLowAndHighRecords() + optionalBackVideoUhdRecord(),
        ).withInferredSelectorEligibility()

        assertTrue(profile.mandatoryReadiness(environment).ready)
        assertEquals(
            listOf(CameraCalibratedQualityTier.Default),
            profile.visibleTiersFor(CameraQualityCalibrationMode.Video, "Back"),
        )
    }

    @Test
    fun childFacingSelectorUsesOnlyEligibleBackTiersInLowMidHighOrder() {
        val environment = CameraQualityCalibrationEnvironment.test()
        val profile = readyMandatoryProfile(environment)

        assertEquals(
            listOf(CameraCalibratedQualityTier.Default, CameraCalibratedQualityTier.Low),
            profile.childFacingVisibleTiersFor(CameraQualityCalibrationMode.Photo, "Back"),
        )
        assertEquals(
            emptyList(),
            profile.childFacingVisibleTiersFor(CameraQualityCalibrationMode.Video, "Back"),
        )
        assertEquals(
            emptyList(),
            profile.childFacingVisibleTiersFor(CameraQualityCalibrationMode.Photo, "Front"),
        )
        assertEquals(
            emptyList(),
            profile.childFacingVisibleTiersFor(CameraQualityCalibrationMode.Video, "Front"),
        )
    }

    @Test
    fun frontNonDefaultVideoTierStaysHiddenForV1EvenWhenMeasured() {
        val profile = CameraQualityCalibrationProfile(
            deviceKey = "test-device",
            updatedAtMillis = 2L,
            records = listOf(
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Video,
                    tier = CameraCalibratedQualityTier.Default,
                    width = 720,
                    height = 1280,
                    graphPreset = CameraQualityGraphPreset.VideoStable,
                    lensLabel = "Front",
                ),
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Video,
                    tier = CameraCalibratedQualityTier.High,
                    width = 1080,
                    height = 1920,
                    graphPreset = CameraQualityGraphPreset.VideoHighFhdProbe,
                    lensLabel = "Front",
                ),
            ),
        )

        assertEquals(
            listOf(CameraCalibratedQualityTier.Default),
            profile.visibleTiersFor(CameraQualityCalibrationMode.Video, "Front"),
        )
        kotlin.test.assertContains(
            profile.selectorDecisionLine(
                mode = CameraQualityCalibrationMode.Video,
                lensLabel = "Front",
                requestedTier = CameraCalibratedQualityTier.High,
            ),
            "front quality selector not calibrated in v1",
        )
    }

    @Test
    fun fhdHighVideoProbeIsVisibleOnlyWhenActualOutputIsDistinctFromMedium() {
        val profile = CameraQualityCalibrationProfile(
            deviceKey = "test-device",
            updatedAtMillis = 2L,
            records = listOf(
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Video,
                    tier = CameraCalibratedQualityTier.Default,
                    width = 720,
                    height = 1280,
                    graphPreset = CameraQualityGraphPreset.VideoStable,
                    gateSamplingProfileId = "standard-16x9-portrait",
                    gateSamplingProfileLabel = "Standard 16:9 portrait",
                    gateSamplingRatio = 0.5625f,
                ),
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Video,
                    tier = CameraCalibratedQualityTier.High,
                    width = 1080,
                    height = 1920,
                    graphPreset = CameraQualityGraphPreset.VideoHighFhdProbe,
                    gateSamplingProfileId = "standard-16x9-portrait",
                    gateSamplingProfileLabel = "Standard 16:9 portrait",
                    gateSamplingRatio = 0.5625f,
                ),
            ),
        )

        assertEquals(
            listOf(CameraCalibratedQualityTier.Default, CameraCalibratedQualityTier.High),
            profile.visibleTiersFor(CameraQualityCalibrationMode.Video, "Back"),
        )
    }

    @Test
    fun selectorHidesDistinctTierWhenGateSamplingProfileIsUnsupported() {
        val profile = CameraQualityCalibrationProfile(
            deviceKey = "test-device",
            updatedAtMillis = 2L,
            records = listOf(
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Video,
                    tier = CameraCalibratedQualityTier.Default,
                    width = 590,
                    height = 1280,
                ),
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Video,
                    tier = CameraCalibratedQualityTier.High,
                    width = 1000,
                    height = 1950,
                    graphPreset = CameraQualityGraphPreset.VideoHighFhdProbe,
                    gateSamplingProfileId = "unsupported-ratio",
                    gateSamplingProfileLabel = "Unsupported ratio",
                    gateSamplingRatio = 0.513f,
                    gateSamplingProfileSelectorEligible = false,
                ),
            ),
        )

        assertEquals(
            listOf(CameraCalibratedQualityTier.Default),
            profile.visibleTiersFor(CameraQualityCalibrationMode.Video, "Back"),
        )
        kotlin.test.assertContains(
            profile.selectorDecisionLine(
                mode = CameraQualityCalibrationMode.Video,
                lensLabel = "Back",
                requestedTier = CameraCalibratedQualityTier.High,
            ),
            "unsupported gate sampling profile",
        )
    }

    @Test
    fun selectorHidesDistinctVideoTierWhenSamplesUseMixedGateProfiles() {
        val profile = CameraQualityCalibrationProfile(
            deviceKey = "test-device",
            updatedAtMillis = 2L,
            records = listOf(
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Video,
                    tier = CameraCalibratedQualityTier.Default,
                    width = 590,
                    height = 1280,
                ),
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Video,
                    tier = CameraCalibratedQualityTier.High,
                    width = 1080,
                    height = 1920,
                    graphPreset = CameraQualityGraphPreset.VideoHighFhdProbe,
                    gateSamplingProfileId = "mixed",
                    gateSamplingProfileLabel = "Mixed sampling profiles",
                    gateSamplingRatio = null,
                    gateSamplingOrientation = "Mixed",
                    gateSamplingProfileSelectorEligible = false,
                ),
            ),
        )

        assertEquals(
            listOf(CameraCalibratedQualityTier.Default),
            profile.visibleTiersFor(CameraQualityCalibrationMode.Video, "Back"),
        )
    }

    @Test
    fun selectorHidesDistinctTierWhenGateSamplingProfileWasNotMeasured() {
        val profile = CameraQualityCalibrationProfile(
            deviceKey = "test-device",
            updatedAtMillis = 2L,
            records = listOf(
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Photo,
                    tier = CameraCalibratedQualityTier.Default,
                    width = 1884,
                    height = 4080,
                ),
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Photo,
                    tier = CameraCalibratedQualityTier.High,
                    width = 3024,
                    height = 4032,
                    graphPreset = CameraQualityGraphPreset.PhotoHighStillProbeV2,
                    gateSamplingProfileId = null,
                    gateSamplingProfileLabel = null,
                    gateSamplingRatio = null,
                    gateSamplingProfileSelectorEligible = null,
                ),
            ),
        )

        assertEquals(
            listOf(CameraCalibratedQualityTier.Default),
            profile.visibleTiersFor(CameraQualityCalibrationMode.Photo, "Back"),
        )
    }

    @Test
    fun photoHighStillProbeV2IsVisibleOnlyWhenMateriallyLargerAndGateSupported() {
        val profile = CameraQualityCalibrationProfile(
            deviceKey = "test-device",
            updatedAtMillis = 2L,
            records = listOf(
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Photo,
                    tier = CameraCalibratedQualityTier.Default,
                    width = 1884,
                    height = 4080,
                    graphPreset = CameraQualityGraphPreset.PhotoStable,
                ),
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Photo,
                    tier = CameraCalibratedQualityTier.High,
                    width = 3024,
                    height = 4032,
                    graphPreset = CameraQualityGraphPreset.PhotoHighStillProbeV2,
                    gateSamplingProfileId = "classic-4x3-portrait",
                    gateSamplingProfileLabel = "Classic 4:3 portrait",
                    gateSamplingRatio = 0.75f,
                ),
            ),
        )

        assertEquals(
            listOf(CameraCalibratedQualityTier.Default, CameraCalibratedQualityTier.High),
            profile.visibleTiersFor(CameraQualityCalibrationMode.Photo, "Back"),
        )
    }

    @Test
    fun photoHighStillProbeV2HidesWhenNotMateriallyLargerThanDefault() {
        val profile = CameraQualityCalibrationProfile(
            deviceKey = "test-device",
            updatedAtMillis = 2L,
            records = listOf(
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Photo,
                    tier = CameraCalibratedQualityTier.Default,
                    width = 1884,
                    height = 4080,
                    graphPreset = CameraQualityGraphPreset.PhotoStable,
                ),
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Photo,
                    tier = CameraCalibratedQualityTier.High,
                    width = 1920,
                    height = 4080,
                    graphPreset = CameraQualityGraphPreset.PhotoHighStillProbeV2,
                ),
            ),
        )

        assertEquals(
            listOf(CameraCalibratedQualityTier.Default),
            profile.visibleTiersFor(CameraQualityCalibrationMode.Photo, "Back"),
        )
        kotlin.test.assertContains(
            profile.selectorDecisionLine(
                mode = CameraQualityCalibrationMode.Photo,
                lensLabel = "Back",
                requestedTier = CameraCalibratedQualityTier.High,
            ),
            "not materially larger than default",
        )
    }

    @Test
    fun photoHighStillProbeV2HidesUnsupportedRatioAndCleanupFailure() {
        val defaultRecord = verifiedRecord(
            mode = CameraQualityCalibrationMode.Photo,
            tier = CameraCalibratedQualityTier.Default,
            width = 1884,
            height = 4080,
            graphPreset = CameraQualityGraphPreset.PhotoStable,
        )
        val unsupportedHigh = verifiedRecord(
            mode = CameraQualityCalibrationMode.Photo,
            tier = CameraCalibratedQualityTier.High,
            width = 1000,
            height = 1950,
            graphPreset = CameraQualityGraphPreset.PhotoHighStillProbeV2,
            gateSamplingProfileId = "unsupported-ratio",
            gateSamplingProfileLabel = "Unsupported ratio",
            gateSamplingRatio = 0.513f,
            gateSamplingProfileSelectorEligible = false,
        )
        val cleanupFailedHigh = unsupportedHigh.copy(
            finalWidth = 3024,
            finalHeight = 4032,
            gateSamplingProfileId = "classic-4x3-portrait",
            gateSamplingProfileLabel = "Classic 4:3 portrait",
            gateSamplingRatio = 0.75f,
            gateSamplingProfileSelectorEligible = true,
            calibrationArtifactCleanupSucceeded = false,
            calibrationArtifactCleanupSummary = "test cleanup failed",
        )

        assertFalse(unsupportedHigh.selectorEligibility(defaultRecord).eligible)
        kotlin.test.assertContains(
            unsupportedHigh.selectorEligibility(defaultRecord).reason,
            "unsupported gate sampling profile",
        )
        assertFalse(cleanupFailedHigh.selectorEligibility(defaultRecord).eligible)
        kotlin.test.assertContains(
            cleanupFailedHigh.selectorEligibility(defaultRecord).reason,
            "cleanup failed",
        )
    }

    @Test
    fun frontStillQualityTiersStayHiddenEvenWhenPhotoHighIsMeasured() {
        val profile = CameraQualityCalibrationProfile(
            deviceKey = "test-device",
            updatedAtMillis = 2L,
            records = listOf(
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Photo,
                    tier = CameraCalibratedQualityTier.Default,
                    width = 1600,
                    height = 3500,
                    graphPreset = CameraQualityGraphPreset.PhotoStable,
                    lensLabel = "Front",
                ),
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Photo,
                    tier = CameraCalibratedQualityTier.High,
                    width = 3024,
                    height = 4032,
                    graphPreset = CameraQualityGraphPreset.PhotoHighStillProbeV2,
                    gateSamplingProfileId = "classic-4x3-portrait",
                    gateSamplingProfileLabel = "Classic 4:3 portrait",
                    gateSamplingRatio = 0.75f,
                    lensLabel = "Front",
                ),
            ),
        )

        assertEquals(
            listOf(CameraCalibratedQualityTier.Default),
            profile.visibleTiersFor(CameraQualityCalibrationMode.Photo, "Front"),
        )
        assertEquals(
            emptyList(),
            profile.childFacingVisibleTiersFor(CameraQualityCalibrationMode.Photo, "Front"),
        )
    }

    @Test
    fun gateStrategySelectionDoesNotDependOnLiveImageAnalysis() {
        assertFalse(cameraQualityGateDependsOnLiveAnalysis(CameraQualityGraphPreset.VideoHighFhdProbe))
        assertFalse(cameraQualityGateDependsOnLiveAnalysis(CameraQualityGraphPreset.PhotoHighStillProbeV2))
        assertFalse(CameraQualityGraphPreset.VideoHighFhdProbe.includesLiveAnalysis)
        assertFalse(CameraQualityGraphPreset.PhotoHighStillProbeV2.includesLiveAnalysis)
    }

    @Test
    fun calibratedSelectorKeepsDistinctLowTierActive() {
        val profile = CameraQualityCalibrationProfile(
            deviceKey = "test-device",
            updatedAtMillis = 2L,
            records = listOf(
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Video,
                    tier = CameraCalibratedQualityTier.Low,
                    width = 332,
                    height = 720,
                ),
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Video,
                    tier = CameraCalibratedQualityTier.Default,
                    width = 590,
                    height = 1280,
                ),
            ),
        )

        assertEquals(
            CameraCalibratedQualityTier.Low,
            profile.calibratedTierFor(
                mode = CameraQualityCalibrationMode.Video,
                lensLabel = "Back",
                requestedTier = CameraCalibratedQualityTier.Low,
            ),
        )
    }

    @Test
    fun profileKeepsDistinctPhotoLowTierVisible() {
        val profile = CameraQualityCalibrationProfile(
            deviceKey = "test-device",
            updatedAtMillis = 2L,
            records = listOf(
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Photo,
                    tier = CameraCalibratedQualityTier.Low,
                    width = 1236,
                    height = 2678,
                ),
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Photo,
                    tier = CameraCalibratedQualityTier.Default,
                    width = 1884,
                    height = 4080,
                ),
            ),
        )

        assertEquals(
            listOf(CameraCalibratedQualityTier.Default, CameraCalibratedQualityTier.Low),
            profile.visibleTiersFor(CameraQualityCalibrationMode.Photo, "Back"),
        )
    }

    @Test
    fun summaryMarksDuplicateHighAsHidden() {
        val profile = CameraQualityCalibrationProfile(
            deviceKey = "test-device",
            updatedAtMillis = 2L,
            records = listOf(
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Photo,
                    tier = CameraCalibratedQualityTier.Default,
                    width = 1884,
                    height = 4080,
                ),
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Photo,
                    tier = CameraCalibratedQualityTier.High,
                    width = 4080,
                    height = 1884,
                ),
            ),
        )

        val summary = profile.summaryLines(
            activeMode = CameraQualityCalibrationMode.Photo,
            activeLensLabel = "Back",
            selectedTier = CameraCalibratedQualityTier.High,
        ).joinToString(separator = "\n")

        kotlin.test.assertContains(summary, "selector hidden: duplicate default")
    }

    @Test
    fun secureProfileRoundTripsThroughAuthenticatedStore() {
        val environment = CameraQualityCalibrationEnvironment.test()
        val profile = readyMandatoryProfile(environment)
        val file = temporaryCalibrationFile()
        val cipher = TestCameraQualityCalibrationCipher()

        CameraQualityCalibrationStore(environment).saveForTest(
            file = file,
            profile = profile,
            cipher = cipher,
            environment = environment,
        )
        val restored = CameraQualityCalibrationStore(environment).loadForTest(
            file = file,
            cipher = cipher,
            environment = environment,
        )

        assertEquals(profile.records.size, restored.records.size)
        assertTrue(restored.mandatoryReadiness(environment).ready)
    }

    @Test
    fun secureProfilePayloadIsJsonV9() {
        val environment = CameraQualityCalibrationEnvironment.test()
        val file = temporaryCalibrationFile()
        val cipher = TestCameraQualityCalibrationCipher()

        CameraQualityCalibrationStore(environment).saveForTest(
            file = file,
            profile = readyMandatoryProfile(environment),
            cipher = cipher,
            environment = environment,
        )

        val envelope = Properties().apply {
            file.inputStream().use(::load)
        }
        assertEquals(CAMERA_QUALITY_CALIBRATION_SCHEMA_VERSION.toString(), envelope.getProperty("version"))
        assertEquals("false", envelope.getProperty("debugOnly"))
        assertEquals("device-camera-quality-calibration", envelope.getProperty("scope"))
        assertEquals("json", envelope.getProperty("payload"))
        val plainText = cipher.lastEncryptedPlainText?.toString(Charsets.UTF_8).orEmpty()
        assertTrue(plainText.trimStart().startsWith("{"))
        kotlin.test.assertContains(plainText, "\"schemaVersion\":$CAMERA_QUALITY_CALIBRATION_SCHEMA_VERSION")
        kotlin.test.assertContains(plainText, "\"records\"")
    }

    @Test
    fun profileSummarizesCalibrationTimingAndGateMode() {
        val environment = CameraQualityCalibrationEnvironment.test()
        val profile = readyMandatoryProfile(environment).copy(
            calibrationStartedAtMillis = 1_000L,
            calibrationCompletedAtMillis = 5_000L,
            calibrationTotalDurationMillis = 4_000L,
            records = readyMandatoryProfile(environment).records.mapIndexed { index, record ->
                record.copy(
                    calibrationBatchStartedAtMillis = 1_000L,
                    calibrationStepIndex = index + 1,
                    calibrationStepCount = cameraQualityCalibrationRequiredStepCount(),
                    calibrationStepStartedAtMillis = 1_000L + (index * 250L),
                    calibrationStepCompletedAtMillis = 1_200L + (index * 250L),
                    calibrationStepDurationMillis = if (
                        record.mode == CameraQualityCalibrationMode.Video &&
                        record.lensLabel.equals("Back", ignoreCase = true) &&
                        record.graphPreset == CameraQualityGraphPreset.VideoStable
                    ) {
                        1_400L
                    } else {
                        200L
                    },
                    calibrationBindWaitMillis = 50L,
                    calibrationCaptureOrRecordMillis = 100L,
                    calibrationRecordingTargetMillis = if (record.mode == CameraQualityCalibrationMode.Video) 1_500L else null,
                    calibrationGateMode = record.gateCoverage?.displayLabel,
                    calibrationGateRequiredForSelector = record.graphPreset.selectorCandidate,
                )
            },
        )

        val summary = profile.summaryLines(
            activeMode = CameraQualityCalibrationMode.Video,
            activeLensLabel = "Back",
            selectedTier = CameraCalibratedQualityTier.High,
        ).joinToString(separator = "\n")

        kotlin.test.assertContains(summary, "Calibration timing: total 4.0s")
        kotlin.test.assertContains(summary, "Slowest calibration step: VideoStable")
        kotlin.test.assertContains(summary, "timing step")
        kotlin.test.assertContains(summary, "gate mode fast single-frame video gate")
    }

    @Test
    fun selectorHidesCandidateWhenGateWasNotFullyEvaluated() {
        val profile = CameraQualityCalibrationProfile(
            deviceKey = "test-device",
            updatedAtMillis = 2L,
            records = listOf(
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Video,
                    tier = CameraCalibratedQualityTier.Default,
                    width = 720,
                    height = 1280,
                    graphPreset = CameraQualityGraphPreset.VideoStable,
                ),
                verifiedRecord(
                    mode = CameraQualityCalibrationMode.Video,
                    tier = CameraCalibratedQualityTier.High,
                    width = 1080,
                    height = 1920,
                    graphPreset = CameraQualityGraphPreset.VideoHighFhdProbe,
                ).copy(
                    calibrationGateMode = "metadata-only test",
                    calibrationGateRequiredForSelector = false,
                ),
            ),
        )

        assertEquals(
            listOf(CameraCalibratedQualityTier.Default),
            profile.visibleTiersFor(CameraQualityCalibrationMode.Video, "Back"),
        )
    }

    @Test
    fun fastVideoProbeRetriesWhenSmokeEvidenceIsIncomplete() {
        val fastRecord = verifiedRecord(
            mode = CameraQualityCalibrationMode.Video,
            tier = CameraCalibratedQualityTier.High,
            width = 1080,
            height = 1920,
            graphPreset = CameraQualityGraphPreset.VideoHighFhdProbe,
        )

        assertFalse(cameraQualityCalibrationNeedsFullTemporalRetry(fastRecord))
        assertTrue(cameraQualityCalibrationNeedsFullTemporalRetry(fastRecord.copy(gateEvaluatedFrameCount = 0)))
        assertTrue(cameraQualityCalibrationNeedsFullTemporalRetry(fastRecord.copy(gateSamplingProfileSelectorEligible = false)))
        assertTrue(cameraQualityCalibrationNeedsFullTemporalRetry(fastRecord.copy(calibrationArtifactCleanupSucceeded = false)))
        assertFalse(
            cameraQualityCalibrationNeedsFullTemporalRetry(
                fastRecord.copy(
                    fastProbeRetried = true,
                    gateCoverage = CameraQualityCalibrationGateCoverage.FullTemporalFallback,
                ),
            ),
        )
    }

    @Test
    fun secureProfileTamperFailsClosed() {
        val environment = CameraQualityCalibrationEnvironment.test()
        val file = temporaryCalibrationFile()
        val cipher = TestCameraQualityCalibrationCipher()
        CameraQualityCalibrationStore(environment).saveForTest(
            file = file,
            profile = readyMandatoryProfile(environment),
            cipher = cipher,
            environment = environment,
        )

        file.writeText(file.readText().replace("cipherText", "cipherTextTampered"))

        kotlin.test.assertFailsWith<Exception> {
            CameraQualityCalibrationStore(environment).loadForTest(
                file = file,
                cipher = cipher,
                environment = environment,
            )
        }
    }

    @Test
    fun sdkAppVersionChangeInvalidatesReadyProfile() {
        val environment = CameraQualityCalibrationEnvironment.test().copy(
            appVersionName = "0.1.0",
            appVersionCode = 1L,
        )
        val upgradedEnvironment = environment.copy(
            appVersionName = "0.1.1",
            appVersionCode = 2L,
        )
        val profile = readyMandatoryProfile(environment)

        assertFalse(profile.mandatoryReadiness(upgradedEnvironment).ready)
    }

    @Test
    fun schemaChangeInvalidatesMandatoryReadiness() {
        val environment = CameraQualityCalibrationEnvironment.test()
        val profile = readyMandatoryProfile(environment)

        val readiness = profile.mandatoryReadiness(
            environment.copy(schemaVersion = environment.schemaVersion + 1),
        )

        assertFalse(readiness.ready)
        kotlin.test.assertContains(readiness.reason, "schema changed")
    }

    @Test
    fun mandatoryReadinessRequiresCleanupProof() {
        val environment = CameraQualityCalibrationEnvironment.test()
        val profile = readyMandatoryProfile(environment).copy(
            records = readyMandatoryProfile(environment).records.mapIndexed { index, record ->
                if (index == 0) {
                    record.copy(calibrationArtifactCleanupSucceeded = false)
                } else {
                    record
                }
            },
        )

        val readiness = profile.mandatoryReadiness(environment)

        assertFalse(readiness.ready)
        kotlin.test.assertContains(readiness.reason, "cleanup")
    }

    @Test
    fun mandatoryReadinessRequiresZoomCalibrationRecords() {
        val environment = CameraQualityCalibrationEnvironment.test()
        val profile = readyMandatoryProfile(environment).copy(
            zoomRecords = emptyList(),
        )

        val readiness = profile.mandatoryReadiness(environment)

        assertFalse(readiness.ready)
        kotlin.test.assertContains(readiness.reason, "Zoom calibration incomplete")
    }

    @Test
    fun mandatoryReadinessDoesNotRequireSelfieZoomCalibrationRecords() {
        val environment = CameraQualityCalibrationEnvironment.test()
        val profile = readyMandatoryProfile(environment)

        assertTrue(profile.zoomRecords.none { record ->
            record.lensLabel.equals("Front", ignoreCase = true)
        })
        assertTrue(profile.mandatoryReadiness(environment).ready)
    }

    @Test
    fun legacyVersionThreeProfileCannotSatisfyMandatoryReadiness() {
        val environment = CameraQualityCalibrationEnvironment.test()
        val profile = readyMandatoryProfile(environment).copy(schemaVersion = 3)

        val readiness = profile.mandatoryReadiness(environment)

        assertFalse(readiness.ready)
        kotlin.test.assertContains(readiness.reason, "schema changed")
    }

    @Test
    fun optionalBackVideoLowAndHighProbeFailuresDoNotBlockMandatoryReadiness() {
        val environment = CameraQualityCalibrationEnvironment.test()
        val readyProfile = readyMandatoryProfile(environment)
        val failedOptionalRecords = optionalBackVideoLowAndHighRecords().map(::failedOptional)
        val profile = readyProfile.copy(
            records = readyProfile.records + failedOptionalRecords,
        ).withInferredSelectorEligibility()

        assertTrue(profile.mandatoryReadiness(environment).ready)
        assertEquals(
            listOf(CameraCalibratedQualityTier.Default),
            profile.visibleTiersFor(CameraQualityCalibrationMode.Video, "Back"),
        )
    }

    private fun isBackVideoLowOrHighProbe(record: CameraQualityCalibrationRecord): Boolean =
        record.mode == CameraQualityCalibrationMode.Video &&
            record.lensLabel.equals("Back", ignoreCase = true) &&
            (
                record.graphPreset == CameraQualityGraphPreset.VideoLow ||
                    record.graphPreset == CameraQualityGraphPreset.VideoHighFhdProbe
                )

    private fun withOptionalDiagnostics(record: CameraQualityCalibrationRecord): CameraQualityCalibrationRecord =
        record.copy(
            probePurpose = CameraQualityCalibrationProbePurpose.OptionalDiagnostics,
            mandatoryForReadiness = false,
            calibrationGateRequiredForSelector = false,
        )

    private fun optionalBackVideoLowAndHighRecords(): List<CameraQualityCalibrationRecord> =
        listOf(
            verifiedRecord(
                mode = CameraQualityCalibrationMode.Video,
                tier = CameraCalibratedQualityTier.Low,
                width = 480,
                height = 854,
                graphPreset = CameraQualityGraphPreset.VideoLow,
            ),
            verifiedRecord(
                mode = CameraQualityCalibrationMode.Video,
                tier = CameraCalibratedQualityTier.High,
                width = 1080,
                height = 1920,
                graphPreset = CameraQualityGraphPreset.VideoHighFhdProbe,
            ),
        ).map(::withOptionalDiagnostics)

    private fun optionalBackVideoUhdRecord(): CameraQualityCalibrationRecord =
        withOptionalDiagnostics(
            verifiedRecord(
                mode = CameraQualityCalibrationMode.Video,
                tier = CameraCalibratedQualityTier.High,
                width = 2160,
                height = 3840,
                graphPreset = CameraQualityGraphPreset.VideoUhdProbe,
            ).copy(
                gateCoverage = CameraQualityCalibrationGateCoverage.MetadataOnly,
            ),
        )

    private fun failedOptional(record: CameraQualityCalibrationRecord): CameraQualityCalibrationRecord =
        withOptionalDiagnostics(
            record.copy(
                status = CameraQualityCalibrationStatus.Failed,
                finalWidth = null,
                finalHeight = null,
                failureReason = "bind failed",
            ),
        )

    private fun verifiedRecord(
        mode: CameraQualityCalibrationMode,
        tier: CameraCalibratedQualityTier,
        width: Int,
        height: Int,
        graphPreset: CameraQualityGraphPreset = CameraQualityGraphPreset.selectorPresetFor(mode, tier),
        gateSamplingProfileId: String? = "tall-phone-portrait",
        gateSamplingProfileLabel: String? = "Tall phone portrait",
        gateSamplingRatio: Float? = 0.461f,
        gateSamplingOrientation: String? = "Portrait",
        gateSamplingProfileSelectorEligible: Boolean? = true,
        lensLabel: String = "Back",
        cleanupSucceeded: Boolean? = true,
    ): CameraQualityCalibrationRecord {
        val probePurpose = when {
            mode == CameraQualityCalibrationMode.Photo &&
                tier == CameraCalibratedQualityTier.Low &&
                graphPreset == CameraQualityGraphPreset.PhotoLow -> CameraQualityCalibrationProbePurpose.DerivedSelectorDecision
            !graphPreset.selectorCandidate -> CameraQualityCalibrationProbePurpose.OptionalDiagnostics
            tier == CameraCalibratedQualityTier.Default -> CameraQualityCalibrationProbePurpose.RequiredDefault
            else -> CameraQualityCalibrationProbePurpose.RequiredSelectorCandidate
        }
        val gateCoverage = when {
            probePurpose == CameraQualityCalibrationProbePurpose.DerivedSelectorDecision -> CameraQualityCalibrationGateCoverage.DerivedFromDefault
            mode == CameraQualityCalibrationMode.Photo -> CameraQualityCalibrationGateCoverage.StillGate
            !graphPreset.selectorCandidate -> CameraQualityCalibrationGateCoverage.MetadataOnly
            else -> CameraQualityCalibrationGateCoverage.FastSingleFrame
        }
        return CameraQualityCalibrationRecord(
            mode = mode,
            lensLabel = lensLabel,
            tier = tier,
            status = CameraQualityCalibrationStatus.Verified,
            requestedPolicy = debugGraphPresetPolicyLabel(graphPreset),
            advertisedVideoQualities = null,
            boundStreamLabel = null,
            finalWidth = width,
            finalHeight = height,
            finalRotationDegrees = 0,
            durationMillis = if (mode == CameraQualityCalibrationMode.Video) 1_200L else null,
            fileSizeBytes = null,
            failureReason = null,
            updatedAtMillis = 1L,
            graphPreset = graphPreset,
            gateSamplingProfileId = gateSamplingProfileId,
            gateSamplingProfileLabel = gateSamplingProfileLabel,
            gateSamplingRatio = gateSamplingRatio,
            gateSamplingOrientation = gateSamplingOrientation,
            gateSamplingProfileSelectorEligible = gateSamplingProfileSelectorEligible,
            gateSampledFrameCount = if (mode == CameraQualityCalibrationMode.Video && gateCoverage != CameraQualityCalibrationGateCoverage.MetadataOnly) 1 else null,
            gateEvaluatedFrameCount = if (mode == CameraQualityCalibrationMode.Video && gateCoverage != CameraQualityCalibrationGateCoverage.MetadataOnly) 1 else null,
            calibrationGateMode = gateCoverage.displayLabel,
            calibrationGateRequiredForSelector = graphPreset.selectorCandidate &&
                probePurpose != CameraQualityCalibrationProbePurpose.OptionalDiagnostics,
            calibrationArtifactCleanupSucceeded = cleanupSucceeded,
            calibrationArtifactCleanupSummary = cleanupSucceeded?.let {
                if (it) "test cleanup confirmed" else "test cleanup failed"
            },
            probePurpose = probePurpose,
            gateCoverage = gateCoverage,
            derivedFromRecordKey = if (probePurpose == CameraQualityCalibrationProbePurpose.DerivedSelectorDecision) {
                "photo.back.default.photo-stable"
            } else {
                null
            },
            mandatoryForReadiness = probePurpose != CameraQualityCalibrationProbePurpose.OptionalDiagnostics,
        )
    }

    private fun verifiedZoomRecord(
        mode: CameraQualityCalibrationMode,
        requestedZoomRatio: Float,
        lensLabel: String = "Back",
        minZoomRatio: Float = 0.6f,
        maxZoomRatio: Float = 5f,
        diagnosticsOnly: Boolean = false,
    ): CameraZoomCalibrationRecord =
        CameraZoomCalibrationRecord(
            mode = mode,
            lensLabel = lensLabel,
            requestedZoomRatio = requestedZoomRatio,
            status = CameraZoomCalibrationStatus.Verified,
            minZoomRatio = minZoomRatio,
            maxZoomRatio = maxZoomRatio,
            appliedZoomRatio = requestedZoomRatio,
            observedZoomRatio = requestedZoomRatio,
            failureReason = null,
            updatedAtMillis = 1L,
            diagnosticsOnly = diagnosticsOnly,
        )

    private fun readyMandatoryProfile(
        environment: CameraQualityCalibrationEnvironment,
    ): CameraQualityCalibrationProfile {
        val records = listOf(
            verifiedRecord(CameraQualityCalibrationMode.Photo, CameraCalibratedQualityTier.Low, 1178, 2550, CameraQualityGraphPreset.PhotoLow),
            verifiedRecord(CameraQualityCalibrationMode.Photo, CameraCalibratedQualityTier.Default, 1800, 3900, CameraQualityGraphPreset.PhotoStable),
            verifiedRecord(CameraQualityCalibrationMode.Photo, CameraCalibratedQualityTier.High, 1800, 3900, CameraQualityGraphPreset.PhotoHighStillProbeV2),
            verifiedRecord(CameraQualityCalibrationMode.Photo, CameraCalibratedQualityTier.Default, 1600, 3500, CameraQualityGraphPreset.PhotoStable, lensLabel = "Front"),
            verifiedRecord(CameraQualityCalibrationMode.Video, CameraCalibratedQualityTier.Default, 720, 1280, CameraQualityGraphPreset.VideoStable),
            verifiedRecord(CameraQualityCalibrationMode.Video, CameraCalibratedQualityTier.Default, 720, 1280, CameraQualityGraphPreset.VideoStable, lensLabel = "Front"),
        )
        val zoomRecords = CameraQualityCalibrationMode.entries.flatMap { mode ->
            cameraZoomCalibrationProductStops().map { zoomRatio ->
                verifiedZoomRecord(
                    mode = mode,
                    lensLabel = "Back",
                    requestedZoomRatio = zoomRatio,
                )
            }
        }
        return CameraQualityCalibrationProfile(
            schemaVersion = environment.schemaVersion,
            deviceKey = environment.deviceKey,
            createdAtMillis = 1L,
            updatedAtMillis = 2L,
            appVersionName = environment.appVersionName,
            appVersionCode = environment.appVersionCode,
            modelId = environment.modelId,
            policyHash = environment.policyHash,
            cameraXGraphAssumption = environment.cameraXGraphAssumption,
            buildFingerprint = environment.buildFingerprint,
            requiredStepSetHash = environment.requiredStepSetHash,
            cameraInventoryHash = environment.cameraInventoryHash,
            records = records,
            zoomRecords = zoomRecords,
        ).withInferredSelectorEligibility()
    }

    private fun temporaryCalibrationFile(): File {
        val directory = kotlin.io.path.createTempDirectory("camera-calibration-test").toFile()
        directory.deleteOnExit()
        return File(directory, "profile.bin").apply {
            deleteOnExit()
        }
    }

    private class TestCameraQualityCalibrationCipher : CameraQualityCalibrationCipher {
        var lastEncryptedPlainText: ByteArray? = null
            private set

        override fun encrypt(
            plainText: ByteArray,
            associatedData: ByteArray,
        ): CameraQualityEncryptedPayload {
            lastEncryptedPlainText = plainText
            val tag = tagFor(plainText, associatedData)
            return CameraQualityEncryptedPayload(
                iv = "test-iv".toByteArray(),
                cipherText = tag + plainText,
            )
        }

        override fun decrypt(
            encryptedPayload: CameraQualityEncryptedPayload,
            associatedData: ByteArray,
        ): ByteArray {
            require(encryptedPayload.cipherText.size >= 16) {
                "ciphertext too short"
            }
            val tag = encryptedPayload.cipherText.take(16).toByteArray()
            val plainText = encryptedPayload.cipherText.drop(16).toByteArray()
            require(tag.contentEquals(tagFor(plainText, associatedData))) {
                "authentication failed"
            }
            return plainText
        }

        private fun tagFor(
            plainText: ByteArray,
            associatedData: ByteArray,
        ): ByteArray = MessageDigest.getInstance("SHA-256")
            .digest(associatedData + plainText)
            .take(16)
            .toByteArray()
    }
}
