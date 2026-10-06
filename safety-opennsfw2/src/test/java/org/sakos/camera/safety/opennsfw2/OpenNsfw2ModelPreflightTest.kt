package org.sakos.camera.safety.opennsfw2

import java.io.ByteArrayInputStream
import java.io.File
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.sakos.camera.safety.core.SafetyCaptureContext
import org.sakos.camera.safety.core.SafetyCaptureId
import org.sakos.camera.safety.core.SafetyEvaluationRequest
import org.sakos.camera.safety.core.SafetyFailureReason
import org.sakos.camera.safety.core.approvalForManagedCapture

class OpenNsfw2ModelPreflightTest {
    private val capture = SafetyCaptureContext(
        captureId = SafetyCaptureId("preflight-capture"),
        capturedAtEpochMillis = 1L,
        width = 1080,
        height = 1920,
        rotationDegrees = 0,
        frontFacing = false,
    )

    private val request = SafetyEvaluationRequest(
        input = "opaque-input",
        capture = capture,
        configuration = OpenNsfw2ModelPreflight.configuration,
    )

    @Test
    fun missingAssetIsReportedAsUnavailableAndCannotApprove() {
        val status = OpenNsfw2ModelPreflight.verify(null)
        assertEquals(OpenNsfw2ModelAssetStatus.Missing, status)

        val evaluator = OpenNsfw2ModelPreflightEvaluator<String> { status }
        val outcome = runSuspend { evaluator.evaluate(request) }
        assertEquals(SafetyFailureReason.ModelUnavailable, outcome.reason)
        assertNull(outcome.approvalForManagedCapture(capture))
    }

    @Test
    fun malformedAssetsFailIntegrityPreflight() {
        val shortAsset = OpenNsfw2ModelPreflight.verify(ByteArrayInputStream(byteArrayOf(1, 2, 3)))
        assertIs<OpenNsfw2ModelAssetStatus.UnexpectedSize>(shortAsset)

        val expectedSizeButWrongDigest = OpenNsfw2ModelPreflight.verify(
            ByteArrayInputStream(ByteArray(OpenNsfw2ModelPreflight.expectedByteCount.toInt())),
        )
        assertIs<OpenNsfw2ModelAssetStatus.HashMismatch>(expectedSizeButWrongDigest)

        val evaluator = OpenNsfw2ModelPreflightEvaluator<String> { expectedSizeButWrongDigest }
        val outcome = runSuspend { evaluator.evaluate(request) }
        assertEquals(SafetyFailureReason.ModelIntegrityFailure, outcome.reason)
        assertNull(outcome.approvalForManagedCapture(capture))
    }

    @Test
    fun closeMakesThePreflightEvaluatorNonApproving() {
        val evaluator = OpenNsfw2ModelPreflightEvaluator<String> {
            OpenNsfw2ModelAssetStatus.Verified
        }
        evaluator.close()

        val outcome = runSuspend { evaluator.evaluate(request) }
        assertEquals(SafetyFailureReason.EvaluatorClosed, outcome.reason)
        assertNull(outcome.approvalForManagedCapture(capture))
    }

    @Test
    fun contractKeepsModelPreprocessingAndPolicyIdentityTogether() {
        assertEquals("opennsfw2_resnet50_v1", OpenNsfw2ModelPreflight.configuration.model.id)
        assertEquals("bea35dc93c86f074", OpenNsfw2ModelPreflight.configuration.model.version)
        assertEquals(listOf(1, 224, 224, 3), OpenNsfw2ModelPreflight.inputShape)
        assertEquals(listOf(1, 2), OpenNsfw2ModelPreflight.outputShape)
        assertEquals(listOf(104, 117, 123), OpenNsfw2ModelPreflight.bgrMeanSubtraction)
    }

    @Test
    fun bundledAssetPassesTheDeclaredIntegrityPreflight() {
        val asset = File("src/main/assets/${OpenNsfw2ModelPreflight.assetPath}")

        assertTrue(asset.isFile)
        assertEquals(
            OpenNsfw2ModelAssetStatus.Verified,
            asset.inputStream().use(OpenNsfw2ModelPreflight::verify),
        )
    }

    private fun <T> runSuspend(block: suspend () -> T): T {
        var result: Result<T>? = null
        block.startCoroutine(
            object : Continuation<T> {
                override val context = EmptyCoroutineContext

                override fun resumeWith(value: Result<T>) {
                    result = value
                }
            },
        )
        return requireNotNull(result) { "The preflight evaluator must complete synchronously." }.getOrThrow()
    }
}
