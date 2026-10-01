package org.sakos.camera.capture.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.*
import org.sakos.camera.capture.camerax.*
import org.sakos.camera.safety.opennsfw2.*

/** Actual graph/capture/gate probes. Caller supplies lifecycle and runtime. No approval or gallery promotion. */
class AndroidCameraCalibrationProbe(
    private val context: Context,
    private val owner: LifecycleOwner,
    private val provider: ProcessCameraProvider,
    private val preview: PreviewView,
    private val worker: Executor,
    private val runtime: OpenNsfw2BitmapRuntime,
    private val outputRotation: () -> Int,
) : CameraCalibrationProbe {
    private val main = ContextCompat.getMainExecutor(context)
    private val store = AndroidVideoPrivateStagingStore(context)
    private val sessions = VideoStagingSessionManager(store)
    private var recording: Recording? = null
    private var staged: VideoStagingSession? = null
    private var binding: CameraGraphBinding? = null
    private var terminal: CompletableDeferred<VideoRecordEvent.Finalize>? = null

    private suspend fun bind(lens: String, preset: CameraQualityGraphPreset): CameraGraphBinding = withContext(Dispatchers.Main.immediate) {
        currentCoroutineContext().ensureActive()
        binding?.closeAnalysis()
        val graph = CameraGraphTooling.bind(provider, owner, preview, lens, preset, outputRotation(), worker)
        binding = graph
        while (preview.previewStreamState.value != PreviewView.StreamState.STREAMING || graph.preview.resolutionInfo == null ||
            (graph.photo?.resolutionInfo == null && graph.video?.resolutionInfo == null)) delay(100)
        delay(250) // Source fast bound-facts settle; timeout is enforced by runner.
        graph
    }

    override suspend fun quality(step: CameraCalibrationStep, fullTemporal: Boolean): CameraQualityCalibrationRecord {
        val started = SystemClock.elapsedRealtime()
        var width: Int? = null; var height: Int? = null; var rotation: Int? = null
        var duration: Long? = null; var size: Long? = null; var profile: GateSamplingProfile? = null
        var sampleCount = 0; var evaluatedCount = 0; var gateStatus = "runtime failure"
        var graph: CameraGraphBinding? = null
        var error: String? = null; var cleanup = true
        val video = step.preset.mode == CameraQualityCalibrationMode.Video
        try {
            currentCoroutineContext().ensureActive()
            graph = bind(step.lens, step.preset)
            if (!video) {
                val bitmap = capture(requireNotNull(graph.photo))
                try {
                    width = bitmap.width; height = bitmap.height; rotation = 0
                    val result = withContext(Dispatchers.Default) { runtime.evaluate(bitmap) }
                    profile = GateSamplingProfile.detect(bitmap.width, bitmap.height)
                    gateStatus = if (result.checkResult.isSafe) "Allow" else "Block"
                    sampleCount = 1; evaluatedCount = 1
                } finally { bitmap.recycle() }
            } else {
                val start = sessions.startRecording(System.currentTimeMillis()) as? VideoRecordingStartResult.Started
                    ?: error("Unresolved private staging cleanup")
                staged = start.session
                val file = store.recordingOutputFile(start.session.id)
                val finalized = CompletableDeferred<VideoRecordEvent.Finalize>()
                terminal = finalized
                recording = requireNotNull(graph.video).output.prepareRecording(context, FileOutputOptions.Builder(file).build())
                    .start(main) { event -> if (event is VideoRecordEvent.Finalize) finalized.complete(event) }
                delay(if (!fullTemporal && step.preset != CameraQualityGraphPreset.VideoUhdProbe) 1_200L
                    else if (step.preset == CameraQualityGraphPreset.VideoStable) 3_000L
                    else if (step.preset == CameraQualityGraphPreset.VideoUhdProbe) 1_000L else 1_500L)
                recording?.stop()
                val event = finalized.await(); recording?.close(); recording = null
                check(!event.hasError()) { "Recording finalization failed" }
                val facts = requireNotNull(readCameraDiagnosticVideoFileFacts(file))
                width = facts.width; height = facts.height; rotation = facts.rotationDegrees
                duration = facts.metadataDurationMillis; size = file.length()
                profile = GateSamplingProfile.detect(requireNotNull(width), requireNotNull(height), rotation)
                withContext(Dispatchers.Default) {
                    if (!fullTemporal) {
                        AndroidVideoFrameDecoder(file).use { decoder ->
                            // Source single-frame mechanics probe, not a temporal capture approval.
                            val sample = VideoTemporalSamplePlanner.buildCalibrationSmokePlan(requireNotNull(duration)).baseSamples.single()
                            decoder.decode(sample).use { frame ->
                                val result = runtime.evaluateVideoBaseSweep(frame.value)
                                gateStatus = if (result.checkResult.isSafe) "Allow" else "Block"
                                sampleCount = 1; evaluatedCount = 1
                            }
                        }
                    } else {
                        val decoder = AndroidVideoFrameDecoder(file)
                        val result = VideoTemporalReviewEngine().review(decoder.durationMillis, decoder, OpenNsfw2VideoFrameEvaluator(runtime))
                        sampleCount = result.samples.size
                        evaluatedCount = result.samples.count { it.evaluation is VideoFrameEvaluation.Decision }
                        check(result.decision != VideoTemporalReviewDecision.Failure) { "runtime failure" }
                        gateStatus = result.decision.name
                    }
                }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { error = failure.javaClass.simpleName }
        finally {
            withContext(NonCancellable) {
                val stopped = runCatching { recording?.stop() }.isSuccess
                val closed = runCatching { recording?.close() }.isSuccess
                recording = null
                if (!stopped || !closed) error = error ?: "Recording terminal request failed"
                val finished = terminal?.let { withTimeoutOrNull(10_000) { it.await(); true } } ?: true
                // Leave durable staging unresolved if CameraX has not acknowledged terminal state.
                staged?.let { cleanup = finished == true && sessions.discard(it.id) is VideoStagingTransitionResult.Removed }
                terminal = null; staged = null
            }
        }
        val status = if (error == null && cleanup && width != null && height != null && evaluatedCount > 0)
            CameraQualityCalibrationStatus.Verified else CameraQualityCalibrationStatus.Failed
        val coverage = if (!video) CameraQualityCalibrationGateCoverage.StillGate
            else if (fullTemporal) CameraQualityCalibrationGateCoverage.FullTemporalFallback else CameraQualityCalibrationGateCoverage.FastSingleFrame
        return CameraQualityCalibrationRecord(step.preset.mode, step.lens, step.preset.tier, status, step.preset.requestedCameraXQuality,
            graph?.camera?.cameraInfo?.let { Recorder.getVideoCapabilities(it).getSupportedQualities(DynamicRange.SDR).joinToString { q -> cameraDiagnosticVideoQualityLabel(q) } },
            graph?.let { cameraDiagnosticUseCaseLine("bound", it.photo?.resolutionInfo ?: it.video?.resolutionInfo) },
            width, height, rotation, duration, size, if (status == CameraQualityCalibrationStatus.Failed) error ?: "Probe or cleanup failed" else null,
            System.currentTimeMillis(), graphPreset = step.preset,
            previewStreamLabel = graph?.let { cameraDiagnosticUseCaseLine("preview", it.preview.resolutionInfo) },
            previewStable = graph != null && preview.previewStreamState.value == PreviewView.StreamState.STREAMING,
            gateStatusLabel = gateStatus, gateDurationMillis = SystemClock.elapsedRealtime() - started,
            gateSamplingProfileId = profile?.id, gateSamplingProfileLabel = profile?.displayLabel,
            gateSamplingRatio = profile?.normalizedRatio, gateSamplingOrientation = profile?.orientation?.displayLabel,
            gateSamplingProfileSelectorEligible = profile?.selectorEligible,
            gateSampledFrameCount = sampleCount, gateEvaluatedFrameCount = evaluatedCount,
            calibrationGateMode = coverage.displayLabel, gateCoverage = coverage,
            calibrationGateRequiredForSelector = true, calibrationArtifactCleanupSucceeded = cleanup,
            probePurpose = step.purpose, mandatoryForReadiness = step.purpose != CameraQualityCalibrationProbePurpose.OptionalDiagnostics)
    }

    private suspend fun capture(capture: ImageCapture): Bitmap = suspendCancellableCoroutine { continuation ->
        capture.takePicture(worker, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                var bitmap: Bitmap? = null
                try {
                    if (!continuation.isActive) return
                    val raw = image.toBitmap(); bitmap = raw
                    if (image.imageInfo.rotationDegrees != 0) bitmap = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height,
                        Matrix().apply { postRotate(image.imageInfo.rotationDegrees.toFloat()) }, true).also { if (it !== raw) raw.recycle() }
                    val result = requireNotNull(bitmap)
                    continuation.resume(result) { _, value, _ -> value.recycle() }
                    bitmap = null
                } catch (error: Exception) { if (continuation.isActive) continuation.resumeWithException(error) }
                finally { image.close(); bitmap?.recycle() }
            }
            override fun onError(error: ImageCaptureException) { if (continuation.isActive) continuation.resumeWithException(error) }
        })
    }

    override suspend fun zoom(lens: String, mode: CameraQualityCalibrationMode, ratios: List<Float>): List<CameraZoomCalibrationRecord> {
        val graph = bind(lens, CameraQualityGraphPreset.selectorPresetFor(mode, CameraCalibratedQualityTier.Default))
        try {
            return ratios.map { ratio ->
                val initial = requireNotNull(graph.camera.cameraInfo.zoomState.value)
                val inRange = cameraZoomRatioInRange(ratio, initial.minZoomRatio, initial.maxZoomRatio)
                var failure: String? = null
                if (inRange) {
                    try {
                        val future = graph.camera.cameraControl.setZoomRatio(ratio)
                        suspendCancellableCoroutine<Unit> { cont -> future.addListener({
                            try { future.get(); if (cont.isActive) cont.resume(Unit) }
                            catch (error: Exception) { if (cont.isActive) cont.resumeWithException(error) }
                        }, main) }
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (error: Exception) { failure = error.javaClass.simpleName }
                }
                val observed = graph.camera.cameraInfo.zoomState.value?.zoomRatio
                CameraZoomCalibrationRecord(mode, lens, ratio,
                    if (!inRange) CameraZoomCalibrationStatus.Unsupported
                    else if (failure == null && observed != null && cameraZoomRatiosEquivalent(ratio, observed)) CameraZoomCalibrationStatus.Verified
                    else CameraZoomCalibrationStatus.Failed,
                    initial.minZoomRatio, initial.maxZoomRatio, if (failure == null && inRange) ratio else observed, observed,
                    if (!inRange) "outside CameraX zoom range" else failure ?: if (observed == null || !cameraZoomRatiosEquivalent(ratio, observed)) "observed zoom did not settle" else null,
                    System.currentTimeMillis())
            }
        } finally { graph.camera.cameraControl.setZoomRatio(1f) }
    }
    override fun cancel() {
        runCatching { recording?.stop() }; runCatching { recording?.close() }; recording = null
        // A stop request does not acknowledge finalization. The suspending owner
        // waits for terminal state; unresolved output stays durable for recovery.
        if (terminal == null || terminal?.isCompleted == true) {
            staged?.let { sessions.discard(it.id) }; staged = null
        }
        binding?.unbind(provider); binding = null
    }
}
