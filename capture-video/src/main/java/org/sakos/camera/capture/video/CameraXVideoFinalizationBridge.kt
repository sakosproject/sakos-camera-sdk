package org.sakos.camera.capture.video

import androidx.camera.video.VideoRecordEvent
import androidx.core.util.Consumer

/** Testable subset of a CameraX finalization event; non-null [errorCode] is always non-approved. */
data class VideoRecordingFinalization(
    val recordedDurationNanos: Long,
    val errorCode: Int? = null,
) {
    init {
        require(recordedDurationNanos >= 0L) { "recorded duration must not be negative." }
    }
}

sealed interface CameraXVideoFinalizationResult {
    data class ReadyForReview(
        val session: VideoStagingSession,
        val durationMillis: Long,
    ) : CameraXVideoFinalizationResult

    data class Discarded(
        val errorCode: Int,
        val cleanup: VideoStagingTransitionResult,
    ) : CameraXVideoFinalizationResult

    data class Rejected(val detail: String) : CameraXVideoFinalizationResult
}

/**
 * Moves a CameraX recording into the durable Reviewing state exactly once. A CameraX finalization
 * error bypasses review and promotion and immediately requests private staging cleanup.
 */
class CameraXVideoFinalizationBridge(
    private val sessions: VideoStagingSessionManager,
) {
    fun finalize(
        session: VideoStagingSession,
        finalization: VideoRecordingFinalization,
    ): CameraXVideoFinalizationResult {
        finalization.errorCode?.let { errorCode ->
            return CameraXVideoFinalizationResult.Discarded(errorCode, sessions.discard(session.id))
        }
        return when (val reviewing = sessions.markReviewing(session.id)) {
            is VideoStagingTransitionResult.Updated -> CameraXVideoFinalizationResult.ReadyForReview(
                session = reviewing.session,
                durationMillis = finalization.recordedDurationNanos / NANOS_PER_MILLISECOND,
            )
            else -> CameraXVideoFinalizationResult.Rejected("Session ${session.id.value} could not enter review: $reviewing")
        }
    }

    fun finalize(
        session: VideoStagingSession,
        event: VideoRecordEvent.Finalize,
    ): CameraXVideoFinalizationResult = finalize(
        session = session,
        finalization = VideoRecordingFinalization(
            recordedDurationNanos = event.recordingStats.recordedDurationNanos,
            errorCode = event.error.takeIf { event.hasError() },
        ),
    )

    fun listener(
        session: VideoStagingSession,
        onFinalized: (CameraXVideoFinalizationResult) -> Unit,
    ): Consumer<VideoRecordEvent> = Consumer { event ->
        if (event is VideoRecordEvent.Finalize) onFinalized(finalize(session, event))
    }

    private companion object {
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
