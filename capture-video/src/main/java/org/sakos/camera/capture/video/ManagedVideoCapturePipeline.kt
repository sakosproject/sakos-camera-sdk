package org.sakos.camera.capture.video

import androidx.camera.video.Recording

/** The narrow recording lifecycle boundary used before a staged session is discarded. */
interface VideoRecordingHandle : AutoCloseable {
    fun stop()
}

/** A CameraX adapter with no output-path, file-store, or promotion responsibility. */
class CameraXVideoRecordingHandle(
    private val recording: Recording,
) : VideoRecordingHandle {
    override fun stop() {
        recording.stop()
    }

    override fun close() {
        recording.close()
    }
}

/** Caller-owned promotion of an approved private staged clip. */
fun interface ApprovedVideoPromoter {
    fun promote(session: VideoStagingSession)
}

sealed interface ManagedVideoReviewResult {
    data class Promoted(val review: VideoTemporalReviewResult) : ManagedVideoReviewResult
    data class NotPromoted(
        val review: VideoTemporalReviewResult,
        val cleanup: VideoStagingTransitionResult,
    ) : ManagedVideoReviewResult
    data class Rejected(val detail: String) : ManagedVideoReviewResult
}

data class ManagedVideoStartResult(
    val recovery: VideoStagingRecoveryReport,
    val start: VideoRecordingStartResult,
)

/**
 * Connects temporal review to private staging. Only a temporal Allow reaches [ApprovedVideoPromoter].
 * The promoter owns the approved destination; this pipeline never maps staged media to a public path.
 */
class ManagedVideoCapturePipeline(
    private val sessions: VideoStagingSessionManager,
    private val temporalReview: VideoTemporalReviewEngine = VideoTemporalReviewEngine(),
) {
    fun recoverThenStart(createdAtEpochMillis: Long): ManagedVideoStartResult = ManagedVideoStartResult(
        recovery = sessions.recoverAbandonedSessions(),
        start = sessions.startRecording(createdAtEpochMillis),
    )

    suspend fun <Frame : Any> reviewFinalized(
        session: VideoStagingSession,
        durationMillis: Long,
        decoder: VideoFrameDecoder<Frame>,
        evaluator: VideoFrameEvaluator<Frame>,
        promoter: ApprovedVideoPromoter,
    ): ManagedVideoReviewResult {
        val reviewing = sessions.markReviewing(session.id)
        if (reviewing !is VideoStagingTransitionResult.Updated) {
            return ManagedVideoReviewResult.Rejected("Session ${session.id.value} could not enter review: $reviewing")
        }
        val review = temporalReview.review(durationMillis, decoder, evaluator)
        if (review.decision != VideoTemporalReviewDecision.Allow) {
            return ManagedVideoReviewResult.NotPromoted(review, sessions.discard(session.id))
        }

        val promoting = sessions.beginPromotion(session.id)
        if (promoting !is VideoStagingTransitionResult.Updated) {
            return ManagedVideoReviewResult.NotPromoted(review, sessions.discard(session.id))
        }
        return try {
            promoter.promote(promoting.session)
            when (val completed = sessions.completePromotion(session.id)) {
                is VideoStagingTransitionResult.Updated -> ManagedVideoReviewResult.Promoted(review)
                else -> ManagedVideoReviewResult.NotPromoted(review, completed)
            }
        } catch (_: Exception) {
            ManagedVideoReviewResult.NotPromoted(review, sessions.discard(session.id))
        }
    }

    /** Stops and releases the recording before clearing its staged clip and sidecars. */
    fun abandonRecording(
        session: VideoStagingSession,
        recording: VideoRecordingHandle,
    ): VideoStagingTransitionResult {
        try {
            recording.stop()
        } finally {
            recording.close()
        }
        return sessions.discard(session.id)
    }
}
