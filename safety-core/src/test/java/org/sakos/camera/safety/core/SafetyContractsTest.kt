package org.sakos.camera.safety.core

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SafetyContractsTest {
    private val configuration = SafetyConfigurationVersion(
        model = SafetyComponentVersion("test-model", "1"),
        preprocessing = SafetyComponentVersion("test-preprocessing", "1"),
        policy = SafetyComponentVersion("test-policy", "1"),
    )

    private val firstCapture = SafetyCaptureContext(
        captureId = SafetyCaptureId("capture-a"),
        capturedAtEpochMillis = 1,
        width = 1920,
        height = 1080,
        rotationDegrees = 90,
        frontFacing = false,
    )

    @Test
    fun invalidCaptureAndVersionValuesCannotConstructRequests() {
        assertFailsWith<IllegalArgumentException> { SafetyCaptureId(" ") }
        assertFailsWith<IllegalArgumentException> {
            firstCapture.copy(width = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            firstCapture.copy(rotationDegrees = 360)
        }
        assertFailsWith<IllegalArgumentException> {
            SafetyComponentVersion("model", " ")
        }
    }

    @Test
    fun onlyAllowForTheSameCaptureProducesManagedApproval() {
        val allowed = SafetyEvaluationOutcome.Decision(
            captureId = firstCapture.captureId,
            receiptId = SafetyEvaluationReceiptId("receipt-a"),
            configuration = configuration,
            decision = SafetyDecision.Allow,
        )

        val approval = assertNotNull(allowed.approvalForManagedCapture(firstCapture))
        assertTrue(approval.isFor(firstCapture))
        assertEquals(SafetyEvaluationReceiptId("receipt-a"), approval.receiptId)

        val otherCapture = firstCapture.copy(captureId = SafetyCaptureId("capture-b"))
        assertFalse(approval.isFor(otherCapture))
        assertNull(allowed.approvalForManagedCapture(otherCapture))
    }

    @Test
    fun blockReviewAndFailureNeverProduceManagedApproval() {
        val blocked = outcome(SafetyDecision.Block)
        val review = outcome(SafetyDecision.Review)
        val cancelled = SafetyEvaluationOutcome.Failure(
            captureId = firstCapture.captureId,
            configuration = configuration,
            reason = SafetyFailureReason.Cancelled,
        )

        assertNull(blocked.approvalForManagedCapture(firstCapture))
        assertNull(review.approvalForManagedCapture(firstCapture))
        assertNull(cancelled.approvalForManagedCapture(firstCapture))
    }

    @Test
    fun unavailableEvaluatorFailsClosedBeforeAndAfterClose() {
        val evaluator = UnavailableSafetyEvaluator<String>(
            unavailableReason = SafetyFailureReason.ModelUnavailable,
            detail = "model asset was not installed",
        )
        val request = SafetyEvaluationRequest(
            input = "opaque-input",
            capture = firstCapture,
            configuration = configuration,
        )

        val unavailable = runSuspend { evaluator.evaluate(request) }
        assertEquals(SafetyFailureReason.ModelUnavailable, unavailable.reason)
        assertNull(unavailable.approvalForManagedCapture(firstCapture))

        evaluator.close()
        val closed = runSuspend { evaluator.evaluate(request) }
        assertEquals(SafetyFailureReason.EvaluatorClosed, closed.reason)
        assertNull(closed.approvalForManagedCapture(firstCapture))
    }

    private fun outcome(decision: SafetyDecision): SafetyEvaluationOutcome.Decision =
        SafetyEvaluationOutcome.Decision(
            captureId = firstCapture.captureId,
            receiptId = SafetyEvaluationReceiptId("receipt-${decision.name.lowercase()}"),
            configuration = configuration,
            decision = decision,
        )

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
        return requireNotNull(result) { "The test evaluator must complete synchronously." }.getOrThrow()
    }
}
