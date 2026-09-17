package org.sakos.camera.sample

enum class SampleCaptureMode {
    Photo,
    Video,
}

enum class SampleCaptureAvailability {
    RuntimeUnavailable,
    CleanupRetryRequired,
    Ready,
}

data class SampleUiState(
    val mode: SampleCaptureMode = SampleCaptureMode.Photo,
    val availability: SampleCaptureAvailability = SampleCaptureAvailability.RuntimeUnavailable,
    val approvedMediaCount: Int = 0,
) {
    init {
        require(approvedMediaCount >= 0) { "approved media count must not be negative." }
    }

    val availabilityMessage: String
        get() = when (availability) {
            SampleCaptureAvailability.RuntimeUnavailable ->
                "Capture is unavailable until the bundled model, private file store, and CameraX recorder are connected."
            SampleCaptureAvailability.CleanupRetryRequired ->
                "Private staging cleanup needs attention before another recording can start."
            SampleCaptureAvailability.Ready -> "Ready to capture and review in the managed SDK path."
        }
}

object SampleUiReducer {
    fun selectMode(state: SampleUiState, mode: SampleCaptureMode): SampleUiState = state.copy(mode = mode)

    fun reportCleanupRetry(state: SampleUiState): SampleUiState =
        state.copy(availability = SampleCaptureAvailability.CleanupRetryRequired)
}
