/* Selectively extracted from user-authorized source commit 843d93f. */
package org.sakos.camera.capture.video

import kotlin.math.ceil
import kotlin.math.roundToLong

enum class VideoGateSamplePass(val label: String) {
    Base("Base sweep"),
    Escalation("Focused escalation"),
}

enum class VideoGateSampleReason(val label: String) {
    Midpoint("midpoint"),
    EarlyAnchor("early anchor"),
    Interval("interval"),
    LateAnchor("late anchor"),
    ConfirmationBefore("confirmation before"),
    ConfirmationAfter("confirmation after"),
    Escalation("escalation"),
}

data class VideoGateSamplePlan(
    val sampledAtMillis: Long,
    val reason: VideoGateSampleReason,
    val pass: VideoGateSamplePass,
)

data class VideoTemporalSamplePlan(
    val durationMillis: Long,
    val baseSamples: List<VideoGateSamplePlan>,
    val supportsNormalAllow: Boolean,
    val unsupportedReason: String?,
)

object VideoTemporalSamplePlanner {
    fun buildCalibrationSmokePlan(
        durationMillis: Long,
        policy: VideoGatePolicy = VideoGatePolicy,
    ): VideoTemporalSamplePlan {
        val normalizedDurationMillis = durationMillis.coerceAtLeast(0L)
        if (normalizedDurationMillis > policy.maxNormalDurationMillis) {
            return VideoTemporalSamplePlan(
                durationMillis = normalizedDurationMillis,
                baseSamples = emptyList(),
                supportsNormalAllow = false,
                unsupportedReason = "Video duration ${formatPlannerDuration(normalizedDurationMillis)} exceeds the supported 10:00 gate limit.",
            )
        }
        val sampledAtMillis = when {
            normalizedDurationMillis <= 0L -> 0L
            else -> normalizedDurationMillis / 2L
        }
        return VideoTemporalSamplePlan(
            durationMillis = normalizedDurationMillis,
            baseSamples = listOf(
                VideoGateSamplePlan(
                    sampledAtMillis = sampledAtMillis,
                    reason = VideoGateSampleReason.Midpoint,
                    pass = VideoGateSamplePass.Base,
                ),
            ),
            supportsNormalAllow = true,
            unsupportedReason = null,
        )
    }

    fun buildBasePlan(
        durationMillis: Long,
        policy: VideoGatePolicy = VideoGatePolicy,
    ): VideoTemporalSamplePlan {
        val normalizedDurationMillis = durationMillis.coerceAtLeast(0L)
        if (normalizedDurationMillis > policy.maxNormalDurationMillis) {
            return VideoTemporalSamplePlan(
                durationMillis = normalizedDurationMillis,
                baseSamples = emptyList(),
                supportsNormalAllow = false,
                unsupportedReason = "Video duration ${formatPlannerDuration(normalizedDurationMillis)} exceeds the supported 10:00 gate limit.",
            )
        }

        val timestamps = when {
            normalizedDurationMillis <= 0L -> listOf(0L)
            normalizedDurationMillis < 1_500L -> listOf(normalizedDurationMillis / 2L)
            normalizedDurationMillis < 30_000L -> percentSamples(
                durationMillis = normalizedDurationMillis,
                sampleCount = policy.shortClipBaseSampleCount,
            )
            normalizedDurationMillis < 2L * 60L * 1000L -> percentSamples(
                durationMillis = normalizedDurationMillis,
                sampleCount = samplesForMediumClip(normalizedDurationMillis, policy),
            )
            else -> intervalSamples(
                durationMillis = normalizedDurationMillis,
                intervalMillis = policy.baseIntervalMillis,
                minSamples = policy.minLongClipBaseSampleCount,
                maxSamples = policy.maxBaseSamples,
            )
        }

        return VideoTemporalSamplePlan(
            durationMillis = normalizedDurationMillis,
            baseSamples = timestamps.mapIndexed { index, sampledAtMillis ->
                VideoGateSamplePlan(
                    sampledAtMillis = sampledAtMillis,
                    reason = reasonForBaseSample(index, timestamps.lastIndex),
                    pass = VideoGateSamplePass.Base,
                )
            },
            supportsNormalAllow = true,
            unsupportedReason = null,
        )
    }

    fun buildEscalationPlan(
        seedSamples: List<VideoGateSamplePlan>,
        durationMillis: Long,
        existingDecodedCount: Int,
        policy: VideoGatePolicy = VideoGatePolicy,
    ): List<VideoGateSamplePlan> {
        val remainingDecodedBudget = policy.maxDecodedSamples - existingDecodedCount
        if (remainingDecodedBudget <= 0 || seedSamples.isEmpty()) {
            return emptyList()
        }

        val escalationPlans = mutableListOf<VideoGateSamplePlan>()
        val seen = mutableSetOf<Pair<Long, VideoGateSamplePass>>()

        fun addPlan(sampledAtMillis: Long, reason: VideoGateSampleReason) {
            if (escalationPlans.size >= remainingDecodedBudget) {
                return
            }
            val clamped = clampTimestamp(
                timestampMillis = sampledAtMillis,
                durationMillis = durationMillis,
                endpointPaddingMillis = policy.endpointPaddingMillis,
            )
            val key = clamped to VideoGateSamplePass.Escalation
            if (seen.add(key)) {
                escalationPlans += VideoGateSamplePlan(
                    sampledAtMillis = clamped,
                    reason = reason,
                    pass = VideoGateSamplePass.Escalation,
                )
            }
        }

        seedSamples
            .take(policy.maxEscalatedTimestamps)
            .forEach { seed ->
                addPlan(seed.sampledAtMillis, VideoGateSampleReason.Escalation)
                if (policy.confirmationSamplesPerSuspiciousTimestamp >= 1) {
                    addPlan(seed.sampledAtMillis - policy.confirmationOffsetMillis, VideoGateSampleReason.ConfirmationBefore)
                }
                if (policy.confirmationSamplesPerSuspiciousTimestamp >= 2) {
                    addPlan(seed.sampledAtMillis + policy.confirmationOffsetMillis, VideoGateSampleReason.ConfirmationAfter)
                }
            }

        return escalationPlans
    }

    private fun samplesForMediumClip(
        durationMillis: Long,
        policy: VideoGatePolicy,
    ): Int {
        val intervalDrivenCount = ceil(durationMillis / 15_000.0).toInt() + 1
        return intervalDrivenCount
            .coerceAtLeast(7)
            .coerceAtMost(policy.mediumClipTargetSampleCount)
    }

    private fun percentSamples(
        durationMillis: Long,
        sampleCount: Int,
    ): List<Long> = (0 until sampleCount)
        .map { index ->
            val position = (index + 1).toDouble() / (sampleCount + 1).toDouble()
            clampTimestamp(
                timestampMillis = (durationMillis * position).roundToLong(),
                durationMillis = durationMillis,
                endpointPaddingMillis = VideoGatePolicy.endpointPaddingMillis,
            )
        }
        .distinct()

    private fun intervalSamples(
        durationMillis: Long,
        intervalMillis: Long,
        minSamples: Int,
        maxSamples: Int,
    ): List<Long> {
        val interiorStart = VideoGatePolicy.endpointPaddingMillis
        val interiorEnd = (durationMillis - VideoGatePolicy.endpointPaddingMillis).coerceAtLeast(interiorStart)
        val rawSamples = buildList {
            add(interiorStart)
            var next = intervalMillis / 2L
            while (next < interiorEnd) {
                add(next)
                next += intervalMillis
            }
            add(interiorEnd)
        }.distinct()

        if (rawSamples.size < minSamples) {
            return percentSamples(
                durationMillis = durationMillis,
                sampleCount = minSamples,
            )
        }

        if (rawSamples.size <= maxSamples) {
            return rawSamples
        }

        return (0 until maxSamples)
            .map { index ->
                val position = index.toDouble() / (maxSamples - 1).toDouble()
                (interiorStart + ((interiorEnd - interiorStart) * position)).roundToLong()
            }
            .distinct()
    }

    private fun reasonForBaseSample(
        index: Int,
        lastIndex: Int,
    ): VideoGateSampleReason = when {
        lastIndex == 0 -> VideoGateSampleReason.Midpoint
        index == 0 -> VideoGateSampleReason.EarlyAnchor
        index == lastIndex -> VideoGateSampleReason.LateAnchor
        else -> VideoGateSampleReason.Interval
    }

    private fun clampTimestamp(
        timestampMillis: Long,
        durationMillis: Long,
        endpointPaddingMillis: Long,
    ): Long {
        if (durationMillis <= 0L) {
            return 0L
        }
        val dynamicPaddingMillis = endpointPaddingMillis.coerceAtMost((durationMillis / 4L).coerceAtLeast(0L))
        val upper = (durationMillis - dynamicPaddingMillis).coerceAtLeast(0L)
        val lower = dynamicPaddingMillis.coerceAtMost(upper)
        return timestampMillis.coerceIn(lower, upper)
    }
}

private fun formatPlannerDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}
