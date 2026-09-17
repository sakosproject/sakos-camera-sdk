/* Selectively extracted from user-authorized source commit 843d93f. */
package org.sakos.camera.capture.video

object VideoGatePolicy {
    const val maxNormalDurationMillis: Long = 10L * 60L * 1000L
    const val endpointPaddingMillis: Long = 500L
    const val baseIntervalMillis: Long = 30L * 1000L
    const val shortClipBaseSampleCount: Int = 5
    const val mediumClipTargetSampleCount: Int = 9
    const val minLongClipBaseSampleCount: Int = 9
    const val maxBaseSamples: Int = 25
    const val maxEscalatedTimestamps: Int = 6
    const val confirmationSamplesPerSuspiciousTimestamp: Int = 2
    const val maxDecodedSamples: Int = 35
    const val confirmationOffsetMillis: Long = 750L
    const val cropOnlyExtremeRequiresTemporalCorroboration: Boolean = true
}
