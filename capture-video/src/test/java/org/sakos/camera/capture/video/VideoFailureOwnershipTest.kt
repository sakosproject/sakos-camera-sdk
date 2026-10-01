package org.sakos.camera.capture.video

import kotlin.test.*
import kotlinx.coroutines.*
import org.sakos.camera.safety.core.SafetyDecision

class VideoFailureOwnershipTest {
    private class Store : VideoPrivateStagingStore {
        val records = linkedMapOf<VideoStagingSessionId, VideoStagingSession>()
        var deletes = 0
        override fun sessions() = records.values.toList()
        override fun writeSession(session: VideoStagingSession) { records[session.id] = session }
        override fun deleteStagedContent(id: VideoStagingSessionId) { deletes++ }
        override fun removeSession(id: VideoStagingSessionId) { records.remove(id) }
    }
    private class Decoder(private val failClose: Boolean = false) : VideoFrameDecoder<Int> {
        var closes = 0
        var frameCloses = 0
        override suspend fun decode(sample: VideoGateSamplePlan) = object : VideoDecodedFrame<Int> {
            override val value = 1
            override fun close() { frameCloses++ }
        }
        override fun close() { closes++; if (failClose) error("synthetic release failure") }
    }

    @Test fun decoderReleaseFailureCannotPromote() = runBlocking {
        val store = Store()
        val sessions = VideoStagingSessionManager(store)
        val session = assertIs<VideoRecordingStartResult.Started>(sessions.startRecording(0)).session
        val decoder = Decoder(true)
        var writes = 0
        val result = ManagedVideoCapturePipeline(sessions).reviewFinalized(session, 1000, decoder,
            VideoFrameEvaluator { _, _ -> VideoFrameEvaluation.Decision(SafetyDecision.Allow, .1f, VideoFrameEvidenceKind.Context) },
            ApprovedVideoPromoter { writes++ })
        assertIs<ManagedVideoReviewResult.NotPromoted>(result)
        assertEquals(0, writes)
        assertEquals(1, decoder.closes)
        assertTrue(decoder.frameCloses > 0)
        assertTrue(store.sessions().isEmpty())
    }

    @Test fun cancellationDiscardsAndClosesWithoutPromotion() = runBlocking {
        val store = Store()
        val sessions = VideoStagingSessionManager(store)
        val session = assertIs<VideoRecordingStartResult.Started>(sessions.startRecording(0)).session
        val decoder = Decoder()
        var writes = 0
        assertFailsWith<CancellationException> {
            ManagedVideoCapturePipeline(sessions).reviewFinalized(session, 1000, decoder,
                VideoFrameEvaluator { _, _ -> throw CancellationException("synthetic cancellation") }, ApprovedVideoPromoter { writes++ })
        }
        assertEquals(0, writes)
        assertEquals(1, decoder.closes)
        assertEquals(1, decoder.frameCloses)
        assertTrue(store.sessions().isEmpty())
    }

    @Test fun recordingReleaseFailureStillRequestsCleanup() {
        val store = Store()
        val sessions = VideoStagingSessionManager(store)
        val session = assertIs<VideoRecordingStartResult.Started>(sessions.startRecording(0)).session
        var closed = false
        assertFailsWith<IllegalStateException> {
            ManagedVideoCapturePipeline(sessions).abandonRecording(session, object : VideoRecordingHandle {
                override fun stop() = error("synthetic stop failure")
                override fun close() { closed = true }
            })
        }
        assertTrue(closed)
        assertEquals(1, store.deletes)
        assertTrue(store.sessions().isEmpty())
    }
}
