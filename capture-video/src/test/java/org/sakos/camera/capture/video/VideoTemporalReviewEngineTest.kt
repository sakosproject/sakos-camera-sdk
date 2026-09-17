package org.sakos.camera.capture.video

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.sakos.camera.safety.core.SafetyDecision

class VideoTemporalReviewEngineTest {
    @Test
    fun clearFramesAllowAndReleaseEveryFrameAndDecoder() = runSuspend {
        val decoder = TestDecoder()
        val result = VideoTemporalReviewEngine().review(
            durationMillis = 30_000L,
            decoder = decoder,
            evaluator = VideoFrameEvaluator { _, _ -> decision(SafetyDecision.Allow, 0.1f) },
        )

        assertEquals(VideoTemporalReviewDecision.Allow, result.decision)
        assertEquals(7, result.samples.size)
        assertTrue(decoder.frames.all { it.closed })
        assertTrue(decoder.closed)
    }

    @Test
    fun unsupportedDurationReturnsReviewWithoutDecoding() = runSuspend {
        val decoder = TestDecoder()
        val result = VideoTemporalReviewEngine().review(
            durationMillis = VideoGatePolicy.maxNormalDurationMillis + 1L,
            decoder = decoder,
            evaluator = VideoFrameEvaluator { _, _ -> decision(SafetyDecision.Allow, 0.1f) },
        )

        assertEquals(VideoTemporalReviewDecision.Review, result.decision)
        assertTrue(result.samples.isEmpty())
        assertTrue(decoder.closed)
    }

    @Test
    fun decodeFailureIsTerminalAndNonAllow() = runSuspend {
        val decoder = TestDecoder(failAtMillis = 500L)
        val result = VideoTemporalReviewEngine().review(
            durationMillis = 1_000L,
            decoder = decoder,
            evaluator = VideoFrameEvaluator { _, _ -> decision(SafetyDecision.Allow, 0.1f) },
        )

        assertEquals(VideoTemporalReviewDecision.Failure, result.decision)
        assertTrue(decoder.closed)
    }

    @Test
    fun contextExtremeBlocks() = runSuspend {
        val result = reviewWith { _, _ -> decision(SafetyDecision.Block, 0.9f, VideoFrameEvidenceKind.Context) }

        assertEquals(VideoTemporalReviewDecision.Block, result.decision)
    }

    @Test
    fun cropExtremeNeedsTemporalSupportAndThenBlocks() = runSuspend {
        val result = reviewWith { _, sample ->
            if (sample.sampledAtMillis == 500L) {
                decision(SafetyDecision.Block, 0.9f)
            } else {
                decision(SafetyDecision.Allow, 0.3f)
            }
        }

        assertEquals(VideoTemporalReviewDecision.Block, result.decision)
    }

    @Test
    fun singleUncorroboratedNonExtremeFinalBlockAllowsLikeSourcePolicy() = runSuspend {
        val result = reviewWith { _, sample ->
            if (sample.sampledAtMillis == 500L) decision(SafetyDecision.Block, 0.5f) else decision(SafetyDecision.Allow, 0.1f)
        }

        assertEquals(VideoTemporalReviewDecision.Allow, result.decision)
        assertEquals(4, result.samples.size)
    }

    @Test
    fun reviewOutcomeCannotAllow() = runSuspend {
        val result = reviewWith { _, _ -> decision(SafetyDecision.Review, 0.5f) }

        assertEquals(VideoTemporalReviewDecision.Review, result.decision)
    }

    @Test
    fun totalDecodedSamplesStayWithinPolicyCap() = runSuspend {
        val result = reviewWith(durationMillis = VideoGatePolicy.maxNormalDurationMillis) { _, _ ->
            decision(SafetyDecision.Allow, 0.31f)
        }

        assertTrue(result.samples.size <= VideoGatePolicy.maxDecodedSamples)
    }

    private suspend fun reviewWith(
        durationMillis: Long = 1_000L,
        block: suspend (Int, VideoGateSamplePlan) -> VideoFrameEvaluation,
    ): VideoTemporalReviewResult = VideoTemporalReviewEngine().review(
        durationMillis = durationMillis,
        decoder = TestDecoder(),
        evaluator = VideoFrameEvaluator(block),
    )

    private fun decision(
        decision: SafetyDecision,
        score: Float,
        evidence: VideoFrameEvidenceKind = VideoFrameEvidenceKind.Crop,
    ) = VideoFrameEvaluation.Decision(decision, score, evidence)

    private class TestDecoder(
        private val failAtMillis: Long? = null,
    ) : VideoFrameDecoder<Int> {
        val frames = mutableListOf<TestFrame>()
        var closed = false

        override suspend fun decode(sample: VideoGateSamplePlan): VideoDecodedFrame<Int> {
            check(sample.sampledAtMillis != failAtMillis) { "synthetic decode failure" }
            return TestFrame(sample.sampledAtMillis.toInt()).also(frames::add)
        }

        override fun close() {
            closed = true
        }
    }

    private class TestFrame(
        override val value: Int,
    ) : VideoDecodedFrame<Int> {
        var closed = false

        override fun close() {
            closed = true
        }
    }
}

private fun <T> runSuspend(block: suspend () -> T): T {
    var result: Result<T>? = null
    block.startCoroutine(object : Continuation<T> {
        override val context = EmptyCoroutineContext

        override fun resumeWith(resumeResult: Result<T>) {
            result = resumeResult
        }
    })
    return requireNotNull(result).getOrThrow()
}
