package org.sakos.camera.consumer

import android.app.Activity
import android.os.Bundle
import android.graphics.Bitmap
import android.graphics.Color
import android.widget.TextView
import org.sakos.camera.safety.opennsfw2.OpenNsfw2BitmapRuntime
import org.sakos.camera.capture.video.VideoGatePolicy
import org.sakos.camera.capture.video.VideoTemporalReviewDecision
import org.sakos.camera.safety.core.SafetyCaptureId
import org.sakos.camera.safety.opennsfw2.IntegratedStillGatePolicyDefaults

class ConsumerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(SafetyCaptureId("consumer-smoke").value == "consumer-smoke")
        check(VideoGatePolicy.maxDecodedSamples == 35)
        check(IntegratedStillGatePolicyDefaults.fallback.elevatedDetectionFloor > 0f)
        check(VideoTemporalReviewDecision.Allow.name == "Allow")
        // Locally generated benign pixels exercise the packaged native runtime, not accuracy.
        val bitmap = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        try {
            for (strategy in org.sakos.camera.safety.opennsfw2.IntegratedOpenNsfw2Strategy.entries) {
                OpenNsfw2BitmapRuntime.open(this, threadCount = 1, strategy = strategy).use { runtime ->
                    val result = runtime.evaluate(bitmap)
                    check(result.strategyId == strategy.id && result.evaluatedViews.isNotEmpty())
                    check(result.evaluatedViews.all { it.scores.nsfwProbability.isFinite() })
                }
            }
            // Source-derived calibration serialization/selector path must survive R8 too.
            val environment = org.sakos.camera.capture.camerax.CameraQualityCalibrationEnvironment.current(this, "synthetic-model", "synthetic-inventory")
            val storage = org.sakos.camera.capture.camerax.AndroidCameraCalibrationProfileStorage(this, environment)
            storage.reset()
            val step = org.sakos.camera.capture.camerax.CameraCalibrationRunner.requiredSteps.first()
            storage.record(org.sakos.camera.capture.camerax.CameraQualityCalibrationRecord(step.preset.mode, step.lens, step.preset.tier,
                org.sakos.camera.capture.camerax.CameraQualityCalibrationStatus.Failed, "synthetic", null, null, null, null, null, null, null, "simulated", 0,
                graphPreset = step.preset, calibrationArtifactCleanupSucceeded = true))
            check(storage.load().records.isNotEmpty())
            check(!storage.load().mandatoryReadiness(environment).ready)
            setContentView(TextView(this).apply { text = "Synthetic packaged runtime: OK" })
        } finally { bitmap.recycle() }
    }
}
