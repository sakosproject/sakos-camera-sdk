/* Adapted from allowlisted LiveSakosNudityRuntime.kt at historical source revision omitted. */
package org.sakos.camera.safety.opennsfw2

/** Executes the exported policy using caller-supplied scores; retains no pixel input. */
class OpenNsfw2StrategyDriver(
    private val openNsfw2Strategy: IntegratedOpenNsfw2Strategy = IntegratedOpenNsfw2Strategy.Fixed14,
    private val sharedConstants: IntegratedStillGatePolicyConstants = IntegratedStillGatePolicyDefaults.fallback,
    private val infer: (SakosCompatibleCropWindow) -> Pair<SakosCompatibleModelScores, Long>,
) {
    private data class Dimensions(val width: Int, val height: Int)
    fun evaluate(width: Int, height: Int): LiveSakosRuntimeEvaluation {
        require(width > 0 && height > 0)
        val bitmap = Dimensions(width, height)
        val samplingProfile = GateSamplingProfile.detect(width, height)
        return when (openNsfw2Strategy) {
            IntegratedOpenNsfw2Strategy.Fixed14 -> evaluateFixedWindowSet(bitmap, openNsfw2Strategy.id,
                openNsfw2Strategy.displayName, openNsfw2Strategy.strategyLabel, samplingProfile)
            IntegratedOpenNsfw2Strategy.Adaptive14 -> evaluateAdaptiveWindowSet(bitmap, samplingProfile)
        }
    }
    private fun nanosToMillis(start: Long, end: Long) = (end - start).coerceAtLeast(0L) / 1_000_000L
    fun evaluateVideoBaseSweep(width: Int, height: Int): LiveSakosRuntimeEvaluation {
        require(width > 0 && height > 0)
        return baseSweep(Dimensions(width, height))
    }
    private fun baseSweep(bitmap: Dimensions): LiveSakosRuntimeEvaluation {
        val samplingProfile = GateSamplingProfile.detect(
            width = bitmap.width,
            height = bitmap.height,
        )
        val stageStartedAtNanos = System.nanoTime()
        val evaluatedViews = mutableListOf<LiveSakosRuntimeViewEvaluation>()
        val stageOne = SakosCompatibleMultiCropStrategy.adaptive14Stages(
            sourceWidth = bitmap.width,
            sourceHeight = bitmap.height,
            samplingProfile = samplingProfile,
        ).first { stage -> stage.id == "stage1" }

        evaluateWindowGroup(
            bitmap = bitmap,
            windows = stageOne.windows,
            evaluatedViews = evaluatedViews,
            allowBlockShortCircuit = true,
        )

        val signal = SakosCompatibleMultiCropStrategy.policySignal(
            views = evaluatedViews,
            sharedConstants = sharedConstants,
        )
        val stageElapsedMillis = nanosToMillis(stageStartedAtNanos, System.nanoTime())
        val stageDecision = if (SakosCompatibleMultiCropStrategy.policyWouldBlock(signal, sharedConstants)) {
            "Block"
        } else {
            "Checked"
        }

        return buildRuntimeEvaluation(
            evaluatedViews = evaluatedViews,
            policySignal = signal,
            sourceSize = "${bitmap.width}x${bitmap.height}",
            strategyId = "video-base-stage1",
            strategyDisplayName = "Video base sweep",
            strategyLabel = "Video timeline base sweep using full frame, center crop, and upper-center torso before focused escalation",
            stageReached = "video-base-stage1",
            stageEvaluations = listOf(
                LiveSakosRuntimeStageEvaluation(
                    stageId = "video-base-stage1",
                    stageLabel = "Video base Stage 1 context sweep",
                    decisionLabel = stageDecision,
                    elapsedMillis = stageElapsedMillis,
                    cumulativeEvaluatedViews = evaluatedViews.size,
                ),
            ),
            samplingProfile = samplingProfile,
        )
    }

    private fun evaluateFixedWindowSet(
        bitmap: Dimensions,
        strategyId: String,
        strategyDisplayName: String,
        strategyLabel: String,
        samplingProfile: GateSamplingProfile,
    ): LiveSakosRuntimeEvaluation {
        val evaluatedViews = mutableListOf<LiveSakosRuntimeViewEvaluation>()
        val stageStartedAtNanos = System.nanoTime()
        evaluateWindowGroup(
            bitmap = bitmap,
            windows = SakosCompatibleMultiCropStrategy.fixed14Windows(
                sourceWidth = bitmap.width,
                sourceHeight = bitmap.height,
                samplingProfile = samplingProfile,
            ),
            evaluatedViews = evaluatedViews,
            allowBlockShortCircuit = true,
        )
        val stageElapsedMillis = nanosToMillis(stageStartedAtNanos, System.nanoTime())
        val signal = SakosCompatibleMultiCropStrategy.policySignal(
            views = evaluatedViews,
            sharedConstants = sharedConstants,
        )

        return buildRuntimeEvaluation(
            evaluatedViews = evaluatedViews,
            policySignal = signal,
            sourceSize = "${bitmap.width}x${bitmap.height}",
            strategyId = strategyId,
            strategyDisplayName = strategyDisplayName,
            strategyLabel = strategyLabel,
            stageReached = strategyId,
            stageEvaluations = listOf(
                LiveSakosRuntimeStageEvaluation(
                    stageId = strategyId,
                    stageLabel = strategyDisplayName,
                    decisionLabel = if (SakosCompatibleMultiCropStrategy.policyWouldBlock(signal, sharedConstants)) {
                        "Block"
                    } else {
                        "Final policy"
                    },
                    elapsedMillis = stageElapsedMillis,
                    cumulativeEvaluatedViews = evaluatedViews.size,
                ),
            ),
            samplingProfile = samplingProfile,
        )
    }

    private fun evaluateAdaptiveWindowSet(
        bitmap: Dimensions,
        samplingProfile: GateSamplingProfile,
    ): LiveSakosRuntimeEvaluation {
        val stagesById = SakosCompatibleMultiCropStrategy.adaptive14Stages(
            sourceWidth = bitmap.width,
            sourceHeight = bitmap.height,
            samplingProfile = samplingProfile,
        ).associateBy { it.id }
        val stageOne = stagesById["stage1"] ?: error("Missing stage1 adaptive definition.")
        val stageTwo = stagesById["stage2"] ?: error("Missing stage2 adaptive definition.")
        val stageThree = stagesById["stage3"] ?: error("Missing stage3 adaptive definition.")
        val shouldRunPortraitSentinels = SakosCompatibleMultiCropStrategy.shouldRunAdaptiveStage1SentinelPass(
            sourceWidth = bitmap.width,
            sourceHeight = bitmap.height,
            samplingProfile = samplingProfile,
        )
        val evaluatedViews = mutableListOf<LiveSakosRuntimeViewEvaluation>()
        val stageEvaluations = mutableListOf<LiveSakosRuntimeStageEvaluation>()
        var finalSignal: SakosCompatiblePolicySignal? = null
        var finalStageReached = "stage3"

        fun evaluateIncrementalSubpack(windows: List<SakosCompatibleCropWindow>): SakosCompatiblePolicySignal? {
            for (window in windows) {
                evaluateWindowGroup(
                    bitmap = bitmap,
                    windows = listOf(window),
                    evaluatedViews = evaluatedViews,
                    allowBlockShortCircuit = false,
                )
                val signal = SakosCompatibleMultiCropStrategy.policySignal(
                    views = evaluatedViews,
                    sharedConstants = sharedConstants,
                )
                if (SakosCompatibleMultiCropStrategy.policyWouldBlock(signal, sharedConstants)) {
                    return signal
                }
            }
            return null
        }

        val stageOneStartedAtNanos = System.nanoTime()
        evaluateWindowGroup(
            bitmap = bitmap,
            windows = stageOne.windows,
            evaluatedViews = evaluatedViews,
            allowBlockShortCircuit = true,
        )
        var stageOneSignal = SakosCompatibleMultiCropStrategy.policySignal(
            views = evaluatedViews,
            sharedConstants = sharedConstants,
        )
        val stageOneDecision = if (SakosCompatibleMultiCropStrategy.policyWouldBlock(stageOneSignal, sharedConstants)) {
            finalSignal = stageOneSignal.copy(
                triggerLabel = "Adaptive Stage 1 blocked. ${stageOneSignal.triggerLabel}",
            )
            finalStageReached = stageOne.id
            "Block"
        } else {
            val stageOneViews = evaluatedViews.take(stageOne.windows.size)
            if (stageOneViews.size == stageOne.windows.size &&
                shouldRunPortraitSentinels &&
                SakosCompatibleMultiCropStrategy.shouldAdaptiveStage1EasyAllow(
                    stageOneViews = stageOneViews,
                    sharedConstants = sharedConstants,
                )
            ) {
                finalSignal = stageOneSignal.copy(
                    policyNsfwEvidence = SakosCompatibleMultiCropStrategy.maxNsfwProbability(stageOneViews)
                        .coerceAtMost(sharedConstants.rawNsfwBlockFloor - 0.01f),
                    safeLeading = true,
                    triggerLabel = "Adaptive Stage 1 easy allow: the initial portrait context views all stayed below ${sharedConstants.stage1EasyAllowMax}, so the selfie-sensitive sentinel branch was skipped.",
                )
                finalStageReached = stageOne.id
                "Allow"
            } else if (stageOneViews.size == stageOne.windows.size &&
                SakosCompatibleMultiCropStrategy.shouldAdaptiveStage1Allow(
                    stageOneViews = stageOneViews,
                    sharedConstants = sharedConstants,
                )
            ) {
                if (shouldRunPortraitSentinels) {
                    evaluateWindowGroup(
                        bitmap = bitmap,
                        windows = SakosCompatibleMultiCropStrategy.adaptiveStage1SentinelWindows(
                            sourceWidth = bitmap.width,
                            sourceHeight = bitmap.height,
                            samplingProfile = samplingProfile,
                        ),
                        evaluatedViews = evaluatedViews,
                        allowBlockShortCircuit = true,
                    )
                    stageOneSignal = SakosCompatibleMultiCropStrategy.policySignal(
                        views = evaluatedViews,
                        sharedConstants = sharedConstants,
                    )
                    val stageOneAndSentinelViews = evaluatedViews.toList()
                    if (SakosCompatibleMultiCropStrategy.policyWouldBlock(stageOneSignal, sharedConstants)) {
                        finalSignal = stageOneSignal.copy(
                            triggerLabel = "Adaptive Stage 1 portrait sentinel pass blocked. ${stageOneSignal.triggerLabel}",
                        )
                        finalStageReached = stageOne.id
                        "Block"
                    } else if (
                        SakosCompatibleMultiCropStrategy.shouldAdaptiveStage1Allow(
                            stageOneViews = stageOneAndSentinelViews,
                            sharedConstants = sharedConstants,
                        )
                    ) {
                        finalSignal = stageOneSignal.copy(
                            policyNsfwEvidence = SakosCompatibleMultiCropStrategy.maxNsfwProbability(stageOneAndSentinelViews)
                                .coerceAtMost(sharedConstants.rawNsfwBlockFloor - 0.01f),
                            safeLeading = true,
                            triggerLabel = "Adaptive Stage 1 allow: the initial context views plus portrait-side sentinel probes all stayed below ${sharedConstants.stage1AllowMax}.",
                        )
                        finalStageReached = stageOne.id
                        "Allow"
                    } else {
                        "Escalate"
                    }
                } else {
                    finalSignal = stageOneSignal.copy(
                        policyNsfwEvidence = SakosCompatibleMultiCropStrategy.maxNsfwProbability(stageOneViews)
                            .coerceAtMost(sharedConstants.rawNsfwBlockFloor - 0.01f),
                        safeLeading = true,
                        triggerLabel = "Adaptive Stage 1 allow: the initial context and torso views all stayed below ${sharedConstants.stage1AllowMax}.",
                    )
                    finalStageReached = stageOne.id
                    "Allow"
                }
            } else {
                "Escalate"
            }
        }
        val stageOneElapsedMillis = nanosToMillis(stageOneStartedAtNanos, System.nanoTime())
        stageEvaluations += LiveSakosRuntimeStageEvaluation(
            stageId = stageOne.id,
            stageLabel = stageOne.label,
            decisionLabel = stageOneDecision,
            elapsedMillis = stageOneElapsedMillis,
            cumulativeEvaluatedViews = evaluatedViews.size,
        )

        if (stageOneDecision == "Block" || stageOneDecision == "Allow") {
            return buildRuntimeEvaluation(
                evaluatedViews = evaluatedViews,
                policySignal = finalSignal ?: stageOneSignal,
                sourceSize = "${bitmap.width}x${bitmap.height}",
                strategyId = openNsfw2Strategy.id,
                strategyDisplayName = openNsfw2Strategy.displayName,
                strategyLabel = openNsfw2Strategy.strategyLabel,
                stageReached = finalStageReached,
                stageEvaluations = stageEvaluations,
                samplingProfile = samplingProfile,
            )
        }

        val portraitAmbiguityPack = if (shouldRunPortraitSentinels) {
            SakosCompatibleMultiCropStrategy.adaptivePortraitAmbiguityPackWindows(
                sourceWidth = bitmap.width,
                sourceHeight = bitmap.height,
                samplingProfile = samplingProfile,
            )
        } else {
            emptyList()
        }

        if (portraitAmbiguityPack.isNotEmpty()) {
            val portraitPackStartedAtNanos = System.nanoTime()
            val portraitPackSignal = evaluateIncrementalSubpack(portraitAmbiguityPack)
            val portraitPackElapsedMillis = nanosToMillis(portraitPackStartedAtNanos, System.nanoTime())
            val portraitPackDecision = if (portraitPackSignal != null) {
                finalSignal = portraitPackSignal.copy(
                    triggerLabel = "Adaptive Stage 2 portrait ambiguity pack blocked. ${portraitPackSignal.triggerLabel}",
                )
                finalStageReached = stageTwo.id
                "Block"
            } else {
                "Checked"
            }
            stageEvaluations += LiveSakosRuntimeStageEvaluation(
                stageId = stageTwo.id,
                stageLabel = "Stage 2 portrait ambiguity pack",
                decisionLabel = portraitPackDecision,
                elapsedMillis = portraitPackElapsedMillis,
                cumulativeEvaluatedViews = evaluatedViews.size,
            )
            if (portraitPackDecision == "Block") {
                return buildRuntimeEvaluation(
                    evaluatedViews = evaluatedViews,
                    policySignal = finalSignal ?: SakosCompatibleMultiCropStrategy.policySignal(
                        views = evaluatedViews,
                        sharedConstants = sharedConstants,
                    ),
                    sourceSize = "${bitmap.width}x${bitmap.height}",
                    strategyId = openNsfw2Strategy.id,
                    strategyDisplayName = openNsfw2Strategy.displayName,
                    strategyLabel = openNsfw2Strategy.strategyLabel,
                    stageReached = finalStageReached,
                    stageEvaluations = stageEvaluations,
                    samplingProfile = samplingProfile,
                )
            }
        }

        val stageTwoStartedAtNanos = System.nanoTime()
        var stageTwoSignal = evaluateIncrementalSubpack(stageTwo.windows)
        val stageTwoElapsedMillis = nanosToMillis(stageTwoStartedAtNanos, System.nanoTime())
        if (stageTwoSignal == null) {
            val strongestLowerLateral = SakosCompatibleMultiCropStrategy.strongestNearFloorLowerLateralView(
                views = evaluatedViews,
                sharedConstants = sharedConstants,
            )
            val refinementWindows = strongestLowerLateral?.let { candidate ->
                SakosCompatibleMultiCropStrategy.adaptiveStrongestCropRefinementWindows(
                    sourceWidth = bitmap.width,
                    sourceHeight = bitmap.height,
                    anchorWindow = candidate.cropWindow,
                )
            }.orEmpty()
            if (refinementWindows.isNotEmpty()) {
                val refinementStartedAtNanos = System.nanoTime()
                val refinementSignal = evaluateIncrementalSubpack(refinementWindows)
                val refinementElapsedMillis = nanosToMillis(
                    refinementStartedAtNanos,
                    System.nanoTime(),
                )
                val refinementDecision = if (refinementSignal != null) {
                    finalSignal = refinementSignal.copy(
                        triggerLabel = "Adaptive Stage 2 strongest-crop refinement blocked. ${refinementSignal.triggerLabel}",
                    )
                    finalStageReached = "stage2-refinement"
                    stageTwoSignal = refinementSignal
                    "Block"
                } else {
                    "Checked"
                }
                stageEvaluations += LiveSakosRuntimeStageEvaluation(
                    stageId = "stage2-refinement",
                    stageLabel = "Stage 2 strongest-crop refinement",
                    decisionLabel = refinementDecision,
                    elapsedMillis = refinementElapsedMillis,
                    cumulativeEvaluatedViews = evaluatedViews.size,
                )
                if (refinementDecision == "Block") {
                    return buildRuntimeEvaluation(
                        evaluatedViews = evaluatedViews,
                        policySignal = finalSignal ?: SakosCompatibleMultiCropStrategy.policySignal(
                            views = evaluatedViews,
                            sharedConstants = sharedConstants,
                        ),
                        sourceSize = "${bitmap.width}x${bitmap.height}",
                        strategyId = openNsfw2Strategy.id,
                        strategyDisplayName = openNsfw2Strategy.displayName,
                        strategyLabel = openNsfw2Strategy.strategyLabel,
                        stageReached = finalStageReached,
                        stageEvaluations = stageEvaluations,
                        samplingProfile = samplingProfile,
                    )
                }
            }
        }
        val stageTwoDecision = when {
            stageTwoSignal != null -> {
                finalSignal = stageTwoSignal.copy(
                    triggerLabel = "Adaptive Stage 2 blocked. ${stageTwoSignal.triggerLabel}",
                )
                finalStageReached = stageTwo.id
                "Block"
            }

            SakosCompatibleMultiCropStrategy.shouldAdaptiveStage2Allow(
                accumulatedViews = evaluatedViews,
                sharedConstants = sharedConstants,
            ) -> {
                val finalStageTwoSignal = SakosCompatibleMultiCropStrategy.policySignal(
                    views = evaluatedViews,
                    sharedConstants = sharedConstants,
                )
                finalSignal = finalStageTwoSignal.copy(
                    policyNsfwEvidence = SakosCompatibleMultiCropStrategy.maxNsfwProbability(evaluatedViews)
                        .coerceAtMost(sharedConstants.rawNsfwBlockFloor - 0.01f),
                    safeLeading = true,
                    triggerLabel = "Adaptive Stage 2 allow: accumulated views stayed below ${sharedConstants.stage2AllowMax} without corroborated crop evidence at or above ${sharedConstants.stage2CorroboratedDetectionFloor}.",
                )
                finalStageReached = stageTwo.id
                "Allow"
            }

            else -> "Escalate"
        }
        stageEvaluations += LiveSakosRuntimeStageEvaluation(
            stageId = stageTwo.id,
            stageLabel = stageTwo.label,
            decisionLabel = stageTwoDecision,
            elapsedMillis = stageTwoElapsedMillis,
            cumulativeEvaluatedViews = evaluatedViews.size,
        )

        if (stageTwoDecision == "Block" || stageTwoDecision == "Allow") {
            return buildRuntimeEvaluation(
                evaluatedViews = evaluatedViews,
                policySignal = finalSignal ?: SakosCompatibleMultiCropStrategy.policySignal(
                    views = evaluatedViews,
                    sharedConstants = sharedConstants,
                ),
                sourceSize = "${bitmap.width}x${bitmap.height}",
                strategyId = openNsfw2Strategy.id,
                strategyDisplayName = openNsfw2Strategy.displayName,
                strategyLabel = openNsfw2Strategy.strategyLabel,
                stageReached = finalStageReached,
                stageEvaluations = stageEvaluations,
                samplingProfile = samplingProfile,
            )
        }

        val stageThreeStartedAtNanos = System.nanoTime()
        evaluateWindowGroup(
            bitmap = bitmap,
            windows = stageThree.windows,
            evaluatedViews = evaluatedViews,
            allowBlockShortCircuit = true,
        )
        val stageThreeSignal = SakosCompatibleMultiCropStrategy.policySignal(
            views = evaluatedViews,
            sharedConstants = sharedConstants,
        )
        finalSignal = stageThreeSignal
        finalStageReached = stageThree.id
        val stageThreeDecision = if (SakosCompatibleMultiCropStrategy.policyWouldBlock(stageThreeSignal, sharedConstants)) {
            "Block"
        } else {
            "Final policy"
        }
        val stageThreeElapsedMillis = nanosToMillis(stageThreeStartedAtNanos, System.nanoTime())
        stageEvaluations += LiveSakosRuntimeStageEvaluation(
            stageId = stageThree.id,
            stageLabel = stageThree.label,
            decisionLabel = stageThreeDecision,
            elapsedMillis = stageThreeElapsedMillis,
            cumulativeEvaluatedViews = evaluatedViews.size,
        )

        return buildRuntimeEvaluation(
            evaluatedViews = evaluatedViews,
            policySignal = finalSignal ?: stageThreeSignal,
            sourceSize = "${bitmap.width}x${bitmap.height}",
            strategyId = openNsfw2Strategy.id,
            strategyDisplayName = openNsfw2Strategy.displayName,
            strategyLabel = openNsfw2Strategy.strategyLabel,
            stageReached = finalStageReached,
            stageEvaluations = stageEvaluations,
            samplingProfile = samplingProfile,
        )
    }

    private fun evaluateWindowGroup(
        bitmap: Dimensions,
        windows: List<SakosCompatibleCropWindow>,
        evaluatedViews: MutableList<LiveSakosRuntimeViewEvaluation>,
        allowBlockShortCircuit: Boolean,
    ) {
        val evaluatedBounds = evaluatedViews.mapTo(mutableSetOf()) { it.cropWindow.boundsLabel() }
        for (cropWindow in windows) {
            if (!evaluatedBounds.add(cropWindow.boundsLabel())) {
                continue
            }
            val evaluation = infer(cropWindow)
            require(evaluation.first.sfwProbability.isFinite() && evaluation.first.nsfwProbability.isFinite())
            require(evaluation.first.sfwProbability in 0f..1f && evaluation.first.nsfwProbability in 0f..1f)
            require(kotlin.math.abs(evaluation.first.sfwProbability + evaluation.first.nsfwProbability - 1f) <= 0.01f)
            require(evaluation.second >= 0L)
            evaluatedViews += LiveSakosRuntimeViewEvaluation(
                cropWindow = cropWindow,
                scores = evaluation.first,
                inferenceMillis = evaluation.second,
            )
            if (allowBlockShortCircuit &&
                SakosCompatibleMultiCropStrategy.shouldShortCircuitBlock(
                    views = evaluatedViews,
                    sharedConstants = sharedConstants,
                )
            ) {
                break
            }
        }
    }

    private fun buildRuntimeEvaluation(
        evaluatedViews: List<LiveSakosRuntimeViewEvaluation>,
        policySignal: SakosCompatiblePolicySignal,
        sourceSize: String,
        strategyId: String,
        strategyDisplayName: String,
        strategyLabel: String,
        stageReached: String,
        stageEvaluations: List<LiveSakosRuntimeStageEvaluation>,
        samplingProfile: GateSamplingProfile,
    ): LiveSakosRuntimeEvaluation {
        val strongestView = policySignal.strongestView
        val representativeView = policySignal.representativeView
        val scores = representativeView.scores
        val inferenceMillis = evaluatedViews.sumOf { it.inferenceMillis }

        return LiveSakosRuntimeEvaluation(
            scores = scores,
            checkResult = SakosCompatibleResultMapper.toCheckResult(
                scores = scores,
                strongestViewLabel = representativeView.cropWindow.label,
                policyNsfwEvidence = policySignal.policyNsfwEvidence,
                safeLeadingOverride = policySignal.safeLeading,
                policyTriggerLabel = policySignal.triggerLabel,
            ),
            inferenceMillis = inferenceMillis,
            sourceSize = sourceSize,
            evaluationStrategy = strategyLabel,
            strategyId = strategyId,
            strategyDisplayName = strategyDisplayName,
            policyNsfwEvidence = policySignal.policyNsfwEvidence,
            policyTriggerLabel = policySignal.triggerLabel,
            representativeViewLabel = representativeView.cropWindow.label,
            representativeViewBounds = representativeView.cropWindow.boundsLabel(),
            strongestViewLabel = strongestView.cropWindow.label,
            strongestViewBounds = strongestView.cropWindow.boundsLabel(),
            stageReached = stageReached,
            stageEvaluations = stageEvaluations,
            supportiveDetectionCount = policySignal.supportiveDetectionCount,
            elevatedDetectionCount = policySignal.elevatedDetectionCount,
            evaluatedViews = evaluatedViews,
            runtimeProfileId = "opennsfw2-${openNsfw2Strategy.id}",
            runtimeProfileDisplayName = "OpenNSFW2 ${openNsfw2Strategy.displayName}",
            modelDisplayName = "SakOS Nudity Model",
            executionMode = "LiteRT",
            samplingProfileId = samplingProfile.id,
            samplingProfileLabel = samplingProfile.displayLabel,
            samplingRatio = samplingProfile.normalizedRatio,
            samplingOrientation = samplingProfile.orientation.displayLabel,
            samplingProfileSelectorEligible = samplingProfile.selectorEligible,
        )
    }

}
