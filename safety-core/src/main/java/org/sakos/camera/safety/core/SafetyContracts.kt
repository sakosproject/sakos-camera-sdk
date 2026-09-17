package org.sakos.camera.safety.core

/** A stable caller-supplied identifier for exactly one captured input. */
@JvmInline
value class SafetyCaptureId(val value: String) {
    init {
        require(value.isNotBlank()) { "capture ID must not be blank." }
        require(value == value.trim()) { "capture ID must not have leading or trailing whitespace." }
    }
}

/** A stable identifier for one evaluator result. */
@JvmInline
value class SafetyEvaluationReceiptId(val value: String) {
    init {
        require(value.isNotBlank()) { "evaluation receipt ID must not be blank." }
        require(value == value.trim()) { "evaluation receipt ID must not have leading or trailing whitespace." }
    }
}

/**
 * Non-sensitive metadata used to associate a result with a single captured input.
 *
 * It intentionally contains no host identity, entitlement, package signature, location,
 * account, storage URI, or model-specific image representation.
 */
data class SafetyCaptureContext(
    val captureId: SafetyCaptureId,
    val capturedAtEpochMillis: Long,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val frontFacing: Boolean,
) {
    init {
        require(capturedAtEpochMillis >= 0) { "capture timestamp must not be negative." }
        require(width > 0) { "capture width must be positive." }
        require(height > 0) { "capture height must be positive." }
        require(rotationDegrees in 0..359) { "rotation must be in 0..359 degrees." }
    }
}

/** A named, versioned part of the evaluation configuration. */
data class SafetyComponentVersion(
    val id: String,
    val version: String,
) {
    init {
        require(id.isNotBlank()) { "component ID must not be blank." }
        require(id == id.trim()) { "component ID must not have leading or trailing whitespace." }
        require(version.isNotBlank()) { "component version must not be blank." }
        require(version == version.trim()) { "component version must not have leading or trailing whitespace." }
    }
}

/**
 * The complete version identity needed to interpret a result. Model, preprocessing and policy
 * are versioned together so a score cannot be separated from the configuration that produced it.
 */
data class SafetyConfigurationVersion(
    val model: SafetyComponentVersion,
    val preprocessing: SafetyComponentVersion,
    val policy: SafetyComponentVersion,
)

/** A request whose input remains owned by the caller. */
data class SafetyEvaluationRequest<Input : Any>(
    val input: Input,
    val capture: SafetyCaptureContext,
    val configuration: SafetyConfigurationVersion,
)

/** A content evaluation decision. `Review` is unresolved and is never approval. */
enum class SafetyDecision {
    Allow,
    Block,
    Review,
}

/**
 * A fail-closed result reason. Implementations return one of these instead of turning an
 * unavailable, malformed, failed, or cancelled evaluation into an allow decision.
 */
enum class SafetyFailureReason {
    InvalidInput,
    EvaluatorUnavailable,
    EvaluatorClosed,
    ModelUnavailable,
    ModelIntegrityFailure,
    InvalidModelOutput,
    InferenceFailure,
    Cancelled,
    UnsupportedInput,
}

sealed interface SafetyEvaluationOutcome {
    val captureId: SafetyCaptureId
    val configuration: SafetyConfigurationVersion

    data class Decision(
        override val captureId: SafetyCaptureId,
        val receiptId: SafetyEvaluationReceiptId,
        override val configuration: SafetyConfigurationVersion,
        val decision: SafetyDecision,
        val rationale: List<String> = emptyList(),
    ) : SafetyEvaluationOutcome

    data class Failure(
        override val captureId: SafetyCaptureId,
        override val configuration: SafetyConfigurationVersion,
        val reason: SafetyFailureReason,
        val detail: String? = null,
    ) : SafetyEvaluationOutcome
}

/**
 * Evidence that a managed capture module may deliver output for one capture.
 *
 * The constructor is internal so SDK code obtains this only through [approvalForManagedCapture].
 * This controls the SDK-managed path; a modified host can always bypass a library API.
 */
class ManagedCaptureApproval internal constructor(
    val captureId: SafetyCaptureId,
    val receiptId: SafetyEvaluationReceiptId,
    val configuration: SafetyConfigurationVersion,
) {
    fun isFor(capture: SafetyCaptureContext): Boolean = captureId == capture.captureId
}

/** Returns approval only for an Allow result that belongs to [capture]. */
fun SafetyEvaluationOutcome.approvalForManagedCapture(
    capture: SafetyCaptureContext,
): ManagedCaptureApproval? = when (this) {
    is SafetyEvaluationOutcome.Decision -> {
        if (decision == SafetyDecision.Allow && captureId == capture.captureId) {
            ManagedCaptureApproval(
                captureId = captureId,
                receiptId = receiptId,
                configuration = configuration,
            )
        } else {
            null
        }
    }

    is SafetyEvaluationOutcome.Failure -> null
}

/**
 * The evaluator contract for existing-camera and managed-capture integrations.
 *
 * Inputs remain caller-owned. An evaluator must not retain the input after this call completes
 * unless a future API explicitly transfers ownership. Cancellation is terminal for the result:
 * implementations must not deliver Allow after cancellation, and callers must not promote output
 * when evaluation is cancelled or throws cancellation.
 */
interface SafetyEvaluator<Input : Any> : AutoCloseable {
    suspend fun evaluate(request: SafetyEvaluationRequest<Input>): SafetyEvaluationOutcome

    override fun close()
}

/** A reusable evaluator for configuration or runtime states that cannot perform evaluation. */
class UnavailableSafetyEvaluator<Input : Any>(
    private val unavailableReason: SafetyFailureReason = SafetyFailureReason.EvaluatorUnavailable,
    private val detail: String? = null,
) : SafetyEvaluator<Input> {
    init {
        require(unavailableReason != SafetyFailureReason.Cancelled) {
            "Cancelled is an evaluation outcome, not an evaluator availability state."
        }
    }

    @Volatile
    private var closed = false

    override suspend fun evaluate(request: SafetyEvaluationRequest<Input>): SafetyEvaluationOutcome.Failure =
        SafetyEvaluationOutcome.Failure(
            captureId = request.capture.captureId,
            configuration = request.configuration,
            reason = if (closed) SafetyFailureReason.EvaluatorClosed else unavailableReason,
            detail = detail,
        )

    override fun close() {
        closed = true
    }
}
