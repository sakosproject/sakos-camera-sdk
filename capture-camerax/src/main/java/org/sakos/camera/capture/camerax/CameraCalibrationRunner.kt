package org.sakos.camera.capture.camerax

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout

data class CameraCalibrationStep(val lens: String, val preset: CameraQualityGraphPreset, val purpose: CameraQualityCalibrationProbePurpose)

interface CameraCalibrationProfileStorage {
    fun load(): CameraQualityCalibrationProfile
    fun reset()
    fun record(record: CameraQualityCalibrationRecord): CameraQualityCalibrationProfile
    fun recordZoom(record: CameraZoomCalibrationRecord): CameraQualityCalibrationProfile
}

interface CameraCalibrationProbe {
    /** Must own, gate and destroy probe output. Block is a valid mechanical gate result; runtime failure is not. */
    suspend fun quality(step: CameraCalibrationStep, fullTemporal: Boolean): CameraQualityCalibrationRecord
    suspend fun zoom(lens: String, mode: CameraQualityCalibrationMode, ratios: List<Float>): List<CameraZoomCalibrationRecord>
    fun cancel()
}

/** Source first-run sequence. No capture is ready until mandatoryReadiness succeeds. Optional diagnostics cannot become selectors. */
class CameraCalibrationRunner(
    private val environment: CameraQualityCalibrationEnvironment,
    private val storage: CameraCalibrationProfileStorage,
    private val probe: CameraCalibrationProbe,
) {
    private val mutex = Mutex()
    suspend fun run(retry: Boolean = false, optionalDiagnostics: Boolean = false,
        progress: (Int, Int) -> Unit = { _, _ -> }): CameraQualityCalibrationProfile {
        check(mutex.tryLock()) { "Calibration already running." }
        try {
            val existing = storage.load()
            if (!retry && existing.mandatoryReadiness(environment).ready) return existing
            storage.reset() // A mismatched/incomplete profile cannot be relabeled under a new identity.
            val steps = requiredSteps + if (optionalDiagnostics) diagnosticSteps else emptyList()
            var completed = 0
            for (step in steps) {
                currentCoroutineContext().ensureActive()
                var record = quality(step, false)
                validateRecord(step, record, false)
                require(record.fastProbeRetried != true)
                var retried = false
                if (cameraQualityCalibrationNeedsFullTemporalRetry(record)) {
                    record = quality(step, true).copy(fastProbeRetried = true)
                    retried = true
                }
                validateRecord(step, record, retried)
                currentCoroutineContext().ensureActive()
                storage.record(record)
                progress(++completed, steps.size + 2)
            }
            for (mode in CameraQualityCalibrationMode.entries) {
                val records = try { withTimeout(55_000) { probe.zoom("back", mode, cameraZoomCalibrationProductStops()) } }
                catch (timeout: kotlinx.coroutines.TimeoutCancellationException) {
                    probe.cancel()
                    cameraZoomCalibrationProductStops().map { ratio -> CameraZoomCalibrationRecord(mode, "back", ratio,
                        CameraZoomCalibrationStatus.Failed, null, null, null, null, "Zoom probe timed out", System.currentTimeMillis()) }
                }
                require(records.size == cameraZoomCalibrationProductStops().size)
                require(records.map { it.requestedZoomRatio }.sorted() == cameraZoomCalibrationProductStops().sorted())
                records.forEach {
                    currentCoroutineContext().ensureActive()
                    require(it.mode == mode && cameraQualityCalibrationLensToken(it.lensLabel) == "back")
                    storage.recordZoom(it)
                }
                progress(++completed, steps.size + 2)
            }
            return storage.load()
        } finally {
            try { probe.cancel() }
            catch (cleanup: Exception) {
                // Never retain a ready profile after unproven terminal cleanup.
                try {
                    storage.reset()
                    storage.record(failureRecord(requiredSteps.first(), false, "Probe cleanup failed"))
                } catch (storageFailure: Exception) { cleanup.addSuppressed(storageFailure) }
                throw cleanup
            } finally { mutex.unlock() }
        }
    }

    private fun validateRecord(step: CameraCalibrationStep, record: CameraQualityCalibrationRecord, fullTemporal: Boolean) {
        require(record.graphPreset == step.preset && cameraQualityCalibrationLensToken(record.lensLabel) == step.lens &&
            record.mode == step.preset.mode && record.tier == step.preset.tier && record.probePurpose == step.purpose &&
            record.mandatoryForReadiness == (step.purpose != CameraQualityCalibrationProbePurpose.OptionalDiagnostics))
        require(record.gateCoverage == if (step.preset.mode == CameraQualityCalibrationMode.Photo) CameraQualityCalibrationGateCoverage.StillGate
            else if (fullTemporal) CameraQualityCalibrationGateCoverage.FullTemporalFallback else CameraQualityCalibrationGateCoverage.FastSingleFrame)
    }

    private suspend fun quality(step: CameraCalibrationStep, fullTemporal: Boolean): CameraQualityCalibrationRecord =
        try { withTimeout(55_000) { probe.quality(step, fullTemporal) } }
        catch (timeout: kotlinx.coroutines.TimeoutCancellationException) { failedProbe(step, fullTemporal, "Probe timed out") }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { failedProbe(step, fullTemporal, "Probe failed") }

    private fun failedProbe(step: CameraCalibrationStep, fullTemporal: Boolean, reason: String): CameraQualityCalibrationRecord {
        probe.cancel()
        return failureRecord(step, fullTemporal, reason)
    }
    private fun failureRecord(step: CameraCalibrationStep, fullTemporal: Boolean, reason: String): CameraQualityCalibrationRecord =
        CameraQualityCalibrationRecord(step.preset.mode, step.lens, step.preset.tier, CameraQualityCalibrationStatus.Failed,
            step.preset.requestedCameraXQuality, null, null, null, null, null, null, null, reason, System.currentTimeMillis(),
            graphPreset = step.preset, previewStable = false, gateStatusLabel = "runtime failure",
            calibrationArtifactCleanupSucceeded = false, probePurpose = step.purpose,
            mandatoryForReadiness = step.purpose != CameraQualityCalibrationProbePurpose.OptionalDiagnostics,
            gateCoverage = if (step.preset.mode == CameraQualityCalibrationMode.Photo) CameraQualityCalibrationGateCoverage.StillGate
                else if (fullTemporal) CameraQualityCalibrationGateCoverage.FullTemporalFallback else CameraQualityCalibrationGateCoverage.FastSingleFrame)

    companion object {
        val requiredSteps = listOf(
            CameraCalibrationStep("back", CameraQualityGraphPreset.PhotoStable, CameraQualityCalibrationProbePurpose.RequiredDefault),
            CameraCalibrationStep("back", CameraQualityGraphPreset.PhotoHighStillProbeV2, CameraQualityCalibrationProbePurpose.RequiredSelectorCandidate),
            CameraCalibrationStep("front", CameraQualityGraphPreset.PhotoStable, CameraQualityCalibrationProbePurpose.RequiredDefault),
            CameraCalibrationStep("back", CameraQualityGraphPreset.VideoStable, CameraQualityCalibrationProbePurpose.RequiredDefault),
            CameraCalibrationStep("front", CameraQualityGraphPreset.VideoStable, CameraQualityCalibrationProbePurpose.RequiredDefault),
        )
        val diagnosticSteps = listOf("back", "front").flatMap { lens ->
            listOf(CameraQualityGraphPreset.VideoLow, CameraQualityGraphPreset.VideoHighFhdProbe, CameraQualityGraphPreset.VideoUhdProbe)
                .map { CameraCalibrationStep(lens, it, CameraQualityCalibrationProbePurpose.OptionalDiagnostics) }
        }
    }
}

class AndroidCameraCalibrationProfileStorage(context: android.content.Context, environment: CameraQualityCalibrationEnvironment) : CameraCalibrationProfileStorage {
    private val context = context.applicationContext
    private val store = CameraQualityCalibrationStore(environment)
    override fun load() = store.load(context)
    override fun reset() = store.clear(context)
    override fun record(record: CameraQualityCalibrationRecord) = store.recordObservation(context, record)
    override fun recordZoom(record: CameraZoomCalibrationRecord) = store.recordZoomObservation(context, record)
}
