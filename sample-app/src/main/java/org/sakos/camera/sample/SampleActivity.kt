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
    private lateinit var permission: Button
    private lateinit var retry: Button
    private lateinit var mediaList: LinearLayout
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
        buildUi()
        store = AndroidVideoPrivateStagingStore(this)
        sessions = VideoStagingSessionManager(store)
        pipeline = ManagedVideoCapturePipeline(sessions)
        lifecycleScope.launch {
            try {
                withContext(dispatcher) {
                    cleanupBlocked = sessions.recoverAbandonedSessions().cleanupFailedSessionIds.isNotEmpty()
                    approved = ApprovedMediaStore(this@SampleActivity)
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
        retry = button("Retry cleanup", RETRY_ID) { retryCleanup() }
        column.addView(retry)
        column.addView(TextView(this).apply { text = "Approved media (private to this sample)"; textSize = 18f })
        mediaList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; id = MEDIA_ID }
        column.addView(ScrollView(this).apply { addView(mediaList) }, LinearLayout.LayoutParams(-1, 220))
        setContentView(column); refreshControls()
    }
    private fun button(label: String, viewId: Int, action: () -> Unit) = Button(this).apply {
        text = label; id = viewId; setOnClickListener { action() }
    }
    private fun hasPermission() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun selector(camera: ProcessCameraProvider): CameraSelector {
        frontFacing = !camera.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
        return if (!frontFacing) CameraSelector.DEFAULT_BACK_CAMERA
        else if (camera.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) CameraSelector.DEFAULT_FRONT_CAMERA
        else error("Camera unavailable")
    }
    private fun livePreview() = Preview.Builder().setTargetRotation(preview.display?.rotation ?: Surface.ROTATION_0).build()
        .also { it.surfaceProvider = preview.surfaceProvider }
    private fun bindCamera(showReady: Boolean = true) {
        if (!foreground || runtime == null || !hasPermission() || busy) return
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            if (!foreground || !hasPermission() || busy) return@addListener
            try {
                val camera = future.get(); provider = camera
                camera.unbindAll()
                val capture = ImageCapture.Builder().setTargetRotation(preview.display?.rotation ?: Surface.ROTATION_0).build()
                val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.SD,
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.SD))).build()
                camera.bindToLifecycle(this, selector(camera), livePreview(), capture)
                imageCapture = capture; videoCapture = VideoCapture.withOutput(recorder)
                if (showReady) showStatus("Ready. Photos review in memory; silent videos use private temporary disk storage.")
            } catch (_: Exception) {
                imageCapture = null; videoCapture = null
                showStatus("Camera unavailable. Retry after returning to the app.")
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
                        val request = SafetyCaptureContext(SafetyCaptureId(UUID.randomUUID().toString()), System.currentTimeMillis(),
                            if (rotated) image.height else image.width, if (rotated) image.width else image.height, 0, frontFacing)
                        val sink = object : ApprovedPhotoSink<Bitmap> {
                            override suspend fun save(input: Bitmap, capture: SafetyCaptureContext, approval: ManagedCaptureApproval) {
                                currentCoroutineContext().ensureActive(); check(token.get())
                                approved.savePhoto(input, capture, approval)
                            }
                        }
                        val result = ManagedPhotoReviewPipeline(OpenNsfw2BitmapEvaluator(requireNotNull(runtime)), sink,
                            OpenNsfw2ModelPreflight.configuration).reviewImageProxy(ownedImage, request) {
                                val raw = it.toBitmap()
                                bitmap = raw
                                val rotation = it.imageInfo.rotationDegrees
                                bitmap = if (rotation == 0) raw else Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height,
                                    Matrix().apply { postRotate(rotation.toFloat()) }, true).also { rotated -> if (rotated !== raw) raw.recycle() }
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
            camera.unbindAll(); camera.bindToLifecycle(this, selector(camera), livePreview(), clip)
            val start = sessions.startRecording(System.currentTimeMillis())
            if (start !is VideoRecordingStartResult.Started) { cleanupBlocked = true; finishCapture("Private cleanup is required."); return }
            val current = start.session; session = current
            val token = active
            val factory = CameraXPrivateVideoRecordingFactory(store)
            val pending = clip.output.prepareRecording(this, factory.outputOptions(current))
            recording = factory.start(pending, ContextCompat.getMainExecutor(this)) { event ->
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
    private fun stopVideo() { recording?.stop(); showStatus("Finalizing private video…"); video.isEnabled = false }
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun reviewVideo(current: VideoStagingSession, token: AtomicBoolean) {
        showStatus("Reviewing sampled video frames…")
        work = lifecycleScope.launch(dispatcher, start = CoroutineStart.ATOMIC) {
            try {
                val result = AndroidVideoReviewBridge(store, pipeline).review(current, requireNotNull(runtime)) { promoting ->
                    check(token.get() && promoting.id == current.id && promoting.state == VideoStagingState.Promoting)
                    approved.saveVideo(store.recordingOutputFile(promoting.id))
                }
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
        busy = false; session = null
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
        photo.isEnabled = foreground && permitted && runtime != null && imageCapture != null && !busy && !cleanupBlocked
        video.isEnabled = recording != null || (foreground && permitted && runtime != null && videoCapture != null && !busy && !cleanupBlocked)
        video.text = if (recording != null) "Stop video" else "Record video"
        cancel.isEnabled = busy; retry.visibility = if (cleanupBlocked) View.VISIBLE else View.GONE
    }
    private fun showStatus(message: String) { status.text = message }
    private fun refreshMedia() {
        if (!::approved.isInitialized) return
        mediaList.removeAllViews()
        approved.items().forEachIndexed { index, file ->
            mediaList.addView(button("Open ${if (file.extension == "jpg") "photo" else "video"} ${index + 1}", View.generateViewId()) { openApproved(file) })
        }
        if (mediaList.childCount == 0) mediaList.addView(TextView(this).apply { text = "No approved media yet." })
    }
    private fun openApproved(file: File) {
        if (file !in approved.items()) return
        if (file.extension == "jpg") {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return
            val image = ImageView(this).apply { setImageBitmap(bitmap); adjustViewBounds = true }
            viewer = AlertDialog.Builder(this).setTitle("Approved photo").setView(image).setPositiveButton("Close", null).create().apply {
                setOnDismissListener { image.setImageDrawable(null); bitmap.recycle(); viewer = null }; show()
            }
        } else {
            val clip = VideoView(this).apply { setVideoPath(file.absolutePath); setOnPreparedListener { start() } }
            viewer = AlertDialog.Builder(this).setTitle("Approved video").setView(clip).setPositiveButton("Close", null).create().apply {
                setOnDismissListener { clip.stopPlayback(); viewer = null }; show()
            }
        }
    }
    override fun onStart() { super.onStart(); foreground = true; if (::status.isInitialized) { bindCamera(); refreshControls() } }
    override fun onStop() { foreground = false; viewer?.dismiss(); cancelCapture(); provider?.unbindAll(); refreshControls(); super.onStop() }
    override fun onDestroy() {
        worker.execute { runtime?.close() }; dispatcher.close(); super.onDestroy()
    }
    companion object {
        const val PREVIEW_ID = 1001; const val STATUS_ID = 1002; const val PERMISSION_ID = 1003
        const val PHOTO_ID = 1004; const val VIDEO_ID = 1005; const val CANCEL_ID = 1006
        const val RETRY_ID = 1007; const val MEDIA_ID = 1008
    }
}
