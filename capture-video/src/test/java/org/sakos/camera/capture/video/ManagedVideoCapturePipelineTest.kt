package org.sakos.camera.capture.video

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.sakos.camera.safety.core.SafetyDecision

class ManagedVideoCapturePipelineTest {
    @Test
    fun allowPromotesOnceAndClearsPrivateStaging() = runManagedSuspend {
        val store = FakeStore()
        val session = start(store)
        var promotions = 0

        val result = pipeline(store).reviewFinalized(
            session, 1_000L, Decoder(), evaluator(SafetyDecision.Allow, 0.1f),
            ApprovedVideoPromoter { promotions += 1 },
        )

        assertIs<ManagedVideoReviewResult.Promoted>(result)
        assertEquals(1, promotions)
        assertEquals(listOf(session.id), store.deleted)
        assertEquals(VideoStagingState.Completed, store.sessions().single().state)
    }

    @Test
    fun blockNeverPromotesAndDiscardsStaging() = runManagedSuspend {
        val store = FakeStore()
        val session = start(store)
        var promotions = 0

        val result = pipeline(store).reviewFinalized(
            session, 1_000L, Decoder(), evaluator(SafetyDecision.Block, 0.9f, VideoFrameEvidenceKind.Context),
            ApprovedVideoPromoter { promotions += 1 },
        )

        assertIs<ManagedVideoReviewResult.NotPromoted>(result)
        assertEquals(0, promotions)
        assertTrue(store.sessions().isEmpty())
        assertTrue(store.deleted.contains(session.id))
    }

    @Test
    fun promotionFailureDiscardsStaging() = runManagedSuspend {
        val store = FakeStore()
        val session = start(store)

        val result = pipeline(store).reviewFinalized(
            session, 1_000L, Decoder(), evaluator(SafetyDecision.Allow, 0.1f),
            ApprovedVideoPromoter { error("synthetic promotion failure") },
        )

        assertIs<ManagedVideoReviewResult.NotPromoted>(result)
        assertTrue(store.sessions().isEmpty())
        assertEquals(listOf(session.id), store.deleted)
    }

    @Test
    fun recoveryRunsBeforeStartingNewRecording() {
        val abandoned = VideoStagingSession(VideoStagingSessionId("abandoned"), VideoStagingState.Recording, 1L)
        val store = FakeStore(listOf(abandoned))

        val result = pipeline(store).recoverThenStart(2L)

        assertEquals(listOf(abandoned.id), result.recovery.removedSessionIds)
        assertIs<VideoRecordingStartResult.Started>(result.start)
        assertEquals(listOf(abandoned.id), store.deleted)
    }

    @Test
    fun abandonmentStopsThenClosesBeforeStagingCleanup() {
        val events = mutableListOf<String>()
        val store = FakeStore(onDelete = { events += "delete" })
        val session = start(store)
        val handle = object : VideoRecordingHandle {
            override fun stop() { events += "stop" }
            override fun close() { events += "close" }
        }

        assertIs<VideoStagingTransitionResult.Removed>(pipeline(store).abandonRecording(session, handle))

        assertEquals(listOf("stop", "close", "delete"), events)
    }

    private fun pipeline(store: FakeStore): ManagedVideoCapturePipeline =
        ManagedVideoCapturePipeline(manager(store))

    private fun start(store: FakeStore): VideoStagingSession =
        assertIs<VideoRecordingStartResult.Started>(manager(store).startRecording(1L)).session

    private fun manager(store: FakeStore): VideoStagingSessionManager {
        var number = 0
        return VideoStagingSessionManager(store) { VideoStagingSessionId("session-${++number}") }
    }

    private fun evaluator(
        decision: SafetyDecision,
        score: Float,
        evidence: VideoFrameEvidenceKind = VideoFrameEvidenceKind.Crop,
    ) = VideoFrameEvaluator<Int> { _, _ -> VideoFrameEvaluation.Decision(decision, score, evidence) }

    private class Decoder : VideoFrameDecoder<Int> {
        override suspend fun decode(sample: VideoGateSamplePlan): VideoDecodedFrame<Int> = object : VideoDecodedFrame<Int> {
            override val value = sample.sampledAtMillis.toInt()
            override fun close() = Unit
        }

        override fun close() = Unit
    }

    private class FakeStore(
        initial: List<VideoStagingSession> = emptyList(),
        private val onDelete: () -> Unit = {},
    ) : VideoPrivateStagingStore {
        private val records = initial.associateByTo(linkedMapOf()) { it.id }
        val deleted = mutableListOf<VideoStagingSessionId>()

        override fun sessions(): List<VideoStagingSession> = records.values.toList()
        override fun writeSession(session: VideoStagingSession) { records[session.id] = session }
        override fun deleteStagedContent(id: VideoStagingSessionId) { deleted += id; onDelete() }
        override fun removeSession(id: VideoStagingSessionId) { records.remove(id) }
    }
}

private fun <T> runManagedSuspend(block: suspend () -> T): T {
    var result: Result<T>? = null
    block.startCoroutine(object : Continuation<T> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(resumeResult: Result<T>) { result = resumeResult }
    })
    return requireNotNull(result).getOrThrow()
}
