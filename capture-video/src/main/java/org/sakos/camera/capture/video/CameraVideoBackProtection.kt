/* Source-derived lifecycle signal; host chooses UI behavior. */
package org.sakos.camera.capture.video

public enum class CameraVideoBackProtection(val reminder: String) {
    None(""),
    Recording("Tap Stop to finish recording"),
    Finishing("Finishing video. Please wait."),
}

public fun cameraVideoBackProtection(
    capturePending: Boolean,
    recordingActive: Boolean,
    recordingInFlight: Boolean,
    stopRequested: Boolean,
    reviewInFlight: Boolean,
): CameraVideoBackProtection = when {
    stopRequested || reviewInFlight -> CameraVideoBackProtection.Finishing
    capturePending || recordingActive || recordingInFlight -> CameraVideoBackProtection.Recording
    else -> CameraVideoBackProtection.None
}
