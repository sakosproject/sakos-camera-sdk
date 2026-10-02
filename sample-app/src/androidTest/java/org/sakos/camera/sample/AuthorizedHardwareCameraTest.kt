package org.sakos.camera.sample

import android.Manifest
import android.app.AlertDialog
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.sakos.camera.capture.camerax.*
import org.sakos.camera.capture.video.*
import org.sakos.camera.safety.core.*

/** Explicit operator opt-in only. Never captures media during ordinary synthetic/CI runs. */
@RunWith(AndroidJUnit4::class)
class AuthorizedHardwareCameraTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test fun approvedOfficeViewsCalibrationControlsCaptureAndCleanup() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveCameraConsent") == "office-floor-ceiling")
        check(Build.MODEL == "SM-G781W") { "This authorization is limited to the selected Samsung model." }
        val context = instrumentation.targetContext
        assertEquals(PackageManager.PERMISSION_DENIED, context.checkSelfPermission(Manifest.permission.CAMERA))
        emit("consent", "Operator approved temporary floor/ceiling preview/photo/video; app-private media only")
        ActivityScenario.launch(SampleActivity::class.java).use { scenario ->
            scenario.onActivity {
                assertFalse(it.findViewById<Button>(SampleActivity.PHOTO_ID).isEnabled)
                assertEquals(View.VISIBLE, it.findViewById<View>(SampleActivity.PERMISSION_ID).visibility)
            }
            emit("permission-denied", "Capture disabled before permission")
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.CAMERA)
            scenario.recreate()
            awaitReady(scenario, 480_000)
            var originalProfile: CameraQualityCalibrationProfile? = null
            scenario.onActivity { activity ->
                val profile = field(activity, "calibration") as CameraQualityCalibrationProfile
                val environment = field(activity, "calibrationEnvironment") as CameraQualityCalibrationEnvironment
                assertTrue(profile.mandatoryReadiness(environment).ready)
                assertTrue(profile.records.all { it.calibrationArtifactCleanupSucceeded == true })
                assertEquals(0, ApprovedMediaStore(activity).items().size)
                originalProfile = profile
                val records = JSONArray()
                profile.records.forEach { r -> records.put(JSONObject().put("lens", r.lensLabel).put("mode", r.mode.name)
                    .put("tier", r.tier.name).put("status", r.status.name).put("width", r.finalWidth).put("height", r.finalHeight)
                    .put("gate", r.gateStatusLabel).put("coverage", r.gateCoverage?.name).put("cleanup", r.calibrationArtifactCleanupSucceeded)) }
                val zoom = JSONArray()
                profile.zoomRecords.forEach { r -> zoom.put(JSONObject().put("mode", r.mode.name).put("ratio", r.requestedZoomRatio)
                    .put("status", r.status.name).put("observed", r.observedZoomRatio)) }
                emit("calibration", JSONObject().put("ready", true).put("records", records).put("zoom", zoom).toString())
                val provider = field(activity, "provider") as ProcessCameraProvider
                val inventory = JSONArray()
                CameraCapabilityDiscovery.discover(provider).forEach { r -> inventory.put(JSONObject().put("id", r.cameraId)
                    .put("lens", r.lens).put("flash", r.flash).put("minZoom", r.minZoom).put("maxZoom", r.maxZoom)
                    .put("videoQualities", JSONArray(r.videoQualities))) }
                emit("sdk-inventory", inventory.toString())
            }
            photo(scenario, "back-default")
            var tiers = emptyList<CameraCalibratedQualityTier>()
            scenario.onActivity { tiers = (field(it, "calibration") as CameraQualityCalibrationProfile)
                .childFacingVisibleTiersFor(CameraQualityCalibrationMode.Photo, "back") }
            repeat(tiers.size.coerceAtMost(3)) {
                var previous = ""
                scenario.onActivity { previous = it.findViewById<Button>(SampleActivity.QUALITY_ID).text.toString()
                    it.findViewById<Button>(SampleActivity.QUALITY_ID).performClick() }
                await(scenario) { ready(it) && (tiers.size == 1 || it.findViewById<Button>(SampleActivity.QUALITY_ID).text.toString() != previous) }
                var low = false
                scenario.onActivity { low = field(it, "selectedTier") == CameraCalibratedQualityTier.Low
                    emit("quality", it.findViewById<Button>(SampleActivity.QUALITY_ID).text.toString()) }
                if (low) photo(scenario, "back-low", true)
            }
            var flashSupported = false
            scenario.onActivity { flashSupported = it.findViewById<Button>(SampleActivity.FLASH_ID).isEnabled }
            if (flashSupported) {
                val observed = mutableSetOf<Int>()
                repeat(3) {
                    var previous = -1
                    scenario.onActivity { previous = requireNotNull(graph(it).photo).flashMode; it.findViewById<Button>(SampleActivity.FLASH_ID).performClick() }
                    await(scenario) { ready(it) && graph(it).photo?.flashMode != previous }
                    var on = false
                    scenario.onActivity { val mode = requireNotNull(graph(it).photo).flashMode; observed.add(mode); on = mode == ImageCapture.FLASH_MODE_ON
                        emit("flash", it.findViewById<Button>(SampleActivity.FLASH_ID).text.toString()) }
                    if (on) photo(scenario, "back-flash-on")
                }
                assertEquals(setOf(ImageCapture.FLASH_MODE_OFF, ImageCapture.FLASH_MODE_AUTO, ImageCapture.FLASH_MODE_ON), observed)
            } else emit("flash", "Not available on selected back graph")
            var stops = emptyList<Float>()
            scenario.onActivity {
                val g = graph(it); val z = requireNotNull(g.camera.cameraInfo.zoomState.value)
                stops = (field(it, "calibration") as CameraQualityCalibrationProfile)
                    .childFacingVisibleZoomStopsFor(CameraQualityCalibrationMode.Photo, "back", z.minZoomRatio, z.maxZoomRatio)
            }
            repeat(stops.size.coerceAtMost(4)) {
                scenario.onActivity { it.findViewById<Button>(SampleActivity.ZOOM_ID).performClick() }
                await(scenario) { val requested = field(it, "selectedZoom") as Float
                    val actual = graph(it).camera.cameraInfo.zoomState.value?.zoomRatio
                    actual != null && kotlin.math.abs(requested - actual) < .02f }
                scenario.onActivity { emit("zoom", "requested=${field(it, "selectedZoom")}; observed=${graph(it).camera.cameraInfo.zoomState.value?.zoomRatio}") }
            }
            scenario.onActivity { activity ->
                val view = activity.findViewById<PreviewView>(SampleActivity.PREVIEW_ID)
                val time = SystemClock.uptimeMillis()
                MotionEvent.obtain(time, time, MotionEvent.ACTION_UP, view.width / 2f, view.height / 2f, 0).let { event ->
                    try { assertTrue(view.dispatchTouchEvent(event)) } finally { event.recycle() }
                }
            }
            SystemClock.sleep(3500)
            emit("tap-focus", "Owned preview AF/AE tap submitted; waited through 3-second auto-cancel (focus accuracy not asserted)")
            video(scenario, "back", true)
            switchLens(scenario, "front")
            scenario.onActivity {
                assertFalse(it.findViewById<Button>(SampleActivity.ZOOM_ID).isEnabled)
                assertEquals(1f, field(it, "selectedZoom") as Float, .001f)
                assertEquals(graph(it).camera.cameraInfo.hasFlashUnit(), it.findViewById<Button>(SampleActivity.FLASH_ID).isEnabled)
            }
            emit("front-controls", "Front selected; selfie zoom fixed at 1x; flash availability respected")
            photo(scenario, "front-default")
            video(scenario, "front", false)
            val recordTimes = requireNotNull(originalProfile).records.map { it.updatedAtMillis }
            scenario.recreate(); awaitReady(scenario)
            scenario.onActivity {
                assertEquals(recordTimes, (field(it, "calibration") as CameraQualityCalibrationProfile).records.map { r -> r.updatedAtMillis })
                assertEquals("Camera: front", it.findViewById<Button>(SampleActivity.LENS_ID).text.toString())
            }
            emit("cache-recreation", "Ready encrypted profile reused without rerunning calibration; front selection retained")
            scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            SystemClock.sleep(1200); awaitReady(scenario)
            photo(scenario, "front-landscape-ui")
            scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
            SystemClock.sleep(800); awaitReady(scenario)
            emit("orientation", "Activity landscape/recreation capture worked; phone was not physically rotated")
            switchLens(scenario, "back")
            cancelVideo(scenario, false)
            cancelVideo(scenario, true)
            scenario.onActivity {
                val library = ApprovedMediaStore(it).library
                val items = library.items()
                emit("owned-approved-counts", "photos=${items.count { r -> r.kind == ReviewedMediaKind.Photo }}; videos=${items.count { r -> r.kind == ReviewedMediaKind.Video }}")
                items.forEach { item -> assertTrue(library.delete(item)) }
                assertTrue(library.items().isEmpty())
                assertTrue(AndroidVideoPrivateStagingStore(it).sessions().all { s -> s.state == VideoStagingState.Completed })
            }
            emit("private-cleanup", "All owned approved entries removed; no unresolved staging sessions")
        }
        emit("complete", "Bounded authorized hardware flow passed; no images or clips copied off-device")
    }

    private fun photo(scenario: ActivityScenario<SampleActivity>, label: String, low: Boolean = false) {
        var before = emptySet<String>()
        scenario.onActivity { before = ApprovedMediaStore(it).items().map { item -> item.id }.toSet(); it.findViewById<Button>(SampleActivity.PHOTO_ID).performClick() }
        await(scenario, 120_000) { ready(it) && status(it).startsWith("Photo ") }
        scenario.onActivity { activity ->
            val store = ApprovedMediaStore(activity); val added = store.items().filter { it.id !in before }; val result = status(activity)
            if (result == "Photo approved.") {
                assertEquals(1, added.size); assertEquals(ReviewedMediaKind.Photo, added.single().kind)
                if (low) assertTrue(added.single().width.toLong() * added.single().height <= 3_000_000)
                val list = activity.findViewById<android.widget.LinearLayout>(SampleActivity.MEDIA_ID)
                assertTrue(list.childCount > 0); (list.getChildAt(0) as Button).performClick()
                val viewer = field(activity, "viewer") as AlertDialog
                assertTrue(viewer.isShowing); viewer.dismiss()
                emit("photo-$label", "$result private delta=1; ${added.single().width}x${added.single().height}; approved viewer opened/closed")
            } else {
                assertEquals("Photo not approved; no image saved.", result); assertTrue(added.isEmpty())
                emit("photo-$label", "$result private delta=0; classifier efficacy not assessed")
            }
        }
    }
    private fun video(scenario: ActivityScenario<SampleActivity>, lens: String, pause: Boolean) {
        var before = 0
        scenario.onActivity { before = ApprovedMediaStore(it).items().size; it.findViewById<Button>(SampleActivity.VIDEO_ID).performClick() }
        await(scenario) { it.findViewById<Button>(SampleActivity.VIDEO_ID).text == "Stop video" }
        SystemClock.sleep(1300)
        if (pause) {
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed(); assertEquals("Stop video", it.findViewById<Button>(SampleActivity.VIDEO_ID).text.toString())
                it.findViewById<Button>(SampleActivity.PAUSE_ID).performClick(); assertEquals("Resume video", it.findViewById<Button>(SampleActivity.PAUSE_ID).text.toString()) }
            SystemClock.sleep(400)
            scenario.onActivity { it.findViewById<Button>(SampleActivity.PAUSE_ID).performClick(); assertEquals("Pause video", it.findViewById<Button>(SampleActivity.PAUSE_ID).text.toString()) }
            SystemClock.sleep(800)
        }
        scenario.onActivity { it.findViewById<Button>(SampleActivity.VIDEO_ID).performClick() }
        await(scenario, 180_000) { ready(it) && status(it).startsWith("Video ") }
        scenario.onActivity {
            val result = status(it); val after = ApprovedMediaStore(it).items().size
            if (result == "Video approved.") assertEquals(before + 1, after)
            else { assertEquals("Video not delivered; private cleanup requested.", result); assertEquals(before, after) }
            assertTrue(AndroidVideoPrivateStagingStore(it).sessions().all { s -> s.state == VideoStagingState.Completed })
            emit("video-$lens", "$result private delta=${after - before}; pause/resume/back-guard=$pause; staging terminal")
        }
    }
    private fun cancelVideo(scenario: ActivityScenario<SampleActivity>, background: Boolean) {
        var before = 0
        scenario.onActivity { before = ApprovedMediaStore(it).items().size; it.findViewById<Button>(SampleActivity.VIDEO_ID).performClick() }
        await(scenario) { it.findViewById<Button>(SampleActivity.VIDEO_ID).text == "Stop video" }; SystemClock.sleep(800)
        if (background) {
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED); SystemClock.sleep(1200)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        } else scenario.onActivity { it.findViewById<Button>(SampleActivity.CANCEL_ID).performClick() }
        awaitReady(scenario)
        scenario.onActivity { assertEquals(before, ApprovedMediaStore(it).items().size)
            assertTrue(AndroidVideoPrivateStagingStore(it).sessions().all { s -> s.state == VideoStagingState.Completed }) }
        emit(if (background) "background-cancel" else "explicit-cancel", "Capture returned ready; no private promotion or unresolved staging")
    }
    private fun switchLens(scenario: ActivityScenario<SampleActivity>, lens: String) {
        scenario.onActivity { assertTrue(it.findViewById<Button>(SampleActivity.LENS_ID).isEnabled); it.findViewById<Button>(SampleActivity.LENS_ID).performClick() }
        await(scenario) { ready(it) && it.findViewById<Button>(SampleActivity.LENS_ID).text == "Camera: $lens" &&
            graph(it).camera.cameraInfo.lensFacing == if (lens == "front") CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK }
        emit("lens", lens)
    }
    private fun awaitReady(scenario: ActivityScenario<SampleActivity>, timeout: Long = 90_000) = await(scenario, timeout) { ready(it) }
    private fun await(scenario: ActivityScenario<SampleActivity>, timeout: Long = 90_000, check: (SampleActivity) -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeout
        var nextProgress = 0L
        while (SystemClock.elapsedRealtime() < deadline) {
            var done = false; var current = ""
            scenario.onActivity { done = check(it); current = status(it) }
            if (done) return
            if (current.startsWith("Camera calibration unavailable") || current == "Camera calibration failed. Retry is available.") {
                scenario.onActivity { emit("failed-calibration", (field(it, "calibration") as? CameraQualityCalibrationProfile)?.records?.joinToString { r -> "${r.lensLabel}/${r.mode}/${r.tier}:${r.status}/${r.failureReason}" } ?: current) }
                fail(current)
            }
            if (SystemClock.elapsedRealtime() > nextProgress) { emit("progress", current); nextProgress = SystemClock.elapsedRealtime() + 10_000 }
            SystemClock.sleep(150)
        }
        scenario.onActivity { fail("Hardware flow timed out: ${status(it)}") }
    }
    private fun ready(activity: SampleActivity): Boolean = activity.findViewById<Button>(SampleActivity.PHOTO_ID).isEnabled &&
        (field(activity, "graph") as? CameraGraphBinding)?.photo?.resolutionInfo != null &&
        activity.findViewById<PreviewView>(SampleActivity.PREVIEW_ID).previewStreamState.value == PreviewView.StreamState.STREAMING
    private fun status(activity: SampleActivity) = activity.findViewById<TextView>(SampleActivity.STATUS_ID).text.toString()
    private fun graph(activity: SampleActivity) = field(activity, "graph") as CameraGraphBinding
    private fun field(activity: SampleActivity, name: String): Any? = SampleActivity::class.java.getDeclaredField(name).apply { isAccessible = true }.get(activity)
    private fun emit(stage: String, detail: String) = instrumentation.sendStatus(0, android.os.Bundle().apply {
        putString("stream", "HARDWARE ${JSONObject().put("stage", stage).put("detail", detail)}\n")
    })
}
