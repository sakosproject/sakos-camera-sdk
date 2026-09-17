/*
 * Selectively extracted from the user-authorized camera/gallery application source
 * at historical source revision omitted. See docs/EXTRACTION_MANIFEST.md.
 */
package org.sakos.camera.safety.opennsfw2

import java.util.Locale

data class SakosCompatibleModelScores(
    val sfwProbability: Float,
    val nsfwProbability: Float,
) {
    fun summaryLines(): List<String> = listOf(
        "SFW probability: ${formatScore(sfwProbability)}",
        "NSFW probability: ${formatScore(nsfwProbability)}",
    )
}

data class LiveSakosRuntimeEvaluation(
    val scores: SakosCompatibleModelScores,
    val checkResult: OpenNsfw2CheckResult,
    val inferenceMillis: Long,
    val sourceSize: String,
    val evaluationStrategy: String,
    val strategyId: String,
    val strategyDisplayName: String,
    val policyNsfwEvidence: Float,
    val policyTriggerLabel: String,
    val representativeViewLabel: String,
    val representativeViewBounds: String,
    val strongestViewLabel: String,
    val strongestViewBounds: String,
    val stageReached: String,
    val stageEvaluations: List<LiveSakosRuntimeStageEvaluation>,
    val supportiveDetectionCount: Int,
    val elevatedDetectionCount: Int,
    val evaluatedViews: List<LiveSakosRuntimeViewEvaluation>,
    val runtimeProfileId: String,
    val runtimeProfileDisplayName: String,
    val modelDisplayName: String,
    val executionMode: String,
    val samplingProfileId: String,
    val samplingProfileLabel: String,
    val samplingRatio: Float,
    val samplingOrientation: String,
    val samplingProfileSelectorEligible: Boolean,
) {
    fun summaryLines(): List<String> = scores.summaryLines() + listOf(
        "Runtime profile: $runtimeProfileDisplayName",
        "Runtime strategy: $strategyDisplayName",
        "Execution mode: $executionMode",
        "Model display name: $modelDisplayName",
        "Inference time: ${inferenceMillis} ms",
        "Source frame: $sourceSize",
        "Sampling profile: $samplingProfileLabel ($samplingProfileId), ratio=${formatScore(samplingRatio)}, orientation=$samplingOrientation, selectorEligible=$samplingProfileSelectorEligible",
        "Evaluation strategy: $evaluationStrategy",
        "Evaluated views: ${evaluatedViews.size}",
        "Stage reached: $stageReached",
        "Policy NSFW evidence: ${formatScore(policyNsfwEvidence)}",
        "Policy trigger: $policyTriggerLabel",
        "Representative view: $representativeViewLabel ($representativeViewBounds)",
        "Strongest view: $strongestViewLabel ($strongestViewBounds)",
        "Supportive detection tiles: $supportiveDetectionCount",
        "Elevated detection tiles: $elevatedDetectionCount",
        "Model result: ${if (checkResult.isSafe) "SFW-leading" else "NSFW-leading"}",
        "Reason: ${checkResult.reason}",
    ) + stageEvaluations.map { it.summaryLine() } + evaluatedViews.map { it.summaryLine() }
}

object SakosCompatibleResultMapper {
    fun toCheckResult(
        scores: SakosCompatibleModelScores,
        strongestViewLabel: String? = null,
        policyNsfwEvidence: Float = scores.nsfwProbability,
        safeLeadingOverride: Boolean? = null,
        policyTriggerLabel: String? = null,
    ): OpenNsfw2CheckResult {
        val reason = buildString {
            append(OpenNsfw2ModelContract.modelId)
            strongestViewLabel?.let { label ->
                append(", representative_view=")
                append(label)
            }
            policyTriggerLabel?.let { label ->
                append(", policy_trigger=")
                append(label)
            }
            append(": sfw_prob=")
            append(formatScore(scores.sfwProbability))
            append(", nsfw_prob=")
            append(formatScore(scores.nsfwProbability))
            append(", policy_nsfw=")
            append(formatScore(policyNsfwEvidence))
        }

        val safeLeading = safeLeadingOverride ?: (scores.nsfwProbability <= scores.sfwProbability)
        return if (safeLeading) {
            OpenNsfw2CheckResult.safe(
                confidence = policyNsfwEvidence,
                reason = reason,
                modelId = OpenNsfw2ModelContract.modelId,
            )
        } else {
            OpenNsfw2CheckResult.blocked(
                confidence = policyNsfwEvidence,
                reason = reason,
                modelId = OpenNsfw2ModelContract.modelId,
            )
        }
    }
}

private fun formatScore(value: Float): String = String.format(Locale.US, "%.3f", value)
