package org.sakos.camera.capture.video

import org.sakos.camera.safety.core.SafetyDecision
import org.sakos.camera.safety.core.SafetyFailureReason
import org.sakos.camera.safety.opennsfw2.IntegratedStillGatePolicyConstants
import org.sakos.camera.safety.opennsfw2.IntegratedStillGatePolicyDefaults
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive

/** The evidence role reported by the model-specific evaluator for one sampled frame. */
enum class VideoFrameEvidenceKind {
    Context,
    Crop,
    CorroboratedDetection,
    NearFloorLateral,
    IsolatedExtreme,
}

/** A frame result accepted by the temporal aggregator. Scores are model-policy evidence in 0..1. */
sealed interface VideoFrameEvaluation {
    data class Decision(
        val decision: SafetyDecision,
        val riskScore: Float,
        val evidenceKind: VideoFrameEvidenceKind,
    ) : VideoFrameEvaluation {
        init {
            require(riskScore.isFinite() && riskScore in 0f..1f) {
                "risk score must be finite and in 0..1."
            }
        }
    }

    data class Failure(
        val reason: SafetyFailureReason,
        val detail: String? = null,
    ) : VideoFrameEvaluation
}

/** A decoded frame whose owner releases its resources when [close] is called. */
interface VideoDecodedFrame<out Frame : Any> : AutoCloseable {
    val value: Frame
}

/** Android decoding belongs behind this boundary; this module owns closing the returned frame. */
interface VideoFrameDecoder<Frame : Any> : AutoCloseable {
    suspend fun decode(sample: VideoGateSamplePlan): VideoDecodedFrame<Frame>
}

/** Model-specific frame review belongs behind this boundary. */
fun interface VideoFrameEvaluator<Frame : Any> {
    suspend fun evaluate(frame: Frame, sample: VideoGateSamplePlan): VideoFrameEvaluation
}

data class VideoTemporalReviewSample(
    val samplePlan: VideoGateSamplePlan,
    val evaluation: VideoFrameEvaluation,
)

enum class VideoTemporalReviewDecision {
    Allow,
    Block,
    Review,
    Failure,
}

data class VideoTemporalReviewResult(
    val decision: VideoTemporalReviewDecision,
    val durationMillis: Long,
    val samples: List<VideoTemporalReviewSample>,
    val detail: String? = null,
)

/**
 * Plans, decodes and aggregates a bounded temporal review.
 *
 * This preserves the source policy's isolated, uncorroborated, non-extreme final-block Allow
 * behavior. Decode and evaluator failures are terminal non-Allow results.
 */
class VideoTemporalReviewEngine(
    private val policy: IntegratedStillGatePolicyConstants = IntegratedStillGatePolicyDefaults.fallback,
) {
    suspend fun <Frame : Any> review(
        durationMillis: Long,
        decoder: VideoFrameDecoder<Frame>,
        evaluator: VideoFrameEvaluator<Frame>,
    ): VideoTemporalReviewResult {
        // Resource-close failures must also produce a non-Allow result.
        return try {
        decoder.use {
        coroutineContext.ensureActive()
        val plan = VideoTemporalSamplePlanner.buildBasePlan(durationMillis)
        if (!plan.supportsNormalAllow) {
            return VideoTemporalReviewResult(
                decision = VideoTemporalReviewDecision.Review,
                durationMillis = plan.durationMillis,
                samples = emptyList(),
                detail = plan.unsupportedReason,
            )
        }

        val baseSamples = plan.baseSamples.map { evaluateSample(it, decoder, evaluator) }
        if (baseSamples.any { it.evaluation is VideoFrameEvaluation.Failure }) {
            return failure(plan.durationMillis, baseSamples)
        }

        val suspicious = selectSuspiciousBaseSamples(baseSamples)
        val selectedSeeds = suspicious.take(VideoGatePolicy.maxEscalatedTimestamps)
        val unresolvedHighRiskCount = suspicious.drop(selectedSeeds.size).count(::isHighRiskForUnresolvedHold)
        val escalationPlan = VideoTemporalSamplePlanner.buildEscalationPlan(
            seedSamples = selectedSeeds.map { it.samplePlan },
            durationMillis = plan.durationMillis,
            existingDecodedCount = baseSamples.size,
        )
        val samples = baseSamples + escalationPlan.map { evaluateSample(it, decoder, evaluator) }
        if (samples.any { it.evaluation is VideoFrameEvaluation.Failure }) {
            failure(plan.durationMillis, samples)
        } else {
            aggregate(plan.durationMillis, samples, unresolvedHighRiskCount)
        }
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        VideoTemporalReviewResult(
            decision = VideoTemporalReviewDecision.Failure,
            durationMillis = durationMillis.coerceAtLeast(0L),
            samples = emptyList(),
            detail = "Temporal video review could not finish: ${error.message ?: error.javaClass.simpleName}.",
        )
    }
    }

    private suspend fun <Frame : Any> evaluateSample(
        sample: VideoGateSamplePlan,
        decoder: VideoFrameDecoder<Frame>,
        evaluator: VideoFrameEvaluator<Frame>,
    ): VideoTemporalReviewSample = try {
        coroutineContext.ensureActive()
        decoder.decode(sample).use { decoded ->
            val evaluation = evaluator.evaluate(decoded.value, sample)
            coroutineContext.ensureActive()
            VideoTemporalReviewSample(sample, evaluation)
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        VideoTemporalReviewSample(
            sample,
            VideoFrameEvaluation.Failure(
                reason = SafetyFailureReason.InferenceFailure,
                detail = "Frame at ${sample.sampledAtMillis}ms could not be decoded or evaluated: ${error.message ?: error.javaClass.simpleName}.",
            ),
        )
    }

    private fun selectSuspiciousBaseSamples(samples: List<VideoTemporalReviewSample>): List<VideoTemporalReviewSample> {
        val elevated = samples.filter {
            ((it.evaluation as? VideoFrameEvaluation.Decision)?.riskScore ?: 0f) >= policy.elevatedDetectionFloor
        }
        val direct = samples.filter { sample ->
            val evaluation = sample.evaluation as VideoFrameEvaluation.Decision
            evaluation.decision != SafetyDecision.Allow || evaluation.riskScore >= policy.stage2AllowMax
        }
        val clusteredElevated = if (elevated.size >= 2) elevated else emptyList()
        return (direct + clusteredElevated)
            .distinctBy { it.samplePlan.sampledAtMillis }
            .sortedByDescending { (it.evaluation as VideoFrameEvaluation.Decision).riskScore }
    }

    private fun isHighRiskForUnresolvedHold(sample: VideoTemporalReviewSample): Boolean {
        val evaluation = sample.evaluation as VideoFrameEvaluation.Decision
        return evaluation.decision != SafetyDecision.Allow ||
            evaluation.riskScore >= policy.rawNsfwBlockFloor - policy.nearFloorLateralGap
    }

    private fun aggregate(
        durationMillis: Long,
        samples: List<VideoTemporalReviewSample>,
        unresolvedHighRiskCount: Int,
    ): VideoTemporalReviewResult {
        val decisions = samples.map { it to (it.evaluation as VideoFrameEvaluation.Decision) }
        val reviewCount = decisions.count { (_, evaluation) -> evaluation.decision == SafetyDecision.Review }
        val finalBlockSamples = decisions.filter { (sample, evaluation) ->
            evaluation.decision == SafetyDecision.Block &&
                (sample.samplePlan.pass == VideoGateSamplePass.Escalation || evaluation.evidenceKind == VideoFrameEvidenceKind.Context)
        }
        val blockedTimestamps = finalBlockSamples.map { it.first.samplePlan.sampledAtMillis }.distinct()
        val corroboratedClipBlock = blockedTimestamps.size >= 2
        fun isExtreme(pair: Pair<VideoTemporalReviewSample, VideoFrameEvaluation.Decision>) =
            pair.second.riskScore >= policy.extremeDetectionFloor
        fun hasTemporalSupport(pair: Pair<VideoTemporalReviewSample, VideoFrameEvaluation.Decision>) = decisions.any { (sample, evaluation) ->
            sample.samplePlan.sampledAtMillis != pair.first.samplePlan.sampledAtMillis &&
                evaluation.riskScore >= policy.elevatedDetectionFloor
        }

        val contextBlockSamples = finalBlockSamples.filter { it.second.evidenceKind == VideoFrameEvidenceKind.Context }
        val contextBlock = contextBlockSamples.any(::isExtreme) || contextBlockSamples.any(::hasTemporalSupport)
        val cropOnlyExtremeBlock = finalBlockSamples.any { pair ->
            pair.second.evidenceKind != VideoFrameEvidenceKind.Context && isExtreme(pair) &&
                (!VideoGatePolicy.cropOnlyExtremeRequiresTemporalCorroboration || hasTemporalSupport(pair))
        }
        val unresolvedBaseCropBlock = decisions.any { (sample, evaluation) ->
            sample.samplePlan.pass == VideoGateSamplePass.Base &&
                evaluation.decision == SafetyDecision.Block &&
                evaluation.evidenceKind != VideoFrameEvidenceKind.Context &&
                decisions.none { (candidate, _) ->
                    candidate.samplePlan.pass == VideoGateSamplePass.Escalation &&
                        candidate.samplePlan.sampledAtMillis == sample.samplePlan.sampledAtMillis
                }
        }
        val singleUncorroboratedNonExtremeBlockAllow = finalBlockSamples.isNotEmpty() &&
            blockedTimestamps.size == 1 &&
            finalBlockSamples.none(::isExtreme) &&
            finalBlockSamples.none(::hasTemporalSupport) &&
            reviewCount == 0 &&
            unresolvedHighRiskCount == 0 &&
            !unresolvedBaseCropBlock
        val unresolvedFinalCropBlock = finalBlockSamples.any { it.second.evidenceKind != VideoFrameEvidenceKind.Context } &&
            !corroboratedClipBlock && !cropOnlyExtremeBlock && !singleUncorroboratedNonExtremeBlockAllow

        val decision = when {
            contextBlock || corroboratedClipBlock || cropOnlyExtremeBlock -> VideoTemporalReviewDecision.Block
            reviewCount > 0 || unresolvedHighRiskCount > 0 || unresolvedBaseCropBlock || unresolvedFinalCropBlock -> VideoTemporalReviewDecision.Review
            else -> VideoTemporalReviewDecision.Allow
        }
        return VideoTemporalReviewResult(decision, durationMillis, samples)
    }

    private fun failure(durationMillis: Long, samples: List<VideoTemporalReviewSample>) = VideoTemporalReviewResult(
        decision = VideoTemporalReviewDecision.Failure,
        durationMillis = durationMillis,
        samples = samples,
        detail = "Temporal video safety review had a frame decode or evaluation failure.",
    )
}
