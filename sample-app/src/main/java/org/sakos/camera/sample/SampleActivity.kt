package org.sakos.camera.sample

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.view.View
import android.view.Surface
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*
import org.sakos.camera.capture.camerax.*
import org.sakos.camera.capture.video.*
import org.sakos.camera.safety.core.*
import org.sakos.camera.safety.opennsfw2.*

/** Private, silent capture sample using managed review and approved-output boundaries. */
class SampleActivity : ComponentActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val dispatcher = worker.asCoroutineDispatcher()
    private lateinit var preview: PreviewView
    private lateinit var status: TextView
    private lateinit var photo: Button
    private lateinit var video: Button
    private lateinit var cancel: Button
    private lateinit var pause: Button
    private var paused = false
    private var stopping = false
    private lateinit var permission: Button
    private lateinit var retry: Button
    private lateinit var mediaList: LinearLayout
    private lateinit var lensButton: Button
    private lateinit var qualityButton: Button
    private lateinit var flashButton: Button
    private lateinit var zoomButton: Button
    private lateinit var calibrationRetry: Button
    private var graph: CameraGraphBinding? = null
    private var calibration: CameraQualityCalibrationProfile? = null
    private var calibrationEnvironment: CameraQualityCalibrationEnvironment? = null
    private var calibrationJob: Job? = null
    private var calibrating = false
    private var destroyed = false
    private var selectedTier = CameraCalibratedQualityTier.Default
    private var flashMode = CameraStillFlashMode.Off
    private var selectedZoom = 1f
    private lateinit var mediaClient: AndroidReviewedMediaClient
    private var outputRotation = Surface.ROTATION_0
    private lateinit var orientation: android.view.OrientationEventListener
    private var provider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var runtime: OpenNsfw2BitmapRuntime? = null
    private lateinit var store: AndroidVideoPrivateStagingStore
    private lateinit var sessions: VideoStagingSessionManager
    private lateinit var pipeline: ManagedVideoCapturePipeline
    private lateinit var approved: ApprovedMediaStore
    private var recording: CameraXVideoRecordingHandle? = null
    private var session: VideoStagingSession? = null
    private var work: Job? = null
    private var active = AtomicBoolean(false)
    private var foreground = false
    private var busy = false
    private var cleanupBlocked = false
    private var frontFacing = false
    private var viewer: AlertDialog? = null
    private val askCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) bindCamera() else showStatus("Camera permission denied. Grant permission to capture.")
        refreshControls()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        frontFacing = savedInstanceState?.getBoolean("frontFacing") ?: false
        selectedTier = savedInstanceState?.getString("quality")?.let { runCatching { CameraCalibratedQualityTier.valueOf(it) }.getOrNull() } ?: CameraCalibratedQualityTier.Default
        flashMode = CameraStillFlashPreferenceStore.read(this)
        orientation = object : android.view.OrientationEventListener(this) {
            override fun onOrientationChanged(degrees: Int) {
                outputRotation = CameraOutputRotation.fromOrientationDegrees(degrees) ?: return
                graph?.updateOutputRotation(outputRotation)
            }
        }
        buildUi()
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val protection = cameraVideoBackProtection(session != null && busy, recording != null, session != null && busy,
                    stopping, session != null && busy && recording == null)
                if (protection != CameraVideoBackProtection.None) showStatus(protection.reminder)
                else { isEnabled = false; onBackPressedDispatcher.onBackPressed(); isEnabled = true }
            }
        })
        store = AndroidVideoPrivateStagingStore(this)
        sessions = VideoStagingSessionManager(store)
        pipeline = ManagedVideoCapturePipeline(sessions)
        lifecycleScope.launch {
            try {
                withContext(dispatcher) {
                    cleanupBlocked = sessions.recoverAbandonedSessions().cleanupFailedSessionIds.isNotEmpty()
                    approved = ApprovedMediaStore(this@SampleActivity)
                    mediaClient = AndroidReviewedMediaClient(this@SampleActivity, approved.library)
                    runtime = OpenNsfw2BitmapRuntime.open(this@SampleActivity)
                }
                refreshMedia(); bindCamera()
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                android.util.Log.e("SakosSample", "Startup failure category: ${error.javaClass.simpleName}", error)
                showStatus("Runtime or private storage unavailable. Restart or retry cleanup.")
            }
            refreshControls()
        }
    }

    private fun buildUi() {
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 20, 20, 20) }
        column.addView(TextView(this).apply { text = "SakOS Camera SDK — managed capture"; textSize = 22f })
        preview = PreviewView(this).apply { id = PREVIEW_ID; implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
        column.addView(preview, LinearLayout.LayoutParams(-1, 0, 1f))
        status = TextView(this).apply { id = STATUS_ID; text = "Opening runtime and recovering private staging…" }
        column.addView(status)
        permission = button("Grant camera permission", PERMISSION_ID) { askCamera.launch(Manifest.permission.CAMERA) }
        column.addView(permission)
        val controls = LinearLayout(this)
        photo = button("Capture photo", PHOTO_ID) { capturePhoto() }
        video = button("Record video", VIDEO_ID) { if (recording != null) stopVideo() else startVideo() }
        cancel = button("Cancel", CANCEL_ID) { cancelCapture() }
        controls.addView(photo); controls.addView(video); controls.addView(cancel)
        column.addView(controls)
        pause = button("Pause video", PAUSE_ID) {
            if (paused) recording?.resume() else recording?.pause()
            paused = !paused; refreshControls()
        }
        column.addView(pause)
        retry = button("Retry cleanup", RETRY_ID) { retryCleanup() }
        column.addView(retry)
        val tools = LinearLayout(this)
        lensButton = button("Camera: back", LENS_ID) { frontFacing = !frontFacing; selectedZoom = 1f; bindCamera() }
        qualityButton = button("Quality: normal", QUALITY_ID) {
            val tiers = calibration?.childFacingVisibleTiersFor(CameraQualityCalibrationMode.Photo, if (frontFacing) "front" else "back").orEmpty()
            selectedTier = tiers.getOrNull((tiers.indexOf(selectedTier) + 1) % tiers.size.coerceAtLeast(1)) ?: CameraCalibratedQualityTier.Default
            bindCamera()
        }
        flashButton = button("Flash: off", FLASH_ID) { flashMode = flashMode.next(); CameraStillFlashPreferenceStore.write(this, flashMode); bindCamera() }
        zoomButton = button("Zoom: 1x", ZOOM_ID) {
            val stops = calibration?.childFacingVisibleZoomStopsFor(CameraQualityCalibrationMode.Photo, if (frontFacing) "front" else "back",
                graph?.camera?.cameraInfo?.zoomState?.value?.minZoomRatio, graph?.camera?.cameraInfo?.zoomState?.value?.maxZoomRatio).orEmpty()
            selectedZoom = stops.getOrNull((stops.indexOf(selectedZoom) + 1) % stops.size.coerceAtLeast(1)) ?: 1f
            graph?.camera?.cameraControl?.setZoomRatio(selectedZoom); refreshControls()
        }
        tools.addView(lensButton); tools.addView(qualityButton); column.addView(tools)
        val extraTools = LinearLayout(this); extraTools.addView(flashButton); extraTools.addView(zoomButton); column.addView(extraTools)
        calibrationRetry = button("Retry camera calibration", CALIBRATION_RETRY_ID) { calibration = null; calibrationJob = null; bindCamera(forceCalibration = true) }
        column.addView(calibrationRetry)
        preview.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP && !busy && !calibrating) {
                graph?.camera?.cameraControl?.startFocusAndMetering(FocusMeteringAction.Builder(preview.meteringPointFactory.createPoint(event.x, event.y),
                    FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE).setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS).build())
            }
            true
        }
        column.addView(TextView(this).apply { text = "Approved media (private to this sample)"; textSize = 18f })
        mediaList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; id = MEDIA_ID }
        column.addView(ScrollView(this).apply { addView(mediaList) }, LinearLayout.LayoutParams(-1, 220))
        setContentView(column); refreshControls()
    }
    private fun button(label: String, viewId: Int, action: () -> Unit) = Button(this).apply {
        text = label; id = viewId; setOnClickListener { action() }
    }
    private fun hasPermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun bindCamera(showReady: Boolean = true, forceCalibration: Boolean = false) {
        if (!foreground || runtime == null || !hasPermission() || busy || calibrating || destroyed) return
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            if (!foreground || !hasPermission() || busy || calibrating || destroyed) return@addListener
            try {
                val camera = future.get(); provider = camera
                val inventory = CameraCapabilityDiscovery.discover(camera)
                val environment = CameraQualityCalibrationEnvironment.current(this, requireNotNull(runtime).configuration.identity(), CameraCapabilityDiscovery.inventoryHash(inventory))
                calibrationEnvironment = environment
                val storage = AndroidCameraCalibrationProfileStorage(this, environment)
                val cached = storage.load()
                if (forceCalibration || !cached.mandatoryReadiness(environment).ready) {
                    if (calibrationJob == null) {
                        calibrating = true; imageCapture = null; videoCapture = null; refreshControls()
                        val probe = AndroidCameraCalibrationProbe(this, this, camera, preview, worker, requireNotNull(runtime)) { outputRotation }
                        val runner = CameraCalibrationRunner(environment, storage, probe)
                        calibrationJob = lifecycleScope.launch {
                            try {
                                calibration = runner.run(retry = forceCalibration) { done, total -> showStatus("Calibrating camera $done/$total. Probe output is discarded.") }
                                val readiness = requireNotNull(calibration).mandatoryReadiness(environment)
                                showStatus(if (readiness.ready) "Camera calibration complete." else "Camera calibration unavailable: ${readiness.reason}")
                            } catch (cancelled: CancellationException) { calibrationJob = null; throw cancelled }
                            catch (_: Exception) { showStatus("Camera calibration failed. Retry is available.") }
                            finally { calibrating = false; refreshControls() }
                            if (calibration?.mandatoryReadiness(environment)?.ready == true && foreground) bindCamera(showReady = false)
                        }
                    }
                    return@addListener
                }
                calibration = cached
                if (!camera.hasCamera(if (frontFacing) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA)) frontFacing = !frontFacing
                val lens = if (frontFacing) "front" else "back"
                val preset = requireNotNull(CameraGraphTooling.selectedPreset(cached, environment, CameraQualityCalibrationMode.Photo, lens, selectedTier))
                graph?.closeAnalysis()
                graph = CameraGraphTooling.bind(camera, this, preview, lens, preset,
                    outputRotation, worker, if (inventory.any { it.lens == lens && it.flash }) flashMode.imageCaptureFlashMode else ImageCapture.FLASH_MODE_OFF)
                imageCapture = graph?.photo
                videoCapture = VideoCapture.withOutput(Recorder.Builder().build()) // Actual calibrated video graph is bound on record.
                selectedZoom = if (frontFacing) 1f else selectedZoom
                graph?.camera?.cameraControl?.setZoomRatio(selectedZoom)
                if (showReady) showStatus("Ready. Calibrated ${lens} camera; photos review in memory and videos use private staging.")
            } catch (_: Exception) {
                imageCapture = null; videoCapture = null; showStatus("Camera unavailable. Retry calibration or return to the app.")
            }
            refreshControls()
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun capturePhoto() {
        val capture = imageCapture ?: return
        if (busy || !foreground || runtime == null || !hasPermission()) return
        beginCapture("Capturing photo…")
        val token = active
        capture.takePicture(worker, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                if (!token.get()) { image.close(); runOnUiThread { finishCapture("Capture cancelled.") }; return }
                // ATOMIC enters the ownership/finally path even if lifecycle cancellation races launch.
                work = lifecycleScope.launch(dispatcher, start = CoroutineStart.ATOMIC) {
                    var bitmap: Bitmap? = null
                    val imageClosed = AtomicBoolean(false)
                    val ownedImage = object : ImageProxy by image {
                        override fun close() { if (imageClosed.compareAndSet(false, true)) image.close() }
                    }
                    try {
                        val rotated = image.imageInfo.rotationDegrees % 180 != 0
                        val rawWidth = if (rotated) image.height else image.width
                        val rawHeight = if (rotated) image.width else image.height
                        val scale = if (selectedTier == CameraCalibratedQualityTier.Low && !frontFacing && rawWidth.toLong() * rawHeight > 3_000_000L)
                            kotlin.math.sqrt(3_000_000.0 / (rawWidth.toLong() * rawHeight)) else 1.0
                        val request = SafetyCaptureContext(SafetyCaptureId(UUID.randomUUID().toString()), System.currentTimeMillis(),
                            kotlin.math.round(rawWidth * scale).toInt(), kotlin.math.round(rawHeight * scale).toInt(), 0, frontFacing)
                        val sink = object : ApprovedPhotoSink<Bitmap> {
                            override suspend fun save(input: Bitmap, capture: SafetyCaptureContext, approval: ManagedCaptureApproval) {
                                currentCoroutineContext().ensureActive(); check(token.get())
                                approved.savePhoto(input, capture, approval) { token.get() }
                            }
                        }
                        val result = ManagedPhotoReviewPipeline(OpenNsfw2BitmapEvaluator(requireNotNull(runtime)), sink,
                            OpenNsfw2ModelPreflight.configuration).reviewImageProxy(ownedImage, request) {
                                val raw = it.toBitmap()
                                bitmap = raw
                                val rotation = it.imageInfo.rotationDegrees
                                bitmap = if (rotation == 0) raw else Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height,
                                    Matrix().apply { postRotate(rotation.toFloat()) }, true).also { rotated -> if (rotated !== raw) raw.recycle() }
                                if (selectedTier == CameraCalibratedQualityTier.Low && !frontFacing) bitmap = CameraGraphTooling.lowPhoto(requireNotNull(bitmap))
                                requireNotNull(bitmap)
                            }
                        withContext(Dispatchers.Main) { finishCapture(if (result is ManagedPhotoResult.Delivered) "Photo approved." else "Photo not approved; no image saved.") }
                    } catch (_: CancellationException) { runOnUiThread { finishCapture("Capture cancelled.") } }
                    catch (_: Exception) { runOnUiThread { finishCapture("Photo failed; no output delivered.") } }
                    finally { ownedImage.close(); bitmap?.let { if (!it.isRecycled) it.recycle() } }
                }
            }
            override fun onError(exception: ImageCaptureException) { runOnUiThread { finishCapture("Camera capture failed. Try again.") } }
        })
    }

    private fun startVideo() {
        val clip = videoCapture ?: return
        if (busy || cleanupBlocked || !foreground || !hasPermission() || runtime == null) return
        beginCapture("Starting silent video…")
        try {
            val camera = requireNotNull(provider)
            graph?.closeAnalysis()
            val videoPreset = requireNotNull(CameraGraphTooling.selectedPreset(requireNotNull(calibration), requireNotNull(calibrationEnvironment),
                CameraQualityCalibrationMode.Video, if (frontFacing) "front" else "back", CameraCalibratedQualityTier.Default))
            graph = CameraGraphTooling.bind(camera, this, preview, if (frontFacing) "front" else "back", videoPreset, outputRotation, worker)
            val boundClip = requireNotNull(graph?.video)
            graph?.camera?.cameraControl?.setZoomRatio(if (frontFacing) 1f else selectedZoom)
            val start = sessions.startRecording(System.currentTimeMillis())
            if (start !is VideoRecordingStartResult.Started) { cleanupBlocked = true; finishCapture("Private cleanup is required."); return }
            val current = start.session; session = current
            val token = active
            val factory = CameraXPrivateVideoRecordingFactory(store)
            val pending = boundClip.output.prepareRecording(this, factory.outputOptions(current))
            recording = factory.start(pending, ContextCompat.getMainExecutor(this)) { event ->
                if (event is VideoRecordEvent.Status && event.recordingStats.recordedDurationNanos / 1_000_000L >= VideoGatePolicy.maxNormalDurationMillis) stopVideo()
                if (event is VideoRecordEvent.Finalize) {
                    recording?.close(); recording = null
                    if (!token.get()) {
                        sessions.discard(current.id); finishCapture("Video cancelled; private cleanup requested.")
                    } else {
                        when (val final = CameraXVideoFinalizationBridge(sessions).finalize(current, event)) {
                            is CameraXVideoFinalizationResult.ReadyForReview -> reviewVideo(final.session, token)
                            else -> { sessions.discard(current.id); finishCapture("Recording failed; private cleanup requested.") }
                        }
                    }
                }
            }
            showStatus("Recording silent video. Stop to review or cancel to discard."); refreshControls()
        } catch (_: Exception) {
            session?.let { sessions.discard(it.id) }; finishCapture("Video could not start. Private cleanup requested.")
        }
    }
    private fun stopVideo() { stopping = true; recording?.stop(); showStatus("Finalizing private video…"); video.isEnabled = false }
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun reviewVideo(current: VideoStagingSession, token: AtomicBoolean) {
        showStatus("Reviewing sampled video frames…")
        work = lifecycleScope.launch(dispatcher, start = CoroutineStart.ATOMIC) {
            try {
                val result = AndroidVideoReviewBridge(store, pipeline).reviewInto(current, requireNotNull(runtime), approved.library, frontFacing) { token.get() }
                withContext(Dispatchers.Main) {
                    finishCapture(when (result) {
                        is ManagedVideoReviewResult.Promoted -> "Video approved."
                        is ManagedVideoReviewResult.PromotedCleanupPending -> "Video approved; private cleanup retry required."
                        else -> "Video not delivered; private cleanup requested."
                    })
                }
            } catch (_: CancellationException) { runOnUiThread { finishCapture("Video review cancelled; private cleanup requested.") } }
            catch (_: Exception) { sessions.discard(current.id); runOnUiThread { finishCapture("Video review failed; private cleanup requested.") } }
        }
    }
    private fun beginCapture(message: String) { active = AtomicBoolean(true); busy = true; showStatus(message); refreshControls() }
    private fun finishCapture(message: String) {
        if (destroyed) return
        busy = false; paused = false; stopping = false; session = null
        cleanupBlocked = runCatching { store.sessions().any { it.state != VideoStagingState.Completed } }.getOrDefault(true)
        showStatus(message); refreshControls(); refreshMedia()
        if (foreground && runtime != null && hasPermission()) bindCamera(showReady = false)
    }
    private fun cancelCapture() {
        active.set(false); work?.cancel()
        recording?.let { it.stop(); it.close() }
    }
    private fun retryCleanup() {
        if (busy) return
        lifecycleScope.launch {
            cleanupBlocked = withContext(dispatcher) { runCatching { sessions.recoverAbandonedSessions().cleanupFailedSessionIds.isNotEmpty() }.getOrDefault(true) }
            showStatus(if (cleanupBlocked) "Cleanup still needs attention." else "Private staging cleanup complete.")
            refreshControls(); bindCamera()
        }
    }
    private fun refreshControls() {
        val permitted = hasPermission()
        permission.visibility = if (permitted) View.GONE else View.VISIBLE
        photo.isEnabled = !calibrating && foreground && permitted && runtime != null && imageCapture != null && !busy && !cleanupBlocked
        video.isEnabled = (recording != null && !stopping) || (!calibrating && foreground && permitted && runtime != null && videoCapture != null && !busy && !cleanupBlocked)
        pause.isEnabled = recording != null && !stopping; pause.visibility = if (recording != null) View.VISIBLE else View.GONE
        pause.text = if (paused) "Resume video" else "Pause video"
        video.text = if (recording != null) "Stop video" else "Record video"
        lensButton.isEnabled = photo.isEnabled && provider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) == true && provider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) == true
        lensButton.text = "Camera: ${if (frontFacing) "front" else "back"}"
        qualityButton.isEnabled = photo.isEnabled; qualityButton.text = "Quality: ${(calibration?.calibratedTierFor(CameraQualityCalibrationMode.Photo, if (frontFacing) "front" else "back", selectedTier) ?: CameraCalibratedQualityTier.Default).labelFor(CameraQualityCalibrationMode.Photo)}"
        flashButton.isEnabled = photo.isEnabled && graph?.camera?.cameraInfo?.hasFlashUnit() == true; flashButton.text = "Flash: ${if (graph?.camera?.cameraInfo?.hasFlashUnit() == true) flashMode.displayLabel else CameraStillFlashMode.Off.displayLabel}"
        zoomButton.isEnabled = photo.isEnabled && !frontFacing; zoomButton.text = "Zoom: ${selectedZoom}x"
        calibrationRetry.isEnabled = foreground && hasPermission() && runtime != null && !busy && !calibrating
        calibrationRetry.visibility = if (imageCapture == null && !calibrating) View.VISIBLE else View.GONE
        cancel.isEnabled = busy; retry.visibility = if (cleanupBlocked) View.VISIBLE else View.GONE
    }
    private fun showStatus(message: String) { status.text = message }
    private fun refreshMedia() {
        if (!::approved.isInitialized) return
        mediaList.removeAllViews()
        approved.items().forEachIndexed { index, file ->
            mediaList.addView(button("Open ${if (file.kind == ReviewedMediaKind.Photo) "photo" else "video"} ${index + 1}", View.generateViewId()) { openApproved(file) })
        }
        if (mediaList.childCount == 0) mediaList.addView(TextView(this).apply { text = "No approved media yet." })
    }
    private fun openApproved(file: ReviewedMediaEntry) {
        if (file !in approved.items()) return
        if (file.kind == ReviewedMediaKind.Photo) {
            val bitmap = mediaClient.decodePreview(file) ?: return
            val image = ImageView(this).apply { setImageBitmap(bitmap); adjustViewBounds = true }
            viewer = AlertDialog.Builder(this).setTitle("Approved photo").setView(image).setPositiveButton("Close", null).create().apply {
                setOnDismissListener { image.setImageDrawable(null); bitmap.recycle(); viewer = null }; show()
            }
        } else {
            val lease = mediaClient.playback(file)
            val clip = VideoView(this).apply { setVideoURI(lease.uri); setOnPreparedListener { start() } }
            viewer = AlertDialog.Builder(this).setTitle("Approved video").setView(clip).setPositiveButton("Close", null).create().apply {
                setOnDismissListener { clip.stopPlayback(); lease.close(); viewer = null }; show()
            }
        }
    }
    override fun onStart() { super.onStart(); foreground = true; orientation.enable(); if (::status.isInitialized) { bindCamera(); refreshControls() } }
    override fun onStop() { foreground = false; orientation.disable(); calibrationJob?.cancel(); calibrationJob = null; viewer?.dismiss(); cancelCapture(); graph?.closeAnalysis(); provider?.unbindAll(); refreshControls(); super.onStop() }
    override fun onDestroy() {
        destroyed = true
        worker.execute { runtime?.close() }; dispatcher.close(); super.onDestroy()
    }
    override fun onSaveInstanceState(outState: Bundle) { outState.putBoolean("frontFacing", frontFacing); outState.putString("quality", selectedTier.name); super.onSaveInstanceState(outState) }
    companion object {
        const val PREVIEW_ID = 1001; const val STATUS_ID = 1002; const val PERMISSION_ID = 1003
        const val PHOTO_ID = 1004; const val VIDEO_ID = 1005; const val CANCEL_ID = 1006
        const val RETRY_ID = 1007; const val MEDIA_ID = 1008
        const val LENS_ID = 1009; const val QUALITY_ID = 1010; const val FLASH_ID = 1011; const val ZOOM_ID = 1012; const val CALIBRATION_RETRY_ID = 1013; const val PAUSE_ID = 1014
    }
}
