/* Selectively extracted from user-authorized source commit 843d93f. */
package org.sakos.camera.capture.video

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoTemporalSamplePlannerTest {
    @Test
    fun oneSecondClipUsesSingleMidpointBaseSample() {
        val plan = VideoTemporalSamplePlanner.buildBasePlan(1_000L)
        assertTrue(plan.supportsNormalAllow)
        assertEquals(1, plan.baseSamples.size)
        assertEquals(500L, plan.baseSamples.single().sampledAtMillis)
        assertEquals(VideoGateSampleReason.Midpoint, plan.baseSamples.single().reason)
    }

    @Test
    fun tenSecondClipUsesFiveBaseSamples() {
        val plan = VideoTemporalSamplePlanner.buildBasePlan(10_000L)
        assertTrue(plan.supportsNormalAllow)
        assertEquals(5, plan.baseSamples.size)
        assertEquals(VideoGateSampleReason.EarlyAnchor, plan.baseSamples.first().reason)
        assertEquals(VideoGateSampleReason.LateAnchor, plan.baseSamples.last().reason)
    }

    @Test
    fun calibrationSmokePlanUsesOneMidpointFrameForShortProbe() {
        val plan = VideoTemporalSamplePlanner.buildCalibrationSmokePlan(1_200L)
        assertTrue(plan.supportsNormalAllow)
        assertEquals(1, plan.baseSamples.size)
        assertEquals(600L, plan.baseSamples.single().sampledAtMillis)
        assertEquals(VideoGateSampleReason.Midpoint, plan.baseSamples.single().reason)
    }

    @Test
    fun thirtySecondClipUsesSevenBaseSamples() {
        val plan = VideoTemporalSamplePlanner.buildBasePlan(30_000L)
        assertTrue(plan.supportsNormalAllow)
        assertEquals(7, plan.baseSamples.size)
    }

    @Test
    fun twoMinuteClipUsesLongClipMinimumBaseSamples() {
        val plan = VideoTemporalSamplePlanner.buildBasePlan(2L * 60L * 1_000L)
        assertTrue(plan.supportsNormalAllow)
        assertEquals(9, plan.baseSamples.size)
    }

    @Test
    fun fiveMinuteClipUsesThirtySecondSweep() {
        val plan = VideoTemporalSamplePlanner.buildBasePlan(5L * 60L * 1_000L)
        assertTrue(plan.supportsNormalAllow)
        assertTrue(plan.baseSamples.size in 11..12)
        assertTrue(plan.baseSamples.first().sampledAtMillis > 0L)
        assertTrue(plan.baseSamples.last().sampledAtMillis < 5L * 60L * 1_000L)
    }

    @Test
    fun tenMinuteClipUsesCappedTimelineSweepInsteadOfFiveFrames() {
        val plan = VideoTemporalSamplePlanner.buildBasePlan(10L * 60L * 1_000L)
        assertTrue(plan.supportsNormalAllow)
        assertTrue(plan.baseSamples.size in 22..25)
        assertTrue(plan.baseSamples.first().sampledAtMillis > 0L)
        assertTrue(plan.baseSamples.last().sampledAtMillis < 10L * 60L * 1_000L)
    }

    @Test
    fun overTenMinuteClipIsNotNormalAllowCandidate() {
        val plan = VideoTemporalSamplePlanner.buildBasePlan(10L * 60L * 1_000L + 1L)
        assertFalse(plan.supportsNormalAllow)
        assertTrue(plan.baseSamples.isEmpty())
        assertTrue(plan.unsupportedReason.orEmpty().contains("10:00"))
    }

    @Test
    fun escalationPlanAddsSeedAndNearbyConfirmationSamplesWithinBudget() {
        val basePlan = VideoTemporalSamplePlanner.buildBasePlan(60_000L)
        val seeds = basePlan.baseSamples.drop(2).take(2)
        val escalationPlan = VideoTemporalSamplePlanner.buildEscalationPlan(seeds, 60_000L, basePlan.baseSamples.size)
        assertEquals(6, escalationPlan.size)
        assertTrue(escalationPlan.all { it.pass == VideoGateSamplePass.Escalation })
        assertTrue(escalationPlan.any { it.reason == VideoGateSampleReason.ConfirmationBefore })
        assertTrue(escalationPlan.any { it.reason == VideoGateSampleReason.ConfirmationAfter })
    }

    @Test
    fun shortSuspiciousClipCanAddNearbyConfirmationSamples() {
        val basePlan = VideoTemporalSamplePlanner.buildBasePlan(1_000L)
        val escalationPlan = VideoTemporalSamplePlanner.buildEscalationPlan(basePlan.baseSamples, 1_000L, basePlan.baseSamples.size)
        assertEquals(3, escalationPlan.size)
        assertEquals(listOf(500L, 250L, 750L), escalationPlan.map { it.sampledAtMillis })
    }
}
