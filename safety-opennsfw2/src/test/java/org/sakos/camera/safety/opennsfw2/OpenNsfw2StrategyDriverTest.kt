package org.sakos.camera.safety.opennsfw2

import kotlin.test.*

/** Simulated scores only; these assertions concern control flow and exact existing policy. */
class OpenNsfw2StrategyDriverTest {
    private fun run(strategy: IntegratedOpenNsfw2Strategy = IntegratedOpenNsfw2Strategy.Adaptive14,
        width: Int = 1080, height: Int = 1920, score: (SakosCompatibleCropWindow) -> Float): LiveSakosRuntimeEvaluation =
        OpenNsfw2StrategyDriver(strategy) { w -> val risk = score(w); SakosCompatibleModelScores(1f - risk, risk) to 1L }.evaluate(width, height)

    @Test fun fixedBlocksWithoutEvaluatingRest() {
        val calls = mutableListOf<String>()
        val result = run(IntegratedOpenNsfw2Strategy.Fixed14) { calls += it.label; 0.99f }
        assertFalse(result.checkResult.isSafe); assertTrue(calls.size < 14)
        assertEquals(calls.size.toLong(), result.inferenceMillis)
    }
    @Test fun fixedSafeEvaluatesEveryUniqueWindow() {
        val result = run(IntegratedOpenNsfw2Strategy.Fixed14) { 0.01f }
        val expected = SakosCompatibleMultiCropStrategy.fixed14Windows(1080, 1920).map { it.boundsLabel() }.distinct()
        assertEquals(expected, result.evaluatedViews.map { it.cropWindow.boundsLabel() }); assertTrue(result.checkResult.isSafe)
    }
    @Test fun adaptivePortraitEasyAllowSkipsSentinels() {
        val result = run { 0.01f }
        assertEquals("stage1", result.stageReached); assertTrue(result.checkResult.isSafe)
        assertEquals(3, result.evaluatedViews.size); assertTrue(result.policyTriggerLabel.contains("easy allow"))
    }
    @Test fun adaptiveLandscapeContextAllow() {
        val result = run(width = 1920, height = 1080) { 0.15f }
        assertEquals("stage1", result.stageReached); assertTrue(result.checkResult.isSafe)
        assertFalse(result.policyTriggerLabel.contains("sentinel"))
    }
    @Test fun adaptiveSentinelAllow() {
        val result = run { 0.15f }
        assertEquals("stage1", result.stageReached); assertTrue(result.checkResult.isSafe)
        assertTrue(result.policyTriggerLabel.contains("sentinel")); assertTrue(result.evaluatedViews.size > 3)
    }
    @Test fun adaptiveSentinelBlockSkipsRemainingStages() {
        val stage1 = SakosCompatibleMultiCropStrategy.adaptive14Stages(1080, 1920).first().windows.map { it.boundsLabel() }.toSet()
        val result = run { if (it.boundsLabel() in stage1) 0.15f else 0.99f }
        assertFalse(result.checkResult.isSafe); assertEquals("stage1", result.stageReached)
        assertEquals(1, result.stageEvaluations.size)
    }
    @Test fun adaptiveImmediateBlock() {
        val result = run { 0.99f }; assertFalse(result.checkResult.isSafe); assertEquals("stage1", result.stageReached)
        assertTrue(result.evaluatedViews.size < 3)
    }
    @Test fun ambiguousPortraitChecksPackThenAllowsStage2() {
        val result = run { 0.21f }; assertTrue(result.checkResult.isSafe); assertEquals("stage2", result.stageReached)
        assertTrue(result.stageEvaluations.any { it.stageLabel.contains("portrait ambiguity") })
    }
    @Test fun ambiguousFallbackReachesRestOfFixedPackWithoutDuplicateInference() {
        val result = run { 0.32f }; assertEquals("stage3", result.stageReached)
        val bounds = result.evaluatedViews.map { it.cropWindow.boundsLabel() }
        assertEquals(bounds.distinct(), bounds)
        assertTrue(SakosCompatibleMultiCropStrategy.fixed14Windows(1080, 1920).all { it.boundsLabel() in bounds })
    }
    @Test fun nearFloorLowerLateralRunsRefinement() {
        val result = run { if (it.label == "Lower-Right lateral bust") 0.425f else 0.21f }
        assertTrue(result.stageEvaluations.any { it.stageId == "stage2-refinement" })
        assertTrue(result.evaluatedViews.any { it.cropWindow.label.contains("refine", true) })
    }
    @Test fun collapsedTinyWindowsAreNotInferredTwice() {
        val result = run(width = 1, height = 1) { 0.01f }
        assertEquals(1, result.evaluatedViews.size)
    }
    @Test fun invalidOutputAndDimensionsFailInsteadOfReturningAllow() {
        for (scores in listOf(SakosCompatibleModelScores(Float.NaN, 0f), SakosCompatibleModelScores(1f, 1f), SakosCompatibleModelScores(-1f, 2f))) {
            assertFailsWith<IllegalArgumentException> { OpenNsfw2StrategyDriver { scores to 1L }.evaluate(32, 32) }
        }
        assertFailsWith<IllegalArgumentException> { run(width = 0) { 0.01f } }
    }
    @Test fun configurationSeparatesStrategiesPreservingDefault() {
        assertEquals(OpenNsfw2ModelPreflight.configuration, OpenNsfw2ModelPreflight.configurationFor(IntegratedOpenNsfw2Strategy.Fixed14))
        assertNotEquals(OpenNsfw2ModelPreflight.configuration, OpenNsfw2ModelPreflight.configurationFor(IntegratedOpenNsfw2Strategy.Adaptive14))
    }
}
