package org.sakos.camera.capture.video

import java.util.UUID

/** A stable identifier for one private staged recording and its sidecars. */
@JvmInline
value class VideoStagingSessionId(val value: String) {
    init {
        require(value.isNotBlank()) { "staging session ID must not be blank." }
        require(value == value.trim()) { "staging session ID must not have surrounding whitespace." }
        require(SESSION_ID.matches(value)) { "staging session ID contains unsafe path characters." }
    }

    private companion object {
        val SESSION_ID = Regex("[A-Za-z0-9][A-Za-z0-9_-]{0,127}")
    }
}

enum class VideoStagingState {
    Recording,
    Reviewing,
    Promoting,
    Completed,
    CleanupFailed,
}

data class VideoStagingSession(
    val id: VideoStagingSessionId,
    val state: VideoStagingState,
    val createdAtEpochMillis: Long,
    val cleanupFailureDetail: String? = null,
) {
    init {
        require(createdAtEpochMillis >= 0L) { "staging session timestamp must not be negative." }
        require((state == VideoStagingState.CleanupFailed) == (cleanupFailureDetail != null)) {
            "only CleanupFailed sessions may contain a cleanup failure detail."
        }
    }
}

/**
 * Boundary for app-private staging storage. [deleteStagedContent] must remove the clip and every
 * session-owned sidecar; callers must not map this boundary to a public media provider.
 */
interface VideoPrivateStagingStore {
    fun sessions(): List<VideoStagingSession>
    fun writeSession(session: VideoStagingSession)
    fun deleteStagedContent(id: VideoStagingSessionId)
    fun removeSession(id: VideoStagingSessionId)
}

/** A private store that can provide the CameraX output path for a staged recording. */
interface VideoPrivateStagingFileStore : VideoPrivateStagingStore {
    fun recordingOutputFile(id: VideoStagingSessionId): java.io.File
}

sealed interface VideoRecordingStartResult {
    data class Started(val session: VideoStagingSession) : VideoRecordingStartResult
    data class BlockedByCleanup(val sessionIds: List<VideoStagingSessionId>) : VideoRecordingStartResult
}

sealed interface VideoStagingTransitionResult {
    data class Updated(val session: VideoStagingSession) : VideoStagingTransitionResult
    data class Rejected(val detail: String) : VideoStagingTransitionResult
    data class CleanupFailed(val session: VideoStagingSession, val detail: String) : VideoStagingTransitionResult
    data object Removed : VideoStagingTransitionResult
}

data class VideoStagingRecoveryReport(
    val removedSessionIds: List<VideoStagingSessionId>,
    val cleanupFailedSessionIds: List<VideoStagingSessionId>,
)

/**
 * Enforces the durable staging state transitions before CameraX or actual files are connected.
 * A cleanup failure remains durable and blocks new recordings until [retryCleanup] succeeds.
 */
class VideoStagingSessionManager(
    private val store: VideoPrivateStagingStore,
    private val nextId: () -> VideoStagingSessionId = { VideoStagingSessionId(UUID.randomUUID().toString()) },
) {
    @Synchronized
    fun startRecording(createdAtEpochMillis: Long): VideoRecordingStartResult {
        val unresolvedCleanup = store.sessions()
            .filter { it.state != VideoStagingState.Completed }
            .map { it.id }
        if (unresolvedCleanup.isNotEmpty()) {
            return VideoRecordingStartResult.BlockedByCleanup(unresolvedCleanup)
        }
        val session = VideoStagingSession(nextId(), VideoStagingState.Recording, createdAtEpochMillis)
        store.writeSession(session)
        return VideoRecordingStartResult.Started(session)
    }

    @Synchronized
    fun markReviewing(id: VideoStagingSessionId): VideoStagingTransitionResult =
        transition(id, VideoStagingState.Recording, VideoStagingState.Reviewing)

    /** Returns the durable Reviewing session without changing its state. */
    @Synchronized
    fun requireReviewing(id: VideoStagingSessionId): VideoStagingTransitionResult =
        requireState(id, VideoStagingState.Reviewing)

    @Synchronized
    fun beginPromotion(id: VideoStagingSessionId): VideoStagingTransitionResult =
        transition(id, VideoStagingState.Reviewing, VideoStagingState.Promoting)

    /**
     * Call only after the owner has promoted an approved output. It removes private staging before
     * recording the completed state, so a cleanup failure remains visible and blocks new captures.
     */
    @Synchronized
    fun completePromotion(id: VideoStagingSessionId): VideoStagingTransitionResult {
        val session = find(id) ?: return VideoStagingTransitionResult.Rejected("Unknown staging session ${id.value}.")
        if (session.state != VideoStagingState.Promoting) {
            return VideoStagingTransitionResult.Rejected("Session ${id.value} cannot complete from ${session.state}.")
        }
        return try {
            store.deleteStagedContent(id)
            val completed = session.copy(state = VideoStagingState.Completed)
            store.writeSession(completed)
            VideoStagingTransitionResult.Updated(completed)
        } catch (error: Exception) {
            recordCleanupFailure(session, error)
        }
    }

    /** Removes a non-approved or abandoned session's staged clip, sidecars, and metadata. */
    @Synchronized
    fun discard(id: VideoStagingSessionId): VideoStagingTransitionResult {
        val session = find(id) ?: return VideoStagingTransitionResult.Removed
        if (session.state == VideoStagingState.Completed) {
            return VideoStagingTransitionResult.Rejected("Completed session ${id.value} has no staged content to discard.")
        }
        return try {
            store.deleteStagedContent(id)
            store.removeSession(id)
            VideoStagingTransitionResult.Removed
        } catch (error: Exception) {
            recordCleanupFailure(session, error)
        }
    }

    @Synchronized
    fun retryCleanup(id: VideoStagingSessionId): VideoStagingTransitionResult {
        val session = find(id) ?: return VideoStagingTransitionResult.Removed
        if (session.state != VideoStagingState.CleanupFailed) {
            return VideoStagingTransitionResult.Rejected("Session ${id.value} is not awaiting cleanup.")
        }
        return discard(id)
    }

    /** Purges every non-completed session discovered at startup; completed output remains untouched. */
    @Synchronized
    fun recoverAbandonedSessions(): VideoStagingRecoveryReport {
        val results = store.sessions()
            .filter { it.state != VideoStagingState.Completed }
            .map { it.id to discard(it.id) }
        return VideoStagingRecoveryReport(
            removedSessionIds = results.mapNotNull { (id, result) -> if (result is VideoStagingTransitionResult.Removed) id else null },
            cleanupFailedSessionIds = results.mapNotNull { (id, result) ->
                if (result is VideoStagingTransitionResult.CleanupFailed) id else null
            },
        )
    }

    private fun transition(
        id: VideoStagingSessionId,
        expected: VideoStagingState,
        next: VideoStagingState,
    ): VideoStagingTransitionResult {
        val session = find(id) ?: return VideoStagingTransitionResult.Rejected("Unknown staging session ${id.value}.")
        if (session.state != expected) {
            return VideoStagingTransitionResult.Rejected("Session ${id.value} cannot move from ${session.state} to $next.")
        }
        val updated = session.copy(state = next)
        store.writeSession(updated)
        return VideoStagingTransitionResult.Updated(updated)
    }

    private fun requireState(
        id: VideoStagingSessionId,
        expected: VideoStagingState,
    ): VideoStagingTransitionResult {
        val session = find(id) ?: return VideoStagingTransitionResult.Rejected("Unknown staging session ${id.value}.")
        if (session.state != expected) {
            return VideoStagingTransitionResult.Rejected("Session ${id.value} is not $expected.")
        }
        return VideoStagingTransitionResult.Updated(session)
    }

    private fun find(id: VideoStagingSessionId): VideoStagingSession? = store.sessions().singleOrNull { it.id == id }

    private fun recordCleanupFailure(
        session: VideoStagingSession,
        error: Exception,
    ): VideoStagingTransitionResult.CleanupFailed {
        val detail = error.message ?: error.javaClass.simpleName
        val failed = session.copy(state = VideoStagingState.CleanupFailed, cleanupFailureDetail = detail)
        store.writeSession(failed)
        return VideoStagingTransitionResult.CleanupFailed(failed, detail)
    }
}
