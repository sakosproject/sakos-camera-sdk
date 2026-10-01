# Safety core API contract

`safety-core` is independent of CameraX, Compose, LiteRT and any host product.
It describes evaluation results and whether a managed capture module may
deliver output. It also supplies a transactional approval-bound private library
and caller-authorized export contracts. It does not classify content, inspect
host trust or provide a user interface. See REVIEWED_LIBRARY for save ownership.

## Inputs and ownership

`SafetyEvaluationRequest<Input>` pairs caller-owned opaque input with a
`SafetyCaptureContext`. The context uses one stable capture ID, dimensions,
rotation, camera-facing flag and timestamp. It deliberately excludes account,
location, package identity, signature/entitlement state, storage URI and
model-specific pixels/tensors.

The caller owns the input before and after `SafetyEvaluator.evaluate`.
Implementations must not retain it after completion unless a later explicit API
adds an ownership transfer. A caller that receives cancellation must treat it
as terminal and must not publish an output from that evaluation.

## Configuration identity

Each result carries `SafetyConfigurationVersion`, which binds together named
model, preprocessing and policy versions. Scores and decisions must be
interpreted only with that complete identity. The core intentionally contains
no model name, tensor contract, threshold, crop strategy or policy constant.

## Outcomes and managed delivery

There are three content decisions:

- `Allow`: the only decision that can create `ManagedCaptureApproval`.
- `Block`: no managed output may be delivered.
- `Review`: unresolved; no managed output may be delivered. It does not create
  a parent-review retention workflow.

Malformed input, unavailable/closed evaluator, missing/corrupt model,
invalid model output, inference failure, cancellation and unsupported input
are `SafetyEvaluationOutcome.Failure` values. They cannot produce an approval.

`approvalForManagedCapture(context)` returns a token only when an `Allow`
receipt belongs to the same capture ID. Capture modules must require that token
before writing or promoting an SDK-managed output. The token gives an SDK
workflow guarantee; a modified host application can bypass a library API.

## Lifecycle and host authorization

`SafetyEvaluator` is closeable and has a suspending `evaluate` operation.
Implementations must release their owned resources on `close` and must never
deliver Allow after cancellation. `UnavailableSafetyEvaluator` is the baseline
fail-closed evaluator for missing runtime/model configuration.

Host authorization belongs outside content classification. This independent
SDK has no host application presence, signature, entitlement, managed-session or
provider-state contract, and it does not synthesize an equivalent trust
snapshot.
