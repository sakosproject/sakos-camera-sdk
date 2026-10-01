package org.sakos.camera.sample

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.sakos.camera.safety.core.*
import org.sakos.camera.safety.opennsfw2.OpenNsfw2ModelPreflight

/** Restricted to an emulator scene; assertions concern mechanics, never accuracy. */
@org.junit.FixMethodOrder(org.junit.runners.MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class SyntheticSampleFlowTest {
    @Test fun aPermissionPhotoVideoCancellationAndBackgroundRecovery() {
        check(Build.MODEL.contains("sdk", true) || Build.FINGERPRINT.contains("emulator", true)) { "Emulator only" }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val pattern = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.CYAN) }
        val syntheticCapture = SafetyCaptureContext(SafetyCaptureId("synthetic-viewer"), 0, 32, 32, 0, false)
        val simulated = SafetyEvaluationOutcome.Decision(syntheticCapture.captureId, SafetyEvaluationReceiptId("simulated-viewer-allow"),
            OpenNsfw2ModelPreflight.configuration, SafetyDecision.Allow)
        try { ApprovedMediaStore(instrumentation.targetContext).savePhoto(pattern, syntheticCapture,
            requireNotNull(simulated.approvalForManagedCapture(syntheticCapture))) } finally { pattern.recycle() }
        ActivityScenario.launch(SampleActivity::class.java).use { scenario ->
            scenario.onActivity {
                assertFalse(it.findViewById<Button>(SampleActivity.PHOTO_ID).isEnabled)
                assertEquals(View.VISIBLE, it.findViewById<View>(SampleActivity.PERMISSION_ID).visibility)
            }
            instrumentation.uiAutomation.grantRuntimePermission(instrumentation.targetContext.packageName, Manifest.permission.CAMERA)
            scenario.recreate()
            await(scenario, 180_000) { it.findViewById<Button>(SampleActivity.PHOTO_ID).isEnabled }
            // Exercise the viewer with the synthetic cyan image committed above.
            var opened = false
            scenario.onActivity {
                val list = it.findViewById<android.widget.LinearLayout>(SampleActivity.MEDIA_ID)
                val first = list.getChildAt(0)
                if (first is Button && first.text.toString().startsWith("Open photo")) { first.performClick(); opened = true }
            }
            assertTrue("Synthetic approved item must be available to the viewer", opened)
            if (opened) {
                instrumentation.waitForIdleSync()
                assertTrue(instrumentation.uiAutomation.rootInActiveWindow.findAccessibilityNodeInfosByText("Approved photo").isNotEmpty())
                instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
            }
            scenario.onActivity { it.findViewById<Button>(SampleActivity.PHOTO_ID).performClick() }
            await(scenario) {
                it.findViewById<TextView>(SampleActivity.STATUS_ID).text.toString().startsWith("Photo ") &&
                    it.findViewById<Button>(SampleActivity.PHOTO_ID).isEnabled
            }
            scenario.onActivity { it.findViewById<Button>(SampleActivity.VIDEO_ID).performClick() }
            await(scenario) { it.findViewById<Button>(SampleActivity.VIDEO_ID).text == "Stop video" }
            SystemClock.sleep(1000)
            scenario.onActivity { it.findViewById<Button>(SampleActivity.PAUSE_ID).performClick(); assertEquals("Resume video", it.findViewById<Button>(SampleActivity.PAUSE_ID).text.toString()) }
            SystemClock.sleep(500)
            scenario.onActivity { it.findViewById<Button>(SampleActivity.PAUSE_ID).performClick(); assertEquals("Pause video", it.findViewById<Button>(SampleActivity.PAUSE_ID).text.toString()) }
            SystemClock.sleep(1500)
            scenario.onActivity { it.findViewById<Button>(SampleActivity.VIDEO_ID).performClick() }
            await(scenario, 120_000) {
                it.findViewById<TextView>(SampleActivity.STATUS_ID).text.toString().startsWith("Video ") &&
                    it.findViewById<Button>(SampleActivity.PHOTO_ID).isEnabled
            }
            scenario.onActivity {
                val store = org.sakos.camera.capture.video.AndroidVideoPrivateStagingStore(it)
                assertTrue(store.sessions().all { s -> s.state == org.sakos.camera.capture.video.VideoStagingState.Completed })
                it.findViewById<Button>(SampleActivity.VIDEO_ID).performClick()
            }
            await(scenario) { it.findViewById<Button>(SampleActivity.VIDEO_ID).text == "Stop video" }
            SystemClock.sleep(1000)
            scenario.onActivity { it.findViewById<Button>(SampleActivity.CANCEL_ID).performClick() }
            await(scenario) { it.findViewById<Button>(SampleActivity.PHOTO_ID).isEnabled }
            scenario.onActivity { it.findViewById<Button>(SampleActivity.VIDEO_ID).performClick() }
            await(scenario) { it.findViewById<Button>(SampleActivity.VIDEO_ID).text == "Stop video" }
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            SystemClock.sleep(1500)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            await(scenario) { it.findViewById<Button>(SampleActivity.PHOTO_ID).isEnabled }
            scenario.onActivity {
                assertTrue(org.sakos.camera.capture.video.AndroidVideoPrivateStagingStore(it).sessions()
                    .all { s -> s.state == org.sakos.camera.capture.video.VideoStagingState.Completed })
            }
        }
    }

    @Test fun bSyntheticApprovedStoreRejectsMismatchedReceiptAndRecoversPendingWrites() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = ApprovedMediaStore(context)
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.CYAN) }
        val capture = SafetyCaptureContext(SafetyCaptureId("synthetic-store"), 0, 32, 32, 0, false)
        val outcome = SafetyEvaluationOutcome.Decision(capture.captureId, SafetyEvaluationReceiptId("simulated-allow"),
            OpenNsfw2ModelPreflight.configuration, SafetyDecision.Allow)
        val before = store.items().size
        try {
            val receipt = requireNotNull(outcome.approvalForManagedCapture(capture))
            assertThrows(IllegalArgumentException::class.java) {
                store.savePhoto(bitmap, capture.copy(captureId = SafetyCaptureId("other")), receipt)
            }
            assertEquals(before, store.items().size)
            store.savePhoto(bitmap, capture, receipt)
            assertEquals(before + 1, store.items().size)
            val pending = java.io.File(context.noBackupFilesDir, "sakos-reviewed-library/synthetic.pending")
            pending.mkdir(); java.io.File(pending, "partial").writeBytes(byteArrayOf(1, 2, 3))
            assertTrue(store.items().none { it.id == "synthetic.pending" })
            ApprovedMediaStore(context)
            assertFalse(pending.exists())
        } finally { bitmap.recycle() }
    }

    @Test fun cBothCamerasCacheRecreationAndOrientation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        check(Build.MODEL.contains("sdk", true) || Build.FINGERPRINT.contains("emulator", true))
        instrumentation.uiAutomation.grantRuntimePermission(instrumentation.targetContext.packageName, Manifest.permission.CAMERA)
        ActivityScenario.launch(SampleActivity::class.java).use { scenario ->
            await(scenario, 180_000) { it.findViewById<Button>(SampleActivity.PHOTO_ID).isEnabled }
            scenario.onActivity {
                assertTrue(it.findViewById<Button>(SampleActivity.LENS_ID).isEnabled)
                it.findViewById<Button>(SampleActivity.LENS_ID).performClick()
            }
            await(scenario) { it.findViewById<Button>(SampleActivity.PHOTO_ID).isEnabled && it.findViewById<Button>(SampleActivity.LENS_ID).text == "Camera: front" }
            scenario.onActivity { it.findViewById<Button>(SampleActivity.PHOTO_ID).performClick() }
            await(scenario) { it.findViewById<Button>(SampleActivity.PHOTO_ID).isEnabled && it.findViewById<TextView>(SampleActivity.STATUS_ID).text.startsWith("Photo ") }
            scenario.recreate()
            await(scenario) { it.findViewById<Button>(SampleActivity.PHOTO_ID).isEnabled && it.findViewById<Button>(SampleActivity.LENS_ID).text == "Camera: front" }
            scenario.onActivity { it.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            SystemClock.sleep(1500)
            await(scenario) { it.findViewById<Button>(SampleActivity.PHOTO_ID).isEnabled }
            scenario.onActivity { it.findViewById<Button>(SampleActivity.LENS_ID).performClick() }
            await(scenario) { it.findViewById<Button>(SampleActivity.PHOTO_ID).isEnabled && it.findViewById<Button>(SampleActivity.LENS_ID).text == "Camera: back" }
            scenario.onActivity { it.findViewById<Button>(SampleActivity.PHOTO_ID).performClick() }
            await(scenario) { it.findViewById<Button>(SampleActivity.PHOTO_ID).isEnabled && it.findViewById<TextView>(SampleActivity.STATUS_ID).text.startsWith("Photo ") }
            var beforeCancel = 0
            scenario.onActivity { beforeCancel = ApprovedMediaStore(it).items().size; it.findViewById<Button>(SampleActivity.VIDEO_ID).performClick() }
            await(scenario) { it.findViewById<Button>(SampleActivity.VIDEO_ID).text == "Stop video" }
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            SystemClock.sleep(1500); scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            await(scenario) { it.findViewById<Button>(SampleActivity.PHOTO_ID).isEnabled }
            scenario.onActivity { assertEquals(beforeCancel, ApprovedMediaStore(it).items().size) }
            scenario.onActivity { it.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        }
    }

    @Test fun dEncryptedCalibrationCacheRejectsChangedIdentityAndInterruptedWrites() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val environment = org.sakos.camera.capture.camerax.CameraQualityCalibrationEnvironment.current(context, "synthetic-cache-model", "synthetic-cache-inventory")
        val store = org.sakos.camera.capture.camerax.CameraQualityCalibrationStore(environment)
        store.clear(context)
        val step = org.sakos.camera.capture.camerax.CameraCalibrationRunner.requiredSteps.first()
        val record = org.sakos.camera.capture.camerax.CameraQualityCalibrationRecord(step.preset.mode, step.lens, step.preset.tier,
            org.sakos.camera.capture.camerax.CameraQualityCalibrationStatus.Failed, "synthetic probe", null, null, null, null, null, null, null, "simulated failure", 0,
            graphPreset = step.preset, calibrationArtifactCleanupSucceeded = true)
        store.recordObservation(context, record)
        assertTrue(store.load(context).records.isNotEmpty())
        assertFalse(store.profileFile(context).readText().contains("simulated failure"))
        val changed = org.sakos.camera.capture.camerax.CameraQualityCalibrationStore(environment.copy(modelId = "changed-synthetic-model"))
        assertTrue(changed.load(context).records.isEmpty())
        assertFalse(changed.load(context).mandatoryReadiness(environment).ready)
        val pending = java.io.File(store.profileFile(context).parentFile, store.profileFile(context).name + ".pending")
        pending.writeBytes(byteArrayOf(1, 2, 3)); store.load(context); assertFalse(pending.exists())
        store.clear(context)
    }

    private fun await(scenario: ActivityScenario<SampleActivity>, timeout: Long = 60_000, check: (SampleActivity) -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeout
        while (SystemClock.elapsedRealtime() < deadline) {
            var done = false
            scenario.onActivity { done = check(it) }
            if (done) return
            SystemClock.sleep(200)
        }
        scenario.onActivity { fail("Sample flow timed out: ${it.findViewById<TextView>(SampleActivity.STATUS_ID).text}") }
    }
}
