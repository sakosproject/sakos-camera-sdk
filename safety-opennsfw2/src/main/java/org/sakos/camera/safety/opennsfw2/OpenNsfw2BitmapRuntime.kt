package org.sakos.camera.safety.opennsfw2

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.SystemClock
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.DataType

/**
 * On-device OpenNSFW2 runner for caller-owned [Bitmap] instances.
 *
 * The runtime verifies the bundled asset before opening it, preserves the
 * documented BGR preprocessing contract, and leaves capture/storage ownership
 * with the host application.
 */
class OpenNsfw2BitmapRuntime private constructor(
    private val interpreter: Interpreter,
    private val threadCount: Int,
    private val policy: IntegratedStillGatePolicyConstants,
) : AutoCloseable {
    private val inputWidth = OpenNsfw2ModelPreflight.inputShape[1]
    private val inputHeight = OpenNsfw2ModelPreflight.inputShape[2]
    private val scratchBitmap = Bitmap.createBitmap(inputWidth, inputHeight, Bitmap.Config.ARGB_8888)
    private val scratchCanvas = Canvas(scratchBitmap)
    private val sourceRect = Rect()
    private val destinationRect = Rect(0, 0, inputWidth, inputHeight)
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val inputBuffer = ByteBuffer.allocateDirect(inputWidth * inputHeight * 3 * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
    private val pixels = IntArray(inputWidth * inputHeight)
    private val output = Array(1) { FloatArray(OpenNsfw2ModelPreflight.outputShape[1]) }
    private var closed = false

    /** Evaluates the existing fixed spatial policy and returns its full local evidence record. */
    @Synchronized
    fun evaluate(bitmap: Bitmap): LiveSakosRuntimeEvaluation {
        check(!closed) { "The OpenNSFW2 runtime has been closed." }
        require(!bitmap.isRecycled) { "The input bitmap has been recycled." }

        val samplingProfile = GateSamplingProfile.detect(bitmap.width, bitmap.height)
        val views = SakosCompatibleMultiCropStrategy.fixed14Windows(
            sourceWidth = bitmap.width,
            sourceHeight = bitmap.height,
            samplingProfile = samplingProfile,
        ).map { window ->
            val (scores, inferenceMillis) = evaluateView(bitmap, window)
            LiveSakosRuntimeViewEvaluation(window, scores, inferenceMillis)
        }
        val signal = SakosCompatibleMultiCropStrategy.policySignal(views, policy)
        val representative = signal.representativeView
        val checkResult = SakosCompatibleResultMapper.toCheckResult(
            scores = representative.scores,
            strongestViewLabel = representative.cropWindow.label,
            policyNsfwEvidence = signal.policyNsfwEvidence,
            safeLeadingOverride = signal.safeLeading,
            policyTriggerLabel = signal.triggerLabel,
        )

        return LiveSakosRuntimeEvaluation(
            scores = representative.scores,
            checkResult = checkResult,
            inferenceMillis = views.sumOf(LiveSakosRuntimeViewEvaluation::inferenceMillis),
            sourceSize = "${bitmap.width}x${bitmap.height}",
            evaluationStrategy = SakosCompatibleMultiCropStrategy.fixed14StrategyLabel(),
            strategyId = IntegratedOpenNsfw2Strategy.Fixed14.id,
            strategyDisplayName = IntegratedOpenNsfw2Strategy.Fixed14.displayName,
            policyNsfwEvidence = signal.policyNsfwEvidence,
            policyTriggerLabel = signal.triggerLabel,
            representativeViewLabel = representative.cropWindow.label,
            representativeViewBounds = representative.cropWindow.boundsLabel(),
            strongestViewLabel = signal.strongestView.cropWindow.label,
            strongestViewBounds = signal.strongestView.cropWindow.boundsLabel(),
            stageReached = IntegratedOpenNsfw2Strategy.Fixed14.id,
            stageEvaluations = listOf(
                LiveSakosRuntimeStageEvaluation(
                    stageId = IntegratedOpenNsfw2Strategy.Fixed14.id,
                    stageLabel = IntegratedOpenNsfw2Strategy.Fixed14.displayName,
                    decisionLabel = if (SakosCompatibleMultiCropStrategy.policyWouldBlock(signal, policy)) "Block" else "Final policy",
                    elapsedMillis = views.sumOf(LiveSakosRuntimeViewEvaluation::inferenceMillis),
                    cumulativeEvaluatedViews = views.size,
                ),
            ),
            supportiveDetectionCount = signal.supportiveDetectionCount,
            elevatedDetectionCount = signal.elevatedDetectionCount,
            evaluatedViews = views,
            runtimeProfileId = "opennsfw2-fixed14",
            runtimeProfileDisplayName = "OpenNSFW2 fixed 14-view",
            modelDisplayName = "SakOS Nudity Model",
            executionMode = "LiteRT",
            samplingProfileId = samplingProfile.id,
            samplingProfileLabel = samplingProfile.displayLabel,
            samplingRatio = samplingProfile.normalizedRatio,
            samplingOrientation = samplingProfile.orientation.displayLabel,
            samplingProfileSelectorEligible = samplingProfile.selectorEligible,
        )
    }

    private fun evaluateView(
        bitmap: Bitmap,
        window: SakosCompatibleCropWindow,
    ): Pair<SakosCompatibleModelScores, Long> {
        sourceRect.set(window.left, window.top, window.left + window.width, window.top + window.height)
        scratchCanvas.drawBitmap(bitmap, sourceRect, destinationRect, paint)
        scratchBitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)

        inputBuffer.clear()
        pixels.forEach { pixel ->
            val red = ((pixel shr 16) and 0xFF).toFloat()
            val green = ((pixel shr 8) and 0xFF).toFloat()
            val blue = (pixel and 0xFF).toFloat()
            inputBuffer.putFloat(blue - OpenNsfw2ModelPreflight.bgrMeanSubtraction[0])
            inputBuffer.putFloat(green - OpenNsfw2ModelPreflight.bgrMeanSubtraction[1])
            inputBuffer.putFloat(red - OpenNsfw2ModelPreflight.bgrMeanSubtraction[2])
        }
        inputBuffer.rewind()

        val startedAt = SystemClock.elapsedRealtimeNanos()
        interpreter.run(inputBuffer, output)
        val elapsedMillis = (SystemClock.elapsedRealtimeNanos() - startedAt).coerceAtLeast(0L) / 1_000_000L
        val scores = SakosCompatibleModelScores(output[0][0], output[0][1])
        require(scores.sfwProbability.isFinite() && scores.nsfwProbability.isFinite()) {
            "The model produced a non-finite result."
        }
        require(scores.sfwProbability in 0f..1f && scores.nsfwProbability in 0f..1f) {
            "The model produced a result outside the expected probability range."
        }
        require(kotlin.math.abs(scores.sfwProbability + scores.nsfwProbability - 1f) <= 0.01f) {
            "The model produced an invalid probability distribution."
        }
        return scores to elapsedMillis
    }

    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        interpreter.close()
        if (!scratchBitmap.isRecycled) scratchBitmap.recycle()
    }

    companion object {
        fun open(
            context: Context,
            threadCount: Int = 4,
            policy: IntegratedStillGatePolicyConstants = IntegratedStillGatePolicyDefaults.load(context),
        ): OpenNsfw2BitmapRuntime {
            require(threadCount > 0) { "threadCount must be positive." }
            require(policy == IntegratedStillGatePolicyDefaults.fallback) {
                "The bundled configuration requires the recorded version-1 policy."
            }
            val status = context.assets.open(OpenNsfw2ModelPreflight.assetPath).use(OpenNsfw2ModelPreflight::verify)
            check(status == OpenNsfw2ModelAssetStatus.Verified) {
                "The bundled OpenNSFW2 model did not pass integrity preflight: $status"
            }
            val interpreter = Interpreter(openModelBuffer(context), Interpreter.Options().setNumThreads(threadCount))
            try {
                check(interpreter.inputTensorCount == 1 && interpreter.outputTensorCount == 1) {
                    "The bundled model has an unexpected tensor count."
                }
                check(interpreter.getInputTensor(0).dataType() == DataType.FLOAT32 &&
                    interpreter.getOutputTensor(0).dataType() == DataType.FLOAT32) {
                    "The bundled model requires float32 tensors."
                }
                check(interpreter.getInputTensor(0).shape().contentEquals(OpenNsfw2ModelPreflight.inputShape.toIntArray())) {
                    "The bundled OpenNSFW2 model has an unexpected input tensor shape."
                }
                check(interpreter.getOutputTensor(0).shape().contentEquals(OpenNsfw2ModelPreflight.outputShape.toIntArray())) {
                    "The bundled OpenNSFW2 model has an unexpected output tensor shape."
                }
                return OpenNsfw2BitmapRuntime(interpreter, threadCount, policy)
            } catch (failure: Throwable) {
                interpreter.close()
                throw failure
            }
        }

        private fun openModelBuffer(context: Context): ByteBuffer = try {
            context.assets.openFd(OpenNsfw2ModelPreflight.assetPath).use { descriptor ->
                FileInputStream(descriptor.fileDescriptor).channel.use { channel ->
                    channel.map(FileChannel.MapMode.READ_ONLY, descriptor.startOffset, descriptor.declaredLength)
                }
            }
        } catch (_: Exception) {
            context.assets.open(OpenNsfw2ModelPreflight.assetPath).use { stream ->
                val bytes = stream.readBytes()
                ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).apply {
                    put(bytes)
                    rewind()
                }
            }
        }
    }
}
