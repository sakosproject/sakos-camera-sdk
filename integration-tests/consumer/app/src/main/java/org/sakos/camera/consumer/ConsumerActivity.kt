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
            OpenNsfw2BitmapRuntime.open(this, threadCount = 1).use { runtime ->
                check(runtime.evaluate(bitmap).evaluatedViews.all { it.scores.nsfwProbability.isFinite() })
            }
            setContentView(TextView(this).apply { text = "Synthetic packaged runtime: OK" })
        } finally { bitmap.recycle() }
    }
}
