package org.sakos.camera.safety.opennsfw2

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.sakos.camera.safety.core.*

/** Runtime mechanics only: generated geometric inputs make no classifier accuracy claim. */
@RunWith(AndroidJUnit4::class)
class SyntheticRuntimeTest {
    @Test fun bundledAssetTensorContractInferenceAndClosedFailure() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(OpenNsfw2ModelAssetStatus.Verified, context.assets.open(OpenNsfw2ModelPreflight.assetPath).use(OpenNsfw2ModelPreflight::verify))
        val bitmap = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLUE)
        canvas.drawRect(20f, 20f, 180f, 180f, Paint().apply { color = Color.GREEN })
        val runtime = OpenNsfw2BitmapRuntime.open(context, threadCount = 1)
        val evaluator = OpenNsfw2BitmapEvaluator(runtime)
        try {
            val result = runtime.evaluate(bitmap)
            assertTrue(result.evaluatedViews.isNotEmpty())
            result.evaluatedViews.forEach {
                assertTrue(it.scores.sfwProbability.isFinite())
                assertTrue(it.scores.nsfwProbability in 0f..1f)
            }
            val capture = SafetyCaptureContext(SafetyCaptureId("synthetic-runtime"), 0, 320, 240, 0, false)
            val request = SafetyEvaluationRequest(bitmap, capture, OpenNsfw2ModelPreflight.configuration)
            val wrong = request.copy(configuration = request.configuration.copy(model = SafetyComponentVersion("wrong", "1")))
            assertTrue(evaluator.evaluate(wrong) is SafetyEvaluationOutcome.Failure)
            assertTrue(evaluator.evaluate(request) is SafetyEvaluationOutcome.Decision)
            evaluator.close()
            assertEquals(SafetyFailureReason.EvaluatorClosed, (evaluator.evaluate(request) as SafetyEvaluationOutcome.Failure).reason)
            evaluator.close()
        } finally { evaluator.close(); bitmap.recycle() }
    }
}
