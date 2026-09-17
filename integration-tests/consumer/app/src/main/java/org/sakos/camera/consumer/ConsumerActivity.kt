package org.sakos.camera.consumer

import android.app.Activity
import android.os.Bundle
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
    }
}
