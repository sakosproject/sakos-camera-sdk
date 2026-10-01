package org.sakos.camera.capture.camerax

import kotlin.test.*
import kotlinx.coroutines.*

class CameraCalibrationRunnerTest {
    private val environment = CameraQualityCalibrationEnvironment.test()
    private inner class Storage : CameraCalibrationProfileStorage {
        var profile = empty(); var resets = 0
        fun empty() = CameraQualityCalibrationProfile(environment.deviceKey, null, emptyList(), modelId = environment.modelId,
            policyHash = environment.policyHash, cameraXGraphAssumption = environment.cameraXGraphAssumption,
            buildFingerprint = environment.buildFingerprint, appVersionName = environment.appVersionName, appVersionCode = environment.appVersionCode,
            requiredStepSetHash = environment.requiredStepSetHash, cameraInventoryHash = environment.cameraInventoryHash)
        override fun load() = profile
        override fun reset() { resets++; profile = empty() }
        override fun record(record: CameraQualityCalibrationRecord): CameraQualityCalibrationProfile {
            var records = profile.records.filterNot { it.recordKey() == record.recordKey() } + record
            if (record.graphPreset == CameraQualityGraphPreset.PhotoStable && record.lensLabel == "back")
                records = records + cameraQualityCalibrationDerivedPhotoLowDecision(record, 0)
            profile = profile.copy(records = records); return profile
        }
        override fun recordZoom(record: CameraZoomCalibrationRecord): CameraQualityCalibrationProfile {
            profile = profile.copy(zoomRecords = profile.zoomRecords + record); return profile
        }
    }
    private open inner class Probe : CameraCalibrationProbe {
        var calls = 0; var cancelled = 0
        override suspend fun quality(step: CameraCalibrationStep, fullTemporal: Boolean): CameraQualityCalibrationRecord {
            calls++
            val video = step.preset.mode == CameraQualityCalibrationMode.Video
            return CameraQualityCalibrationRecord(step.preset.mode, step.lens, step.preset.tier, CameraQualityCalibrationStatus.Verified,
                "synthetic requested", "advertised synthetic", "measured synthetic", if (step.preset.tier == CameraCalibratedQualityTier.High) 4000 else 3000,
                if (step.preset.tier == CameraCalibratedQualityTier.High) 3000 else 2250, 0, if (video) 1200L else null, if (video) 100L else null, null, 0,
                graphPreset = step.preset, previewStable = true, gateStatusLabel = "simulated Allow",
                gateSamplingProfileId = "portrait-3-4", gateSamplingProfileSelectorEligible = true,
                gateSampledFrameCount = 1, gateEvaluatedFrameCount = 1, calibrationGateRequiredForSelector = true,
                calibrationArtifactCleanupSucceeded = true, probePurpose = step.purpose,
                mandatoryForReadiness = step.purpose != CameraQualityCalibrationProbePurpose.OptionalDiagnostics,
                gateCoverage = if (!video) CameraQualityCalibrationGateCoverage.StillGate else if (fullTemporal) CameraQualityCalibrationGateCoverage.FullTemporalFallback else CameraQualityCalibrationGateCoverage.FastSingleFrame)
        }
        override suspend fun zoom(lens: String, mode: CameraQualityCalibrationMode, ratios: List<Float>) = ratios.map { ratio ->
            CameraZoomCalibrationRecord(mode, lens, ratio, if (ratio < 1f) CameraZoomCalibrationStatus.Unsupported else CameraZoomCalibrationStatus.Verified,
                1f, 5f, if (ratio < 1f) 1f else ratio, if (ratio < 1f) 1f else ratio, if (ratio < 1f) "outside range" else null, 0)
        }
        override fun cancel() { cancelled++ }
    }
    @Test fun firstRunReadyCachesAndExplicitRetryReruns() = runBlocking {
        val storage = Storage(); val probe = Probe(); val runner = CameraCalibrationRunner(environment, storage, probe)
        assertTrue(runner.run().mandatoryReadiness(environment).ready); assertEquals(5, probe.calls)
        runner.run(); assertEquals(5, probe.calls)
        runner.run(retry = true); assertEquals(10, probe.calls); assertEquals(2, storage.resets)
    }
    @Test fun advertisedHighCannotHideFailedDefaultOrCleanup() = runBlocking {
        for (cleanupFailure in listOf(false, true)) {
            val storage = Storage(); val probe = object : Probe() {
                override suspend fun quality(step: CameraCalibrationStep, fullTemporal: Boolean): CameraQualityCalibrationRecord {
                    val record = super.quality(step, fullTemporal)
                    return if (step.preset == CameraQualityGraphPreset.PhotoStable && step.lens == "back") {
                        if (cleanupFailure) record.copy(calibrationArtifactCleanupSucceeded = false)
                        else record.copy(status = CameraQualityCalibrationStatus.Failed, failureReason = "simulated probe failure")
                    } else record
                }
            }
            val profile = CameraCalibrationRunner(environment, storage, probe).run()
            assertFalse(profile.mandatoryReadiness(environment).ready)
            assertFalse(profile.recordFor(CameraQualityCalibrationMode.Photo, "back", CameraCalibratedQualityTier.Default)?.selectorEligibility(null)?.eligible == true)
        }
    }
    @Test fun failedFastProbeRetriesOnceWithFullTemporalEvidence() = runBlocking {
        val probe = object : Probe() {
            override suspend fun quality(step: CameraCalibrationStep, fullTemporal: Boolean): CameraQualityCalibrationRecord {
                val record = super.quality(step, fullTemporal)
                return if (step.preset.mode == CameraQualityCalibrationMode.Video && !fullTemporal)
                    record.copy(status = CameraQualityCalibrationStatus.Failed, failureReason = "simulated decode failure", gateEvaluatedFrameCount = 0) else record
            }
        }
        val profile = CameraCalibrationRunner(environment, Storage(), probe).run()
        assertEquals(7, probe.calls); assertTrue(profile.mandatoryReadiness(environment).ready)
        assertTrue(profile.records.filter { it.mode == CameraQualityCalibrationMode.Video }.all { it.fastProbeRetried == true })
    }
    @Test fun cancellationCleansProbeAndCannotPersistLateRecordOrReady() = runBlocking {
        val storage = Storage(); val entered = CompletableDeferred<Unit>()
        val probe = object : Probe() {
            override suspend fun quality(step: CameraCalibrationStep, fullTemporal: Boolean): CameraQualityCalibrationRecord {
                entered.complete(Unit); awaitCancellation()
            }
        }
        val job = launch { CameraCalibrationRunner(environment, storage, probe).run() }
        entered.await(); job.cancelAndJoin()
        assertEquals(1, probe.cancelled); assertTrue(storage.profile.records.isEmpty()); assertFalse(storage.profile.mandatoryReadiness(environment).ready)
    }
    @Test fun concurrentRunRejectedAndDiagnosticGraphsRemainNonSelectors() = runBlocking {
        val storage = Storage(); val entered = CompletableDeferred<Unit>()
        val probe = object : Probe() {
            override suspend fun quality(step: CameraCalibrationStep, fullTemporal: Boolean): CameraQualityCalibrationRecord {
                entered.complete(Unit); awaitCancellation()
            }
        }
        val runner = CameraCalibrationRunner(environment, storage, probe)
        val job = launch { runner.run() }; entered.await()
        assertFailsWith<IllegalStateException> { runner.run() }; job.cancelAndJoin()
        val profile = CameraCalibrationRunner(environment, storage, Probe()).run(optionalDiagnostics = true)
        assertTrue(profile.mandatoryReadiness(environment).ready)
        assertEquals(listOf(CameraCalibratedQualityTier.Default), profile.visibleTiersFor(CameraQualityCalibrationMode.Video, "back"))
        assertFalse(CameraQualityGraphPreset.PhotoHighProbe.selectorCandidate); assertFalse(CameraQualityGraphPreset.VideoUhdProbe.selectorCandidate)
    }
    @Test fun inventoryFingerprintIncludesAdvertisedChangesButNotEnumerationOrder() {
        val back = CameraAdvertisedCapabilities("back", "0", true, 1f, 5f, listOf("HD", "SD"))
        val front = CameraAdvertisedCapabilities("front", "1", false, 1f, 1f, listOf("SD"))
        assertEquals(CameraCapabilityDiscovery.inventoryHash(listOf(back, front)), CameraCapabilityDiscovery.inventoryHash(listOf(front, back)))
        assertNotEquals(CameraCapabilityDiscovery.inventoryHash(listOf(back, front)), CameraCapabilityDiscovery.inventoryHash(listOf(back.copy(maxZoom = 2f), front)))
        assertNotEquals(CameraCapabilityDiscovery.inventoryHash(listOf(back, front)), CameraCapabilityDiscovery.inventoryHash(listOf(back)))
    }
    @Test fun outputRotationBoundariesAndUnknownRetainCallerChoice() {
        assertEquals(null, CameraOutputRotation.fromOrientationDegrees(-1))
        assertEquals(0, CameraOutputRotation.fromOrientationDegrees(44))
        assertEquals(3, CameraOutputRotation.fromOrientationDegrees(45))
        assertEquals(3, CameraOutputRotation.fromOrientationDegrees(134))
        assertEquals(2, CameraOutputRotation.fromOrientationDegrees(135))
        assertEquals(1, CameraOutputRotation.fromOrientationDegrees(225))
        assertEquals(0, CameraOutputRotation.fromOrientationDegrees(315))
        assertFailsWith<IllegalArgumentException> { CameraOutputRotation.fromOrientationDegrees(360) }
    }
    @Test fun timedOutOrThrowingProbeProducesRecoverableNonReadyProfile() = runBlocking {
        for (timedOut in listOf(false, true)) {
            val storage = Storage(); val probe = object : Probe() {
                override suspend fun quality(step: CameraCalibrationStep, fullTemporal: Boolean): CameraQualityCalibrationRecord {
                    if (timedOut) withTimeout(1) { delay(1000) }
                    throw IllegalStateException("simulated failure")
                }
            }
            val result = CameraCalibrationRunner(environment, storage, probe).run()
            assertFalse(result.mandatoryReadiness(environment).ready)
            assertTrue(result.records.all { it.status == CameraQualityCalibrationStatus.Failed })
            assertTrue(probe.cancelled > 0)
        }
    }
    @Test fun duplicateZoomStopsCannotCreateReadyProfile() = runBlocking {
        val storage = Storage(); val probe = object : Probe() {
            override suspend fun zoom(lens: String, mode: CameraQualityCalibrationMode, ratios: List<Float>) =
                super.zoom(lens, mode, ratios).map { it.copy(requestedZoomRatio = 2f) }
        }
        assertFailsWith<IllegalArgumentException> { CameraCalibrationRunner(environment, storage, probe).run() }
        assertTrue(storage.profile.zoomRecords.isEmpty())
        assertFalse(storage.profile.mandatoryReadiness(environment).ready)
    }
    @Test fun canceledZoomCannotPersistReturnedLateEvidence() = runBlocking {
        val storage = Storage(); val probe = object : Probe() {
            override suspend fun zoom(lens: String, mode: CameraQualityCalibrationMode, ratios: List<Float>): List<CameraZoomCalibrationRecord> {
                val result = super.zoom(lens, mode, ratios)
                currentCoroutineContext().cancel()
                return result
            }
        }
        val job = launch { CameraCalibrationRunner(environment, storage, probe).run() }
        job.join(); assertTrue(job.isCancelled)
        assertTrue(storage.profile.zoomRecords.isEmpty())
        assertFalse(storage.profile.mandatoryReadiness(environment).ready)
    }
}
