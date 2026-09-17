package org.sakos.camera.capture.video

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.sakos.camera.safety.core.SafetyDecision

class CameraXVideoFinalizationBridgeTest {
    @Test
    fun cameraXErrorDiscardsPrivateStagingWithoutReview() {
        val store = FakeStore()
        val manager = manager(store)
        val session = start(manager)

        val result = CameraXVideoFinalizationBridge(manager).finalize(
            session,
            VideoRecordingFinalization(recordedDurationNanos = 1_000_000L, errorCode = 7),
        )

        assertIs<CameraXVideoFinalizationResult.Discarded>(result)
        assertEquals(listOf(session.id), store.deleted)
        assertTrue(store.sessions().isEmpty())
    }

    @Test
    fun cleanFinalizationMarksReviewingConvertsDurationAndAllowsPreparedReview() = runSuspend {
        val store = FakeStore()
        val manager = manager(store)
        val session = start(manager)
        val ready = assertIs<CameraXVideoFinalizationResult.ReadyForReview>(
            CameraXVideoFinalizationBridge(manager).finalize(session, VideoRecordingFinalization(2_500_000_000L)),
        )
        var promotions = 0

        val review = ManagedVideoCapturePipeline(manager).reviewPrepared(
            ready.session,
            ready.durationMillis,
            Decoder(),
            VideoFrameEvaluator { _, _ -> VideoFrameEvaluation.Decision(SafetyDecision.Allow, 0.1f, VideoFrameEvidenceKind.Crop) },
            ApprovedVideoPromoter { promotions += 1 },
        )

        assertEquals(2_500L, ready.durationMillis)
        assertIs<ManagedVideoReviewResult.Promoted>(review)
        assertEquals(1, promotions)
        assertEquals(VideoStagingState.Completed, store.sessions().single().state)
    }

    @Test
    fun duplicateCleanFinalizationIsRejectedBeforeReview() {
        val store = FakeStore()
        val manager = manager(store)
        val session = start(manager)
        val bridge = CameraXVideoFinalizationBridge(manager)

        assertIs<CameraXVideoFinalizationResult.ReadyForReview>(bridge.finalize(session, VideoRecordingFinalization(1L)))
        assertIs<CameraXVideoFinalizationResult.Rejected>(bridge.finalize(session, VideoRecordingFinalization(1L)))
        assertEquals(VideoStagingState.Reviewing, store.sessions().single().state)
    }

    private fun manager(store: FakeStore): VideoStagingSessionManager =
        VideoStagingSessionManager(store) { VideoStagingSessionId("session") }

    private fun start(manager: VideoStagingSessionManager): VideoStagingSession =
        assertIs<VideoRecordingStartResult.Started>(manager.startRecording(1L)).session

    private class Decoder : VideoFrameDecoder<Int> {
        override suspend fun decode(sample: VideoGateSamplePlan): VideoDecodedFrame<Int> = object : VideoDecodedFrame<Int> {
            override val value = sample.sampledAtMillis.toInt()
            override fun close() = Unit
        }

        override fun close() = Unit
    }

    private class FakeStore : VideoPrivateStagingStore {
        private val records = linkedMapOf<VideoStagingSessionId, VideoStagingSession>()
        val deleted = mutableListOf<VideoStagingSessionId>()

        override fun sessions(): List<VideoStagingSession> = records.values.toList()
        override fun writeSession(session: VideoStagingSession) { records[session.id] = session }
        override fun deleteStagedContent(id: VideoStagingSessionId) { deleted += id }
        override fun removeSession(id: VideoStagingSessionId) { records.remove(id) }
    }
}

private fun <T> runSuspend(block: suspend () -> T): T {
    var result: Result<T>? = null
    block.startCoroutine(object : Continuation<T> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(resumeResult: Result<T>) { result = resumeResult }
    })
    return requireNotNull(result).getOrThrow()
}
