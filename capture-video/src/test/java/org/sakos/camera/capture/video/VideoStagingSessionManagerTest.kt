package org.sakos.camera.capture.video

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class VideoStagingSessionManagerTest {
    @Test
    fun nonApprovedDiscardRemovesClipSidecarsAndMetadata() {
        val store = FakeStore()
        val manager = manager(store)
        val session = assertIs<VideoRecordingStartResult.Started>(manager.startRecording(1L)).session

        val result = manager.discard(session.id)

        assertIs<VideoStagingTransitionResult.Removed>(result)
        assertEquals(listOf(session.id), store.deletedContent)
        assertTrue(store.sessions().isEmpty())
    }

    @Test
    fun duplicateDiscardIsIdempotent() {
        val store = FakeStore()
        val manager = manager(store)
        val session = assertIs<VideoRecordingStartResult.Started>(manager.startRecording(1L)).session

        assertIs<VideoStagingTransitionResult.Removed>(manager.discard(session.id))
        assertIs<VideoStagingTransitionResult.Removed>(manager.discard(session.id))

        assertEquals(listOf(session.id), store.deletedContent)
    }

    @Test
    fun cleanupFailureBlocksNewRecordingUntilRetrySucceeds() {
        val store = FakeStore(failDelete = true)
        val manager = manager(store)
        val session = assertIs<VideoRecordingStartResult.Started>(manager.startRecording(1L)).session

        assertIs<VideoStagingTransitionResult.CleanupFailed>(manager.discard(session.id))
        assertIs<VideoRecordingStartResult.BlockedByCleanup>(manager.startRecording(2L))

        store.failDelete = false
        assertIs<VideoStagingTransitionResult.Removed>(manager.retryCleanup(session.id))
        assertIs<VideoRecordingStartResult.Started>(manager.startRecording(3L))
    }

    @Test
    fun outOfOrderEventsAreRejectedWithoutChangingState() {
        val store = FakeStore()
        val manager = manager(store)
        val session = assertIs<VideoRecordingStartResult.Started>(manager.startRecording(1L)).session

        assertIs<VideoStagingTransitionResult.Rejected>(manager.beginPromotion(session.id))
        assertEquals(VideoStagingState.Recording, store.sessions().single().state)
        assertIs<VideoStagingTransitionResult.Updated>(manager.markReviewing(session.id))
        assertIs<VideoStagingTransitionResult.Rejected>(manager.markReviewing(session.id))
        assertEquals(VideoStagingState.Reviewing, store.sessions().single().state)
    }

    @Test
    fun completedPromotionDeletesStagingButPreservesCompletionRecord() {
        val store = FakeStore()
        val manager = manager(store)
        val session = assertIs<VideoRecordingStartResult.Started>(manager.startRecording(1L)).session
        manager.markReviewing(session.id)
        manager.beginPromotion(session.id)

        val result = assertIs<VideoStagingTransitionResult.Updated>(manager.completePromotion(session.id))

        assertEquals(VideoStagingState.Completed, result.session.state)
        assertEquals(listOf(session.id), store.deletedContent)
        assertEquals(VideoStagingState.Completed, store.sessions().single().state)
    }

    @Test
    fun recoveryPurgesAbandonedSessionsAndPreservesCompletedSession() {
        val abandoned = VideoStagingSession(VideoStagingSessionId("abandoned"), VideoStagingState.Reviewing, 1L)
        val completed = VideoStagingSession(VideoStagingSessionId("completed"), VideoStagingState.Completed, 2L)
        val store = FakeStore(initial = listOf(abandoned, completed))

        val report = manager(store).recoverAbandonedSessions()

        assertEquals(listOf(abandoned.id), report.removedSessionIds)
        assertTrue(report.cleanupFailedSessionIds.isEmpty())
        assertEquals(listOf(completed), store.sessions())
        assertEquals(listOf(abandoned.id), store.deletedContent)
    }

    @Test
    fun recoveryKeepsFailureDurableWhenDeletionFails() {
        val abandoned = VideoStagingSession(VideoStagingSessionId("abandoned"), VideoStagingState.Recording, 1L)
        val store = FakeStore(initial = listOf(abandoned), failDelete = true)

        val report = manager(store).recoverAbandonedSessions()

        assertEquals(listOf(abandoned.id), report.cleanupFailedSessionIds)
        assertEquals(VideoStagingState.CleanupFailed, store.sessions().single().state)
    }

    private fun manager(store: FakeStore): VideoStagingSessionManager {
        var index = 0
        return VideoStagingSessionManager(store) { VideoStagingSessionId("session-${++index}") }
    }

    private class FakeStore(
        initial: List<VideoStagingSession> = emptyList(),
        var failDelete: Boolean = false,
    ) : VideoPrivateStagingStore {
        private val records = initial.associateByTo(linkedMapOf()) { it.id }
        val deletedContent = mutableListOf<VideoStagingSessionId>()

        override fun sessions(): List<VideoStagingSession> = records.values.toList()

        override fun writeSession(session: VideoStagingSession) {
            records[session.id] = session
        }

        override fun deleteStagedContent(id: VideoStagingSessionId) {
            if (failDelete) error("synthetic delete failure")
            deletedContent += id
        }

        override fun removeSession(id: VideoStagingSessionId) {
            records.remove(id)
        }
    }
}
