package org.sakos.camera.capture.video

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AndroidVideoPrivateStagingStoreTest {
    private val root = createTempDirectory("sakos-video-staging-").toFile()

    @AfterTest
    fun removeTemporaryRoot() {
        root.deleteRecursively()
    }

    @Test
    fun persistsSessionAndKeepsRecordingOutputUnderSessionDirectory() {
        val store = AndroidVideoPrivateStagingStore(root)
        val session = recordingSession("session-1")

        store.writeSession(session)
        val output = store.recordingOutputFile(session.id)

        assertEquals(listOf(session), AndroidVideoPrivateStagingStore(root).sessions())
        assertTrue(output.path.startsWith(root.path))
        assertEquals("recording.mp4", output.name)
        assertTrue(requireNotNull(output.parentFile).isDirectory)
    }

    @Test
    fun reloadsBackupMetadataWhenReplacementWasInterrupted() {
        val store = AndroidVideoPrivateStagingStore(root)
        val session = recordingSession("session-backup")
        store.writeSession(session)
        val metadata = File(root, "metadata/${session.id.value}.properties")
        val backup = File(root, "metadata/${session.id.value}.properties.bak")
        assertTrue(metadata.renameTo(backup))

        assertEquals(listOf(session), AndroidVideoPrivateStagingStore(root).sessions())
    }

    @Test
    fun deleteStagedContentRemovesRecordingAndAllSessionSidecars() {
        val store = AndroidVideoPrivateStagingStore(root)
        val session = recordingSession("session-2")
        store.writeSession(session)
        val output = store.recordingOutputFile(session.id)
        output.writeBytes(byteArrayOf(1, 2, 3))
        File(requireNotNull(output.parentFile), "decoder-sidecar.json").writeText("{}")

        store.deleteStagedContent(session.id)

        assertFalse(requireNotNull(output.parentFile).exists())
        assertEquals(listOf(session), store.sessions())
    }

    @Test
    fun orphanContentBlocksStartUntilRecoveryDeletesIt() {
        val store = AndroidVideoPrivateStagingStore(root)
        val id = VideoStagingSessionId("orphan")
        store.recordingOutputFile(id).writeBytes(byteArrayOf(1))
        val manager = VideoStagingSessionManager(store) { VideoStagingSessionId("next") }

        val blocked = assertIs<VideoRecordingStartResult.BlockedByCleanup>(manager.startRecording(5L))
        assertEquals(listOf(id), blocked.sessionIds)
        assertIs<VideoStagingTransitionResult.Removed>(manager.retryCleanup(id))
        assertIs<VideoRecordingStartResult.Started>(manager.startRecording(6L))
    }

    @Test
    fun malformedMetadataBlocksStartUntilRecoveryDeletesIt() {
        val store = AndroidVideoPrivateStagingStore(root)
        val id = VideoStagingSessionId("malformed")
        store.recordingOutputFile(id).writeBytes(byteArrayOf(1))
        File(root, "metadata/${id.value}.properties").apply {
            requireNotNull(parentFile).mkdirs()
            writeText("not=valid\nstate=not-a-state")
        }
        val manager = VideoStagingSessionManager(store) { VideoStagingSessionId("next") }

        assertIs<VideoRecordingStartResult.BlockedByCleanup>(manager.startRecording(5L))
        assertIs<VideoStagingTransitionResult.Removed>(manager.retryCleanup(id))
        assertTrue(store.sessions().isEmpty())
    }

    @Test
    fun traversalLikeSessionIdsAreRejected() {
        assertFailsWith<IllegalArgumentException> { VideoStagingSessionId("../outside") }
        assertFailsWith<IllegalArgumentException> { VideoStagingSessionId("has/slash") }
    }

    private fun recordingSession(value: String) = VideoStagingSession(
        id = VideoStagingSessionId(value),
        state = VideoStagingState.Recording,
        createdAtEpochMillis = 1L,
    )
}
