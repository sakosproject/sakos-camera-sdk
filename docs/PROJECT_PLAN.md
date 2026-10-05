# SakOS Camera SDK implementation plan

## Expanded acceptance — camera tooling and strategy execution, 2026-10-01

The owner's newer instruction reopens local completion after `d1f24fe`. Keep the
same worktree/branch and all private/synthetic-only restrictions above.

| Work | Concrete scope | Acceptance gate | Status |
| --- | --- | --- | --- |
| E. Source inventory | Allowlisted camera/runtime/gallery Kotlin code; current source revision and snapshot; reusable versus app integrations | Pin source evidence and inventory graph, selector, zoom, focus, orientation, profile and first-run behavior before code edits | Inventoried in TOOLING_PARITY; snapshots pinned |
| F. Live strategies | Pure score driver and Bitmap runtime; Fixed14 default, optional Adaptive14; evaluator identity | Deterministic early exits, portrait/sentinel/refinement/fallback/duplicate/invalid tests; both strategies on synthetic Bitmap and minified consumer | Complete for authorized local scope; both strategies and clean repeat passed |
| G. Camera tooling | capture-camerax discovery, source graph plans, actual probes, selection, secure no-backup profile/cache identity, retries/cancellation; minimal sample lens/quality/zoom/focus/rotation wiring | Mocked capability matrices and lifecycle failures; first-run/retry gates; actual isolated emulator probes; no probe output promotion; front/back/recreation checks | Complete for authorized local scope; JVM and isolated emulator gates passed |
| H. Corrected candidate | All relevant docs/site/examples, local artifacts/notices/audit/checksums and repeat verifier | Full clean JVM/debug/lint and synthetic Android tests; Maven/minified consumer; exact model hash; truthful deferred validation and hardware/efficacy/legal gates | Complete for local scope; clean 0ad514d candidate, repaired focused Android suite and exact current consumer validated; see BUILD_NOTES |
| I. Standalone reviewed gallery core | Source camera repositories/provider and gallery client/repository/exporter; reusable approved private library and caller-authorized transactional destination | Capture/configuration-bound approval, no private intermediate exposure, cancel/failure cleanup, approved-only inventory/open/export; mocked save failures and synthetic Android integration without host application | Complete for authorized local scope; private save/playback and authorization passed |
| J. Final ownership review | Probe-contract validation and unconditional runner unlock; shared-root private write ownership; shared playback leases; truthful committed-export cleanup results | Deterministic malformed/retry/cleanup/concurrent ownership and post-commit failure tests; two-client synthetic Android playback; repeat full clean candidate | Complete for local scope; ownership tests, repaired focused Android suite and exact current consumer passed |

Local narrow commits remain authorized. Advertised capabilities never alone make
a graph usable. Calibration tests establish mechanics only; no universal phone
compatibility, model quality or real-world efficacy is inferred.

## Expanded completion record - 2026-10-01

Acceptance for items E through J is complete for the authorized local scope. The full candidate
verifier and same-source clean repeat passed at `6d37cb8`: 185 JVM tests, 12
synthetic Android tests, zero lint errors (43 warnings), 24 inspected artifacts,
current text/permissions/notices/model/link checks and identical 16 Maven
outputs. Only the test-key localRuntime APK differed among all 24 hashes.
BUILD_NOTES records commands, limits and the resolved atomic commit failure.
Final ledger/inspection changes affect no artifact implementation. Independent
validation remains deferred/unverified; owner/legal, physical/API-range,
real-world efficacy, intake and every external-action gate remain unresolved.

## Follow-up: one physical-device camera flow - 2026-10-02

The separately authorized live-camera flow passed on a Samsung Galaxy S20 FE
(SM-G781W, Android 13/API 33). It exercised first-run calibration, actual
rear/front photo and video capture, selected controls, activity recreation,
cancellation and private save/delete using only operator-approved office-floor
and ceiling views. Test captures remained on-device and were deleted.

This is a bounded camera-workflow mechanics result on one handset. It does not
close broad physical-camera/API-range/OEM coverage, sensor-rotation or focus-
sharpness checks, independent validation, or classifier efficacy. See
`docs/CAMERA_TOOLING.md` for the complete sanitized record. The earlier Phase 7
"deferred until a device is connected" status below is superseded by this
limited follow-up, not by a broad device-compatibility claim.
## Candidate repeatability repair — 2026-10-01

An interrupted synthetic sample instrumentation run can leave only the sample
app's generated no-backup test state behind. The test now removes that state
before and after every case and explicitly revokes its own Camera permission;
the runner treats an already-absent sample package as a successful reset while
still failing other uninstall errors, and retries only a transient local ADB
daemon disconnect during test-APK installation. This does not touch device
media, a private corpus, or any external service. The prior two clean implementation-gate runs remain valid. The latest clean
non-emulator candidate and repaired focused source Android suite passed. After
the existing tunnel was restored, the exact current consumer APK passed its
one synthetic runtime test. All 16 Maven files remain byte-identical to the
verified implementation baseline. The owner requested avoiding redundant
whole-suite rebuilds; no additional whole-serial verifier run is claimed.

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

## Earlier implementation milestones (prior candidate)

| Phase | Outcome | Status |
| --- | --- | --- |
| 1. Scope and provenance | Independent SDK boundary, source records, notices, and project-owner authorization | Complete |
| 2. Safety contracts and policy | Capture-bound decisions, model configuration, and spatial policy | Complete |
| 3. Bundled model runtime | Verified asset, LiteRT Bitmap runtime, and fail-closed evaluator | Complete for local verification |
| 4. Photo capture bridge | In-memory CameraX callback to a caller-owned approved-output boundary | Complete for local verification |
| 5. Video capture bridge | Private staging, CameraX recording/finalization boundary, and temporal review seam | Complete for local verification |
| 6. Independent validation | Deferred; completion and results are unverified | Deferred |
| 7. Physical-device verification | One opt-in camera mechanics flow on Galaxy S20 FE / Android 13 | Single-handset flow passed; broader API/OEM/camera coverage remains open |
| 8. Release preparation | Private local candidate artifacts and evidence; external decisions remain gated | Local candidate preparation complete |
| 9. Public launch | Any external release, publication, or website deployment | Not started |

No private validation material, identifying paths, outputs, or process details
belong in this repository.

## Earlier candidate completion record — 2026-10-01

The earlier four completion phases passed before the expanded acceptance above.
This dated snapshot is superseded by the expanded work. The complete repeat verifier
ran at clean commit `e76718e`: 94 JVM tests, six synthetic Android tests, zero
lint errors, local Maven/notices/permissions inspection and separate minified
consumer runtime integration. All 16 Maven deliverables matched the earlier
complete candidate byte-for-byte. Of 24 inventoried artifacts, 23 matched;
the test-key localRuntime APK differed and its hash was recorded with that candidate.
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

**Later physical gate:** the isolated emulator covers synthetic lifecycle and
orientation mechanics. Physical-camera verification needs separate authorization
and an owner-connected device.

## Phase 5 — video capture bridge

**Completed record:** App-private no-backup staging, recovery cleanup,
recording output preparation, and finalization-to-review state transitions are
implemented. Error finalization discards the staged session; only a clean
finalization reaches the injected review seam. Local verification is recorded
in `docs/BUILD_NOTES.md`.

**Later physical gate:** isolated-emulator recording, staging, cleanup, recovery
and approved-output mechanics are recorded in BUILD_NOTES. Physical-camera
verification still needs separate authorization and an owner-connected device.

## Phase 6 — independent validation

Independent validation is deferred; completion and results are unverified here.
If undertaken, it must remain private and outside this repository. Do not add input
material, identifiers, paths, outputs, descriptions of procedures, or results
to the SDK repository.

## Phase 7 — physical-device verification

Run only after the project owner connects an Android device. Record the
device-facing completion status without attaching private captures or their
details. Keep any supporting material outside this repository.

## Phase 8 — release preparation

Private local candidate compilation, tests, Maven artifacts, checksums and
sanitized evidence are authorized by the expanded scope above. External release
still requires cleared rights/notices, owner-approved version/destinations,
verified intake and explicit action-specific authorization. No external action
or real release signing is authorized by this plan.

## Phase 9 — public launch

This phase requires a separate explicit instruction naming each external
action. It is not authorized by implementation or release preparation.

## Historical Terra / High handoff (superseded)

> Work only on the current phase in `docs/PROJECT_PLAN.md`. Use Terra with High
> reasoning. Inspect the current checkout first, preserve unrelated changes,
> update the plan with the result, run the phase checks, run `git diff --check`,
> and create one narrow commit. Do not begin a later phase, publish anything,
> or add private validation material or its details to the repository.
