# SakOS Camera SDK implementation plan

## Authorized local completion — 2026-10-01

The owner's current instruction supersedes the former Terra/High and phase-stop
handoff. Continue all remaining authorized local work on
`codex/phase-7-finalization-bridge`, starting clean at `853f651`. Use only
gpt-6.1-sol at high reasoning or lower, without agents. Local narrow commits are
authorized. No merge, push, public publication, tag, upload, deployment, DNS,
visibility, signing, physical-device access, VM changes, or security changes.

Only synthetic benign patterns/shapes, mocks, simulated scores and an explicitly
provided isolated emulator may be used. Do not read a media corpus. Independent
validation remains outside the repository. Preserve provenance and notices;
synthetic runtime checks do not establish accuracy, parity or redistribution rights.

| Local completion phase | Scope and files | Acceptance gate | Status |
| --- | --- | --- | --- |
| A. Managed Android bridges | capture-camerax close/approval/configuration; capture-video Android decoder/evaluator, cancellation, cleanup and staging recovery; safety-opennsfw2 runtime checks | Relevant JVM failure/ownership tests; instrumented synthetic decoder/runtime tests compile; no failed or cancelled review promotes | Local implementation gate passed |
| B. Functional sample | sample-app permission, CameraX preview/lifecycle, in-memory photo, silent private video, approved-only private viewer and recovery | Build and UI/instrumented coverage; only matching Allow reaches approved storage; background cancellation cleans staging | Passed on isolated synthetic emulator |
| C. Private candidate verification | scripts, local Maven AAR/source/POM/notices, separate minified consumer and synthetic runtime integration | Clean debug build, all JVM tests, lint, instrumented APKs; supplied emulator checks; repeat script succeeds and hashes artifacts | Passed including clean-commit repeat |
| D. Sanitized evidence and docs | README, docs, website, changelog, candidate manifest and private text audit | Claims match executed checks; historical contrary textual findings only in private output; unresolved owner/legal/physical/efficacy gates explicit | Complete for authorized local scope |

Each phase records exact checks in BUILD_NOTES and commits locally. Missing
emulator or external gates do not stop independent implementation and packaging.
The records below retain earlier outcomes; their old stop instructions and
release-preparation restrictions are historical and superseded for local work.

## Earlier implementation milestones (updated local status)

| Phase | Outcome | Status |
| --- | --- | --- |
| 1. Scope and provenance | Independent SDK boundary, source records, notices, and project-owner authorization | Complete |
| 2. Safety contracts and policy | Capture-bound decisions, model configuration, and spatial policy | Complete |
| 3. Bundled model runtime | Verified asset, LiteRT Bitmap runtime, and fail-closed evaluator | Complete for local verification |
| 4. Photo capture bridge | In-memory CameraX callback to a caller-owned approved-output boundary | Complete for local verification |
| 5. Video capture bridge | Private staging, CameraX recording/finalization boundary, and temporal review seam | Complete for local verification |
| 6. Independent validation | Private work performed outside this repository | Deferred |
| 7. Physical-device verification | Camera, storage, cleanup, and recovery verification | Deferred until a device is connected |
| 8. Release preparation | Private local candidate artifacts and evidence; external decisions remain gated | Local candidate preparation complete |
| 9. Public launch | Any external release, publication, or website deployment | Not started |

No private validation material, identifying paths, outputs, or process details
belong in this repository.

## Completion record — 2026-10-01

All four authorized local completion phases passed. The complete repeat verifier
ran at clean commit `e76718e`: 94 JVM tests, six synthetic Android tests, zero
lint errors, local Maven/notices/permissions inspection and separate minified
consumer runtime integration. All 16 Maven deliverables matched the earlier
complete candidate byte-for-byte. Of 24 inventoried artifacts, 23 matched;
the test-key localRuntime APK differed and its exact current hash is recorded.
No universal signed-APK reproducibility is claimed. See BUILD_NOTES and the
ignored private candidate manifest. The final ledger-only commit changes no
artifact code. Owner/legal, physical/API-range, real-world efficacy,
independent-validation, intake and all external-action gates remain unresolved.

## Historical Phase 3 — bundled model runtime

**Scope:** Keep the authorized model only in `safety-opennsfw2`. Verify its
checksum before LiteRT opens it. Run the documented BGR preprocessing and the
existing fixed spatial policy over caller-owned `Bitmap` inputs. Return only
SDK safety outcomes; do not write, upload, retain, or expose capture input.

**Files:** `safety-opennsfw2/src/main/assets/model/`,
`OpenNsfw2BitmapRuntime.kt`, `OpenNsfw2BitmapEvaluator.kt`,
`OpenNsfw2ModelPreflight.kt`, model/runtime documentation, and focused tests.

**Acceptance gate:** the bundled asset matches the recorded SHA-256; the
runtime compiles against LiteRT; malformed or unavailable runtime states are
non-approving; policy/model/preprocessing identity is versioned together; the
full clean debug build and diff check pass.

**Progress record — 2026-09-17:** The project owner authorized import from the
local camera/gallery application source. The source provenance record identifies
`sakos_nudity_model.tflite`, its OpenNSFW2/Yahoo lineage, size, checksum, and
retained notices. The exact 6,128,536-byte asset was copied unchanged to the
module. A LiteRT Bitmap runtime and fail-closed `SafetyEvaluator` adapter were
added. Focused module verification, common clean-build verification, and final
repository review pass.

**Stop condition:** do not represent local compilation as physical-device
verification. Phase 7 remains deferred until the project owner connects a
device.

## Phase 4 — photo capture bridge

**Completed record:** `ManagedPhotoCaptureCallback` bridges CameraX in-memory
success and failure events to the existing close-safe review pipeline. The
adapter owns no output location, model input retention, permission, metadata,
or network behavior. Local verification is recorded in `docs/BUILD_NOTES.md`.

**Later gate:** perform real camera lifecycle and orientation verification only
after a device is connected.

## Phase 5 — video capture bridge

**Completed record:** App-private no-backup staging, recovery cleanup,
recording output preparation, and finalization-to-review state transitions are
implemented. Error finalization discards the staged session; only a clean
finalization reaches the injected review seam. Local verification is recorded
in `docs/BUILD_NOTES.md`.

**Later gate:** perform actual recording, staging, cleanup, recovery, and
approved-output verification only after a device is connected.

## Phase 6 — independent validation

This work remains private and outside this repository. Do not add input
material, identifiers, paths, outputs, descriptions of procedures, or results
to the SDK repository.

## Phase 7 — physical-device verification

Run only after the project owner connects an Android device. Record the
device-facing completion status without attaching private captures or their
details. Keep any supporting material outside this repository.

## Phase 8 — release preparation

Before any release work, establish the artifact identity, notice inventory,
security intake, package destination, website destination, and owner sign-off.
No release output or external action is authorized by this plan.

## Phase 9 — public launch

This phase requires a separate explicit instruction naming each external
action. It is not authorized by implementation or release preparation.

## Historical Terra / High handoff (superseded)

> Work only on the current phase in `docs/PROJECT_PLAN.md`. Use Terra with High
> reasoning. Inspect the current checkout first, preserve unrelated changes,
> update the plan with the result, run the phase checks, run `git diff --check`,
> and create one narrow commit. Do not begin a later phase, publish anything,
> or add private validation material or its details to the repository.
