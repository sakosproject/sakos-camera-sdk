# SakOS Camera SDK project plan

Status: foundation complete; phased implementation has not started. Revised 2026-09-17 for one-phase-at-a-time execution with **Terra / High**.

This is the authoritative implementation plan. Updating it does not authorize extraction, publication, deployment, or production changes. Select one phase explicitly when starting implementation; stop after its gate and execution record.

## Purpose

Build an independent Android SDK and sample app under SakOS (Safe Kids OS), preserving the existing capture-review helpers, adaptive image sampling, and temporal video review developed for camera/gallery application. Consumers must not need host application.

## Agreed direction

- Repository: `sakosproject/sakos-camera-sdk`, private during preparation, intended for a reviewed public launch.
- Website: `https://sakosproject.org`, with a SakOS homepage, `/camera/` product page, and `/docs/camera/` developer documentation.
- Deliver an SDK plus sample app with both photo and video support.
- Use Apache-2.0 for project-owned material and retain separate upstream licenses and notices.
- Bundle offline inference; require no account, runtime download, Firebase, or host application installation.
- Keep the existing host application product separate during extraction.

## Planned modules

| Module | Responsibility |
| --- | --- |
| `safety-core` | Evaluation contracts, versioned policy, decisions, and failure reasons |
| `safety-opennsfw2` | Model adapter, preprocessing, spatial sampling, evidence aggregation, and LiteRT runtime |
| `capture-camerax` | In-memory photo capture and approved-only output |
| `capture-video` | Private recording, temporal review, promotion, and cleanup |
| `sample-app` | Camera UI and minimal approved-media viewer |

Keep UI optional for library consumers. Offer an evaluation-only integration and a managed capture integration; only the managed pipeline can make SDK-controlled storage guarantees.

## Preserve and verify the existing work

- Preserve fixed and adaptive crop strategies, portrait-specific checks, corroborated evidence, early exits, runtime buffer reuse, and video sampling/escalation logic.
- Move host authorization outside content classification; do not replace host application checks with fake trusted host application state.
- Compare decisions with the source implementation on the same authorized corpus before changing thresholds or strategies.
- Keep model-specific preprocessing and thresholds versioned together. Alternative models require separate validation.
- Retain portable benchmark tooling and regression tests without copying private media or machine-specific configuration.

## Storage contract

- Photos: evaluate in memory before saving; no SDK-written image file for rejected captures.
- Video: use private temporary disk storage, expose only approved clips, and delete rejected, failed, or cancelled clips and sidecars.
- Purge abandoned video sessions before accepting new recordings. Report cleanup failures and block new recording until resolved.
- Exclude staging from backups, galleries, thumbnails, and export paths.
- Do not claim disk-free video, guaranteed forensic erasure, perfect detection, or enforcement over other apps or a modified host.
- Review unresolved/parent-review outcomes without retaining rejected media by default; a parent review product flow is outside the initial scope.

## Execution rules for Terra / High

1. Use this plan, the selected phase, and its prerequisite evidence. Do not restart discovery or implement later phases opportunistically. Configure the task to Terra with High reasoning; this document does not switch the active model.
2. Verify the actual working directory, `git status --short --branch`, HEAD, and applicable `AGENTS.md` before work. The saved project is `local workstation path omitted`; isolated worktrees can have different paths. This planning worktree is `local workstation path omitted`, initially detached at `651b05f`. Before implementation, create a `codex/` branch if still detached and no existing implementation branch is designated; do not move the user's saved checkout.
3. Keep `local workstation path omitted` and `local workstation path omitted` read-only. Do not run source-repository builds, edit source documents, or reset source changes. Do not copy either repository wholesale or import its Git history.
4. Before code changes in each phase, append the exact source-to-target file list, intended behavior, tests, and acceptance criteria to this plan. Proposed target names below are defaults, not claims that those files exist. If inspection reveals additional dependencies, document the smallest required additions before editing.
5. Import only files or explicitly identified code sections cleared by Phase 1. Preserve attribution and record adaptations. Never copy signing files, production Firebase configuration, reviewer credentials, private media, internal screenshots/logs, machine settings, or unrelated resources. Do not print secrets in logs.
6. Preserve the existing default classification behavior. Separate required changes (host authorization removal, fail-closed error handling, video cleanup) from numerical/model changes. Do not tune thresholds, replace the model, simplify adaptive sampling, or upgrade the toolchain during extraction.
7. Run the selected phase's focused checks and the common verification below. Append results after every implementation turn, including partial work and blockers. Mark a phase complete only when its gate is met; unavailable device/corpus evidence stays pending.
8. Stop at the selected phase boundary. If a prerequisite, redistribution right, or safety behavior cannot be established, finish independent in-scope work, record the blocker, and stop dependent work. Do not manufacture rights, parity evidence, scores, or successful checks.
9. Commit, push, tag, change repository visibility, publish packages, deploy the website, change DNS, and migrate production only when explicitly authorized. Local Maven verification in Phase 11 is an internal test, not authorization to publish remotely.
10. Preserve unrelated user changes. Recovery means reverting only the current phase's attributable edits after inspecting the diff; never blanket-reset or clean the worktree. Video runtime recovery is a separate implementation requirement in Phase 7B.

## Verified planning baseline and source map

Read-only planning inspection on 2026-09-17 found:

- SDK HEAD `651b05f5289a0ecaf58829bdf691c6bf06c94724`, clean before this revision and equal to the local `origin/main` reference. No remote fetch or live visibility audit was performed.
- Source HEAD `historical source revision omitted`; existing modifications to `docs/BUILD_NOTES.md` and `scripts/build-play-release-bundles-with-reviewer-token.ps1`. Recheck before any future source reads/imports.
- Source `ai-nudity-gate/build.gradle.kts`, `app-camera/build.gradle.kts`, `gradle/libs.versions.toml`, and `gradle/wrapper/gradle-wrapper.properties` confirm compile SDK 36, app target SDK 36, min SDK 26, AGP 8.13.2, Kotlin 2.0.21, Java bytecode 11, Gradle 8.13, CameraX 1.5.3, and LiteRT 1.4.2. Preserve these values initially. Verify the compatible Gradle execution JDK separately; Java bytecode 11 does not mean running Gradle on JDK 11.
- `NudityGateContract.kt` still contains `RegaCaptureTrustSnapshot`; `SakosCompatibleGatePolicy.kt` mixes trust checks with content policy and exposes a legacy fail-open option. The independent SDK must separate these responsibilities and fail closed on invalid/missing results.
- `VideoGatePolicy.kt` still caps decoded samples at 35 and uses a 30-second base interval. `VideoGateEvaluationRunner.kt` still contains `singleUncorroboratedNonExtremeBlockAllow`. Characterize these before adapting them.
- Source `docs/UPSTREAM_PROVENANCE.md` describes private SakOS imports and license declarations. It is a lead for Phase 1, not verified clearance for redistribution.

Paths below are relative to `local workstation path omitted`. Phase 1 must pin exact source revisions/hashes and expand this candidate map into an approved file/section allowlist.

| Source area | Candidate material | Destination / treatment |
| --- | --- | --- |
| `private source path omitted` | `NudityGateContract.kt`, `SakosCompatibleGatePolicy.kt` | Split generic contracts into `safety-core`; retain model-specific thresholds/policy in `safety-opennsfw2`; remove host application trust integration |
| Same directory | `SakosCompatibleModelContract.kt`, `SakosCompatibleScoreMapping.kt`, `SakosCompatibleMultiCropEvaluation.kt`, `GateSamplingProfile.kt`, `IntegratedOpenNsfw2Strategy.kt`, `IntegratedNudityRuntimeProfile.kt`, `IntegratedStillGatePolicyDefaults.kt` | `safety-opennsfw2`, preserving sampling and policy behavior |
| Same directory | `LiveSakosNudityRuntime.kt` | `safety-opennsfw2` runtime, preprocessing, inference ownership and cleanup |
| `private source path omitted` | `policy/opennsfw2_still_gate_policy.json`, `model/sakos_nudity_model.tflite` | Import only after exact-file clearance; keep policy/model identity linked |
| `private source path omitted` | `SakosCompatibleScoreMappingTest.kt`, `SakosCompatibleMultiCropEvaluationTest.kt`, `RegaAppCaptureGateTest.kt` | Preserve relevant regression cases; distinguish content-policy cases from intentionally removed host-trust requirements |
| `private host-app source file omitted` | `SharedGateEvaluationRunner.kt`, `ReviewedCaptureRepository.kt`, selected photo sections in `CameraActivity.kt` | `capture-camerax`; extract small orchestration units, never copy the entire Activity |
| Same directory | `VideoTemporalSamplePlanner.kt`, `VideoGatePolicy.kt`, `VideoGateEvaluationRunner.kt` | `capture-video` temporal planning and aggregation |
| Same directory | `TemporaryVideoCaptureRepository.kt`, `ReviewedVideoRepository.kt`, selected recording sections in `CameraActivity.kt` | `capture-video` lifecycle; add explicit non-approved cleanup and abandoned-session recovery |
| `app-camera/src/test/java/com/host application/media/camera/` | `VideoTemporalSamplePlannerTest.kt`, relevant `CameraVideoRecordingStateTest.kt` and `CameraPreviewFramingTest.kt` cases | Portable regression coverage in the corresponding SDK module |
| `scripts/nudity-model-benchmark.py`, relevant benchmark support | Algorithm/tooling only, after dependency inspection | `tools/benchmark/`; exclude corpus, identifying paths, private manifests and outputs |
| `core-policy/MediaSuiteAccessGate.kt` (under its package path), `core-ui` host application state, app manifests and Gradle files | Inspect integration boundaries | Do not import entitlement/provider/signing/diagnostics integration |

Handoff-only facts to reverify in Phase 1: model size 6,128,536 bytes; SHA-256 `051A21BF697858C1E2537354A99BE09A48D26BBFBA0C35216B340F16DE7528D7`; model ID `opennsfw2_resnet50_v1`; float32 input `[1,224,224,3]`, BGR subtraction `[104,117,123]`, output `[1,2]`. Static review previously found that the normal non-Allow video path retains staging; do not treat current production cleanup as satisfying this SDK's contract.

## Phase status and dependency order

Each row is a separately selected implementation unit. `4A` and `4B`, and `7A` and `7B`, are separate turns/phases, not instructions to execute both together.

| Phase | Deliverable | Prerequisite | Status |
| --- | --- | --- | --- |
| 0 | Repository foundation | None | Complete |
| 1 | Provenance evidence and exact extraction allowlist | 0 | Project-owned code cleared; exact model asset remains blocked |
| 2 | Independent Gradle/module skeleton | 1: scaffold files cleared | Complete |
| 3 | Evaluation contracts and failure semantics | 2; relevant Phase 1 clearance | Complete |
| 4A | Spatial sampling and model-specific policy | 3; relevant Phase 1 clearance | Not started |
| 4B | Bundled inference runtime | 4A; exact model clearance | Not started |
| 5 | Managed in-memory photo capture | 4B | Not started |
| 6 | Temporal video review engine | 4B | Not started |
| 7A | Private video staging and cleanup state machine | 6 | Not started |
| 7B | CameraX recording, promotion and recovery integration | 7A | Not started |
| 8 | Usable Compose sample and approved-media viewer | 5, 7B | Not started |
| 9 | Corpus comparison and performance report | 8; authorized local corpus | Not started |
| 10 | Device and lifecycle fault validation | 8; suitable devices | Not started |
| 11 | Local Maven artifacts and separate minified consumer | 8; 9/10 required before release readiness | Not started |
| 12 | Developer documentation and release presentation assets | 9, 10, 11 for verified claims | Not started |
| 13 | Local responsive website | 12; brand/hosting approach recorded | Not started |
| 14 | Reviewed release preparation | 1–13 gates passed | Not started |
| 15 | Explicit public launch actions | 14; specific user authorization | Not started |

### Phase 1 — Provenance and allowlisted extraction inventory

**Scope:** documentation only. Create `docs/PROVENANCE.md`, `docs/EXTRACTION_MANIFEST.md`, and `docs/BASELINE.md`; add full license texts under `third_party/licenses/` only from verified sources. Update this plan with the evidence and import decisions. No Android source or model import yet.

**Work:**

- Recheck source status and guidance; record source commit plus hashes for candidate files, including any uncommitted candidate content. Enumerate each file or selected Activity section, dependencies, destination, license/author provenance, intended adaptation and verification. A directory wildcard is not an import allowlist.
- Trace the exact model and code adaptations through the private SakOS sources, conversion process and upstream notices using available authorized access. Identify evidence gaps explicitly; a build-file license declaration or matching hash alone is insufficient evidence of complete redistribution rights.
- Reverify the model contract/hash and toolchain; inventory policy asset, test and benchmark dependencies. Retain attribution/contribution history as appropriate without importing production Git history.
- Classify each candidate `cleared`, `blocked`, or `excluded`. Audit artwork and names separately. Keep private host addresses, credentials and corpus details out of public-facing evidence.

**Gate:** every planned import has a disposition and evidence pointer. Model/code imports remain blocked wherever rights are unresolved; record exactly what evidence is needed. Unblocked original scaffolding may proceed when explicitly selected, but do not mark the whole provenance phase complete with unresolved required imports.

**Validation:** hash/file inventory consistency, source status unchanged, `git diff --check`. No Gradle. No legal certainty claims based only on historical documentation.

### Phase 2 — Independent Android build skeleton

**Scope:** root `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, cleared wrapper files, minimal manifests/build files for the five modules, `.gitignore`, and `docs/BUILD_NOTES.md`. A minimal sample Activity can make the app buildable; no camera or fake approval behavior.

**Work:** recreate only needed build configuration at the verified baseline. Use `org.sakos.camera` as the proposed SDK package root and `org.sakos.camera.sample` for the sample, recording final names before source creation. Libraries depend inward on contracts; `sample-app` composes them. Keep Compose confined to the sample. Use standard local debug signing and no production signing setup. Do not carry Firebase, host application modules, release scripts or machine-specific paths.

**Gate:** all five modules build independently of the production checkout. An unavailable engine cannot approve media. No model is implied by an empty skeleton. No installation coordinates are advertised.

**Validation:** `:sample-app:dependencies --configuration debugRuntimeClasspath`, common clean debug build, manifest/dependency inspection, `git diff --check`.

### Phase 3 — Core contracts and explicit failure semantics

**Scope:** `safety-core/src/main/` and `src/test/`, plus `docs/API_CONTRACT.md`. Keep the core free of CameraX, Compose and model-specific preprocessing/thresholds.

**Work:** define evaluation input/context, versioned model/preprocessing/policy identity, decisions, failure reasons, cancellation and evaluator lifecycle. Define ownership of inputs/results and distinguish content decisions from host authorization. An unresolved/review outcome is non-approved; it does not create a parent-review retention flow. Managed capture must deliver only a successful Allow tied to the evaluated capture. Do not preserve the legacy fail-open option in the managed workflow or fake `RegaCaptureTrustSnapshot`.

**Gate:** missing/invalid input, unavailable evaluator, failure and cancellation cannot become Allow. Contracts support both evaluation-only and managed capture without a host application dependency. Describe deliberate API differences from the source.

**Validation:** `:safety-core:testDebugUnitTest`, targeted invalid-state/ownership/decision tests, common clean debug build. Establish Android library test task naming here; if core is deliberately made a pure JVM module, document that decision and replace its task with `:safety-core:test` before running it.

### Phase 4A — Spatial sampling and model-specific policy

**Scope:** cleared policy, geometry, score mapping and strategy code/tests in `safety-opennsfw2`; policy asset; `docs/BEHAVIOR_PARITY.md`.

**Work:** extract Fixed14 and Adaptive14 without reducing them to a generic model call. Preserve full/context views, portrait/sentinel probes, ambiguity crops, lower-lateral refinement, broader fallback, corroboration and early exits. Retain applicable tier thresholds and the default Adaptive14 profile. Keep model-specific values outside generic core contracts. Separate content policy from source trust checks and identify expected error-handling differences.

**Gate:** source regression cases plus deterministic synthetic score/crop traces reproduce view order, geometry, evidence decisions and early exits. Each removed source test has a reason; do not delete failing tests merely to obtain a pass. This gate is algorithmic characterization, not model/corpus parity.

**Validation:** `:safety-opennsfw2:testDebugUnitTest`, preserved score-mapping/multicrop/content-policy cases, common clean debug build. Record numerical tolerances and their justification before comparisons.

### Phase 4B — Bundled model and inference runtime

**Scope:** cleared model asset, `safety-opennsfw2` runtime and Android tests, model metadata, provisional `docs/MODEL_CARD.md`.

**Work:** adapt `LiveSakosNudityRuntime.kt`; preserve resize/channel/normalization behavior and link model, preprocessing and policy versions. Validate model integrity and tensor contract; reject malformed shapes, non-finite/out-of-range scores and failed inference. Serialize access to reusable interpreter/buffers; handle concurrent evaluate/close/cancel calls and release owned resources. Cancellation must prevent delivery even if a native inference call must finish before resources can close.

**Gate:** successful real inference uses the verified bundled model; missing/corrupt assets and invalid outputs fail closed. No runtime downloads, telemetry, account or host application requirements. Original defaults remain intact; errors and deliberate differences are documented.

**Validation:** module unit tests, `:safety-opennsfw2:connectedDebugAndroidTest` on an available authorized test target for real LiteRT/preprocessing behavior, common clean debug build. Emulator evidence is not physical-camera validation. If no target is available, record the outstanding runtime gate rather than calling it passed.

### Phase 5 — Managed photo capture

**Scope:** `capture-camerax`, focused tests and photo integration documentation. Read source photo orchestration and reviewed-save paths; do not copy `CameraActivity.kt` wholesale.

**Work:** use in-memory `ImageCapture.OnImageCapturedCallback`, close `ImageProxy` on every path, preserve orientation/front-camera geometry, review before writing, and deliver only the approved capture. Define an approved-output abstraction and make save errors/cancellation visible. Do not write rejected images or debug thumbnails/EXIF/location side effects. Evaluation-only callers retain responsibility for their own storage.

**Gate:** fake evaluator/output-sink tests prove zero SDK image-file writes for blocked, unresolved, failed and cancelled captures. Allow saves the reviewed image exactly once; stale results and double taps cannot promote a different capture. Partial approved-output failures are cleaned up according to the documented output ownership contract.

**Validation:** `:capture-camerax:testDebugUnitTest`, relevant Android capture tests where available, common clean debug build. Physical optics/rotation tests remain Phase 10 gates.

### Phase 6 — Temporal video review engine

**Scope:** planner, frame-decoding boundary and aggregation in `capture-video`; temporal tests and behavior notes. No recording or retained staging workflow yet.

**Work:** extract `VideoTemporalSamplePlanner.kt`, `VideoGatePolicy.kt`, and the evaluation/aggregation portions of `VideoGateEvaluationRunner.kt`. Preserve duration handling, endpoint padding, 30-second interval, 35 decoded-sample cap, escalation and corroboration rules. Add a deterministic frame/evaluator test seam. Explicitly characterize the single uncorroborated non-extreme blocked-timestamp Allow rule; do not silently tighten or weaken it. Release decoded frames/retrievers and propagate cancellation/errors.

**Gate:** compare source and SDK plans/decisions for short/long/unsupported duration, boundary timestamps, corrupt/undecodable frames, escalation exhaustion, context extremes, crop-only extremes and isolated evidence. No sampled-review result is represented as every-frame coverage.

**Validation:** `:capture-video:testDebugUnitTest`, preserved planner tests and synthetic temporal score fixtures, decoder Android tests where available, common clean debug build.

### Phase 7A — Private staging and cleanup state machine

**Scope:** `capture-video` session/storage abstraction and lifecycle tests; `docs/VIDEO_STORAGE.md`. No sample recording UI yet.

**Work:** specify and implement recording → reviewing → promoting → completed states, plus non-approved cleanup and cleanup-failed states. Use app-private staging excluded from backups; define file/sidecar ownership and durable recovery metadata. Delete rejected, unresolved, failed and cancelled sessions including sidecars. Make cleanup idempotent; if it fails, surface that failure and block new recordings until recovery succeeds. Design promotion/recovery together so interruption cannot expose an unreviewed file, duplicate delivery or leave an unmanaged staged copy.

**Gate:** deterministic filesystem fault tests cover delete failure, metadata failure, low storage, cancellation, duplicate events and each interrupted transition. Cleanup-failed state survives restart or is rediscovered from staging. Startup recovery preserves already completed approved outputs while purging abandoned staging. No provider, thumbnail, backup or export path exposes staging.

**Validation:** `:capture-video:testDebugUnitTest`, Android private-storage/backup configuration tests as needed, common clean debug build. Document which guarantees require host integration and which the SDK enforces itself.

### Phase 7B — Recording, review, promotion and startup recovery

**Scope:** CameraX recording orchestration and approved-output integration in `capture-video`, Android integration tests.

**Work:** connect CameraX private file output to Phase 6 review and Phase 7A lifecycle. Run recovery before accepting the first/new recording. Stop/release recorder and decoder handles before deletion; bind each review result to its session. Promote only Allow and finish staging cleanup. Handle recording finalization races, cancellation during review/promotion, app backgrounding and interrupted promotion without bypassing the cleanup block. Report any residual private data honestly.

**Gate:** integration tests demonstrate approved-only delivery and cleanup on every other terminal outcome. Reproduce the source's retained non-Allow case and show that the SDK now cleans it up; record this as a versioned intentional lifecycle change. No parent-review retention escape hatch.

**Validation:** `:capture-video:testDebugUnitTest`, `:capture-video:connectedDebugAndroidTest` on a suitable target, common clean debug build. Deterministic simulated restarts supplement, but do not replace, Phase 10 process-death tests.

### Phase 8 — Sample app and approved-media viewer

**Scope:** `sample-app`, UI resources, sample integration tests, usage README. No full Gallery parity or host application migration.

**Work:** add Compose photo/video controls, front/back selection, capture/review/cancellation states, useful failure/cleanup-retry messages, and a minimal viewer populated only from completed approved outputs. Handle camera/microphone permission denial and lifecycle changes. Demonstrate the managed path and provide a small evaluation-only example with accurate storage responsibility notes. Keep UI professional and accessible without exposing raw implementation details to normal users.

**Gate:** fresh install works without host application, accounts or network permission/runtime network dependencies. No blocked clips/thumbnails appear in the viewer. Cleanup failure disables recording with an actionable recovery state. No location tagging, sharing/cloud sync or unrelated product features.

**Validation:** sample unit/UI smoke checks and merged manifest inspection, common clean debug build; capture test-target limitations. Use synthetic/authorized safe media for screenshots, never private corpus media.

### Phase 9 — Behavioral comparison and performance evidence

**Scope:** portable `tools/benchmark/`, sanitized `docs/validation/PARITY_REPORT.md` and model-card updates. The corpus remains outside this repository and is never copied or uploaded.

**Work:** run the same authorized inputs through a pinned source reference and SDK with identical model/policy/strategy settings. Keep the production source read-only: reuse existing verified reference outputs or prepare a cleared temporary reference harness outside it. Validate that the harness represents source behavior. Compare decisions, scores, selected views/timestamps, early exits and rationale categories. Isolate intended host/error/cleanup differences from unintended classification differences.

**Gate:** no unexplained decision mismatch; score tolerances are justified. Report false accepts, false rejects, unresolved/error outcomes, denominators, labeling method, corpus limitations, device/runtime identity and latency distribution. Missing labels or corpus means relevant metrics remain unavailable, not zero. Resolve extraction errors within scope; proposed policy changes require a separate versioned decision.

**Validation:** reproducible benchmark command and sanitized results, tool smoke tests, `git diff --check`; run Android common checks only if Android files change. No claim of perfect detection from zero observed misses.

### Phase 10 — Physical-device and fault validation

**Scope:** `docs/validation/DEVICE_MATRIX.md`, test harnesses and narrowly scoped fixes to already implemented modules.

**Work:** cover fresh-install airplane-mode operation, no host application, API 26 compatibility and a current supported Android version, front/back cameras, orientation, repeated capture, resource cleanup and latency. Test missing/corrupt models, invalid results, inference failure, permission revocation, cancellation, backgrounding, low storage, deletion failure and interruption during promotion. Kill the process during recording, review and promotion; relaunch and verify recovery before new recording. Inspect staging, sidecars, approved outputs, gallery/provider exposure and backup rules.

**Gate:** record device/OS/build/model/policy versions and pass/fail/pending per scenario with sanitized evidence. Emulator results do not close physical-camera gates. If devices or fault-injection access are unavailable, preserve the open matrix and stop short of release readiness.

**Validation:** exact reproducible test steps, focused regression tests for fixes, common clean debug build for Android changes. Never treat a debug build as device validation or production delivery.

### Phase 11 — Local Maven publication and independent consumer

**Scope:** library publishing metadata, consumer R8 rules where actually needed, a local verification script, `integration-tests/consumer/` as a separate Gradle build, `docs/validation/CONSUMER_REPORT.md`.

**Work:** publish the four libraries to a repository-local test Maven directory under `build/`. Use explicitly provisional local coordinates and record them; remote namespace/version ownership remains a release decision. The separate consumer must resolve Maven artifacts and transitive dependencies without `project(...)`, composite-build substitution or production checkout paths. Verify release minification, model/policy assets, public API use and offline runtime operation. Inspect POM/module metadata, licenses/notices and consumer rules. Do not use production signing credentials.

**Gate:** build and run the separate minified consumer without host application; inspect the packaged model and notices. Record local publication separately from any remote release. Phases 9/10 must pass before claiming the artifacts ready for release.

**Validation:** create `scripts/verify-local-consumer.ps1` with the exact local publish tasks and separate-consumer `assembleRelease` command; run it and record output. Also run common Android checks. Do not assume `publishToMavenLocal` alone proves correct transitive metadata or consumer execution.

### Phase 12 — Developer documentation and launch assets

**Scope:** `README.md`, developer docs, model card, third-party notices, `CONTRIBUTING.md`, `SECURITY.md`, `CHANGELOG.md`, `.github/ISSUE_TEMPLATE/`, sanitized screenshots/demo assets.

**Work:** document quick start, modules, both entry points, customization/versioning, API reference, supported baseline, troubleshooting, storage responsibilities, model provenance and measured limitations. Document the preserved isolated-timestamp behavior and sampled video coverage honestly. Explain that alternative models require evaluation and hosts can bypass an app-level SDK. Retain upstream credits distinct from SakOS sampling/capture contributions.

Prepare a coherent brand/visual brief and real sample screenshots/demo from authorized media. Verify the security-reporting channel and repository settings before advertising them; do not enable external services or messaging implicitly. Keep private source paths/hosts and credentials out of public material.

**Gate:** quick start reproduces the verified local consumer; no invented install coordinates, public releases, badges, metrics or demos. Published claims trace to actual evidence. Docs explicitly distinguish SDK-managed guarantees from host behavior and avoid general child-safety certification claims.

**Validation:** snippet/API consistency, links and accessibility/asset-rights review. Documentation-only work does not run Gradle; compile changed runnable examples when applicable.

### Phase 13 — Local website implementation and responsive review

**Scope:** proposed `website/` with umbrella `/`, product `/camera/`, and developer `/docs/camera/` routes. Local development/preview only.

**Work:** implement the Phase 12 identity and verified content, adaptive-review explanatory visuals, real screenshots/demo, keyboard navigation and accessible responsive layouts. Before adding dependencies, record the chosen stack, exact package scripts and deployment-compatible output in this plan. Hosting remains unresolved until then; do not assume DNS or an existing site has been audited.

**Gate:** review at narrow mobile, tablet and desktop widths, including resizing, focus states, readable content, sensible crops and local navigation. No nonfunctional download/install buttons, fake metrics or undeployed public-demo claims. No analytics/telemetry added by default.

**Validation:** recorded package install/build/check commands, browser inspection and asset/link checks. Android Gradle is unnecessary unless Android artifacts change. Do not deploy or modify DNS in this phase.

### Phase 14 — Reviewed release preparation

**Scope:** `docs/RELEASE_CHECKLIST.md`, `docs/RELEASE_RUNBOOK.md`, candidate artifacts and final local evidence. Repository stays private.

**Work:** audit the exact export, licenses, model rights, dependency metadata and artifact hashes; scan for secrets/production identifiers/private data. Verify README/model-card claims against Phases 9–13. Record actual repository visibility and target package registry/coordinates, release version, hosting account/platform, DNS changes and security-reporting readiness. Prepare release notes, artifact list, website build and a sequenced rollback/incident plan. If targets remain undecided, record the specific unresolved decision before preparing dependent changes.

**Gate:** every launch action is concrete and reviewable, with commands, target/account, evidence and limits. All required gates are passed; no unresolved rights, privacy or unexplained parity failures. Public release is still not authorized by completing this phase.

**Validation:** clean scoped diff, package/install reproduction, sanitized release manifest and site preview. Repeat expensive checks only when changed artifacts invalidate prior evidence.

### Phase 15 — Explicitly authorized public launch

**Scope:** only the release actions the user names after reviewing Phase 14. Repository visibility, commit/push/tag, package publication, website deployment and DNS are distinct actions; approval of one does not imply the others.

**Work:** use the prepared runbook, recheck targets and execute the authorized subset. Verify public artifact resolution, license/model files, live routes and DNS as applicable. Update the release record with exact versions/URLs and remaining unperformed actions. host application migration remains a separate project.

**Gate:** report observed external delivery independently from local builds; do not claim a website is deployed because a GitHub website field points to it. If an external action fails, stop dependent actions and report the exact state.

## Common verification and evidence

For every implementation phase:

- Recheck SDK and source status; review changed files against the recorded phase allowlist and run `git diff --check`.
- For Android source/assets/manifests/resources/Gradle changes, run the phase's focused tests, then `.\gradlew.bat --warning-mode all clean assembleDebug` from the SDK root. Do not run this command for documentation-only work.
- Keep full local logs under `build-logs/`; `.gitignore` already excludes `*.log`. Record exact commands, exit codes, environment and sanitized conclusions in tracked `docs/BUILD_NOTES.md`. Never force-add sensitive/raw logs merely to make evidence tracked.
- Capture the native exit code immediately; `Tee-Object` output is not itself evidence that Gradle passed. Example after the wrapper exists:

```powershell
New-Item -ItemType Directory -Force -Path build-logs | Out-Null
$phaseId = '3' # Replace with the selected phase.
& .\gradlew.bat --warning-mode all :safety-core:testDebugUnitTest 2>&1 |
    Tee-Object -FilePath "build-logs/phase-$phaseId-focused.log"
$focusedExitCode = $LASTEXITCODE
if ($focusedExitCode -ne 0) { throw "Focused tests failed: $focusedExitCode" }
& .\gradlew.bat --warning-mode all clean assembleDebug 2>&1 |
    Tee-Object -FilePath "build-logs/phase-$phaseId-clean-debug.log"
$buildExitCode = $LASTEXITCODE
if ($buildExitCode -ne 0) { throw "Clean debug build failed: $buildExitCode" }
git diff --check
```

Use the selected phase's actual focused task instead of the Phase 3 example. The clean build removes module test outputs; retain required test summaries/reports before running it. Inspect warnings and report unresolved ones without unrelated upgrades. Connected tests require an available authorized target; do not mark them passed when skipped.

Use `not started`, `in progress`, `blocked`, or `complete` in the phase table. Record local build, algorithm characterization, actual-model execution, corpus comparison, physical-device validation, local Maven consumption, remote publication and website deployment as separate evidence categories.

## Copy-ready Terra / High implementation prompt

Select Terra / High in the task settings. Paste this prompt with a single phase ID, such as `1`, `4A`, or `7B`. The plan contains the phase-specific files, exclusions and gates, so one prompt can be reused without carrying a growing chat history.

```text
Implement Phase <ID> only from docs/PROJECT_PLAN.md in the current
sakos-camera-sdk checkout. Read that plan in full, the README, applicable
AGENTS.md, and the prerequisite phase records. Use the agreed scope; do not
restart product discovery or automatically continue into another phase.

Verify cwd, branch/HEAD and Git status first. Keep local workstation path omitted and
local workstation path omitted read-only and preserve all unrelated changes. If this
SDK worktree is detached, create an appropriate codex/ implementation branch
without altering the saved checkout. Do not commit, push, publish, deploy,
change DNS/visibility, or modify production signing.

Before code edits, append the exact phase file/section allowlist, steps,
tests and acceptance criteria to docs/PROJECT_PLAN.md. Use only cleared
imports from docs/EXTRACTION_MANIFEST.md; Phase 1 creates that document.
Preserve model/policy defaults, adaptive sampling and temporal review;
keep host authorization separate and enforce the planned privacy contract.

Complete the selected phase, run its focused checks and the common
verification, and update the phase status and append-only execution record.
For Android changes, capture focused tests and
.\gradlew.bat --warning-mode all clean assembleDebug under build-logs and
record results in docs/BUILD_NOTES.md. No Gradle for docs-only work.

Stop at the phase boundary. If required rights, evidence, tools, corpus or
devices are unavailable, record the precise blocker and pending gates;
do not substitute invented evidence or implement dependent work.
Finish with changed files, checks/results, intentional behavior differences,
remaining limitations and the next eligible phase. Do not claim public
release, device validation or website deployment from a local build.
```

For the first implementation turn, replace `<ID>` with `1`. Phase 1 is a provenance/documentation audit; it does not import the model or Android source. Phase 15 instead requires a prompt naming the approved external actions from the release runbook; the no-publication prompt above is intentionally insufficient for launch.

## Execution record template

Append an entry after each implementation turn, including partial work. Preserve earlier records; correct mistaken claims with a dated follow-up rather than silently overwriting history.

```text
### Phase <ID> — <date> — <in progress / blocked / complete>
- Checkout/branch/HEAD and source revision/status:
- Prerequisites and provenance clearance:
- Pre-edit exact source sections / target files / implementation steps:
- Acceptance criteria and planned checks:
- Changes actually made and intentional behavioral differences:
- Commands, exit codes, logs/reports, device/runtime versions:
- Gate results (passed / failed / pending), limitations and blockers:
- Source worktree preservation and SDK diff review:
- Remaining work and next eligible phase (not automatically authorized):
```

## Setup execution record — 2026-09-17

- Updated the SakOS GitHub organization display name, description, and website link.
- Created the private SDK repository with an initial README, Android ignore template, and Apache-2.0 license.
- Cloned it into the existing empty project directory.
- Added this plan and an introductory README. No SDK source, model weights, private corpus, or production configuration has been imported.
- Website deployment and DNS changes have not been performed. This setup does not assert SDK or model release readiness.

### Phase 1 — 2026-09-17 — blocked for source/model import

- Checkout/branch/HEAD and source revision/status: SDK branch `codex/phase-1-provenance`, based on `651b05f5289a0ecaf58829bdf691c6bf06c94724`; source reference `historical source revision omitted` remained read-only with its two pre-existing modifications to `docs/BUILD_NOTES.md` and `scripts/build-play-release-bundles-with-reviewer-token.ps1`.
- Prerequisites and provenance clearance: created `docs/BASELINE.md`, `docs/PROVENANCE.md`, and `docs/EXTRACTION_MANIFEST.md`. The manifest pins the candidate source files/hashes and clears only independently authored Phase 2 scaffolding. It blocks the exact model and all verbatim/adapted source imports until exact model redistribution, conversion provenance, contributor authorization and notices are evidenced.
- Pre-edit exact source sections / target files / implementation steps: no source section was copied. The target files were this plan and the three Phase 1 documentation records. The recorded source reference includes the model asset/contract, still policy/runtime/sampling candidates, photo/video orchestration candidates, their selected test references and benchmark script.
- Acceptance criteria and planned checks: every proposed import has a disposition and evidence requirement; source status remains preserved; no model/source import occurs; later Phase 2 may author the cleared independent skeleton only.
- Changes actually made and intentional behavioral differences: created the provenance inventory; no product behavior changed. The independent SDK will later remove host application trust coupling, fail closed, and add video cleanup/recovery, but none is implemented here.
- Commands, exit codes, logs/reports, device/runtime versions: read-only status, Git history, source hashes and contract/policy inspections completed successfully. `git diff --check` passed. Gradle was not run because Phase 1 is documentation only.
- Gate results (passed / failed / pending), limitations and blockers: inventory/disposition gate passed. Source/model redistribution clearance is pending and blocks import; neither a matching checksum nor private-repository license declarations prove it. No corpus, device, inference, local Maven, publication or website evidence exists.
- Source worktree preservation and SDK diff review: source retained only its pre-existing two edits. SDK diff contains only the Phase 1 plan/provenance/manifest/baseline documentation.
- Remaining work and next eligible phase (not automatically authorized): Phase 2 may create independently authored build/module scaffolding using the cleared baseline, without source/model imports. Clearance evidence is still required before Phases 3–7 can incorporate the blocked source-derived behavior or model.

### Phase 2 — 2026-09-17 — in progress

- Checkout/branch/HEAD and source revision/status: SDK branch `codex/phase-2-skeleton`, based on Phase 1 commit `4e259f8`; source remains a read-only reference at `historical source revision omitted` with its two pre-existing unrelated modifications.
- Prerequisites and provenance clearance: `docs/EXTRACTION_MANIFEST.md` clears independently authored Gradle/module/sample scaffolding and a wrapper generated from the official Gradle 8.13 distribution. It does not clear source/model copying.
- Pre-edit exact source sections / target files / implementation steps: no source code or asset is an input. Create root `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, generated Gradle 8.13 wrapper files, library manifests/build files for `safety-core`, `safety-opennsfw2`, `capture-camerax`, and `capture-video`, plus the `sample-app` build file, manifest, minimal Compose activity and resources. Add `docs/BUILD_NOTES.md` to record build evidence. Use packages `org.sakos.camera` and `org.sakos.camera.sample`; include no permissions, signing configuration, model asset, camera implementation, network/Firebase dependency, host application dependency, or approval behavior.
- Acceptance criteria and planned checks: the five included modules resolve only their declared inward SDK dependencies and AndroidX/LiteRT baseline dependencies; the sample shell builds with standard debug signing and plainly states capture is not wired. Run `:sample-app:dependencies --configuration debugRuntimeClasspath`, then `--warning-mode all clean assembleDebug`, capture logs under ignored `build-logs/`, inspect manifests/dependency output, run `git diff --check`, and append final results before committing.
- Changes actually made and intentional behavioral differences: added the five-module Gradle skeleton, generated an official Gradle 8.13 wrapper, and added a Compose-only sample setup screen. The sample requests no permissions and has no camera, microphone, model, capture, review, approval or output behavior. No source-derived code/asset, Firebase, host application integration, production signing or release configuration was introduced.
- Commands, exit codes, logs/reports, device/runtime versions: wrapper generation, dependency inspection and clean debug build all exited 0. Sanitized command/results are in `docs/BUILD_NOTES.md`; raw logs are ignored under `build-logs/`. The build used Android Studio OpenJDK 21.0.10 and produced 153 actionable tasks in 61 seconds. It reported SDK XML version 4 and unstripped debug native libraries; both are recorded as non-failing environment/package observations.
- Gate results (passed / failed / pending), limitations and blockers: passed all local Phase 2 checks. The sample runtime graph resolved all four library modules and their inward dependencies; manifest inspection found no permissions, providers, queries, Firebase or host application references. The source/model provenance block remains unchanged. This is not a functional SDK, inference proof, device test, consumer test, publication or release gate.
- Source worktree preservation and SDK diff review: source remains read-only with only its two pre-existing edits. SDK changes are limited to independently authored build/module/sample scaffolding, the generated Gradle wrapper and tracked evidence; `git diff --check` passed.
- Remaining work and next eligible phase (not automatically authorized): Phase 3 can independently author generic evaluation contracts and fail-closed semantics in `safety-core`. The later provenance amendment clears selective project-owned source code under the user's extraction authorization, but model-source clearance remains required before a model asset import and real inference.

### Phase 3 — 2026-09-17 — in progress

- Checkout/branch/HEAD and source revision/status: SDK branch `codex/phase-3-core-contracts`, based on Phase 2 commit `e6043a7`; no source checkout changes are permitted or planned.
- Prerequisites and provenance clearance: Phase 2 skeleton and Phase 1's independently authored generic-contract clearance are present. Source/model imports remain blocked and are not inputs to this phase.
- Pre-edit exact source sections / target files / implementation steps: no source file/section will be copied or adapted. Create independently authored `safety-core/src/main/java/org/sakos/camera/safety/core/SafetyContracts.kt`, `safety-core/src/test/java/org/sakos/camera/safety/core/SafetyContractsTest.kt`, and `docs/API_CONTRACT.md`; update this plan and `docs/BUILD_NOTES.md`. Define opaque capture identity/context, versioned model/preprocessing/policy identity, Allow/Block/Review decisions, non-approving failure reasons, an evaluator lifecycle interface, a fail-closed unavailable evaluator and an approval token emitted only for a matching Allow receipt. Keep the core free of CameraX, Compose, tensor details, thresholds, host trust/entitlement and storage I/O.
- Acceptance criteria and planned checks: invalid identity/geometry/version values cannot construct requests; unavailable, invalid-output, failed and cancelled outcomes cannot yield an approval token; approval is bound to the evaluated capture/receipt; the public contracts mention no host application or model-specific policy. Run `:safety-core:testDebugUnitTest`, the common clean debug build, source/dependency searches and `git diff --check`; capture logs under ignored `build-logs/` and record results before committing.
- Changes actually made and intentional behavioral differences: added generic capture/configuration identities, content decisions, failure reasons, evaluator lifecycle and a fail-closed unavailable evaluator. `ManagedCaptureApproval` has an internal constructor and can be produced only from an Allow receipt for the same capture context. Review, failure and cancellation have no approval path. The API intentionally has no host trust snapshot, model/tensor/threshold, storage, UI or CameraX dependency.
- Commands, exit codes, logs/reports, device/runtime versions: `:safety-core:testDebugUnitTest` and the common clean debug build both exited 0. Sanitized results are in `docs/BUILD_NOTES.md`; raw logs are ignored under `build-logs/`. The build used the Phase 2 recorded Android Studio OpenJDK 21.0.10 environment.
- Gate results (passed / failed / pending), limitations and blockers: passed invalid-value, capture-binding, Block/Review/cancelled, and unavailable/closed evaluator checks. The test report generated during focused testing was removed by the required later clean build; the successful focused command log is retained as evidence. The contracts do not yet execute a model, own input storage or prove cancellation against a native interpreter. Source/model provenance remains blocked.
- Source worktree preservation and SDK diff review: no source worktree operation occurred; its two pre-existing edits remain untouched. SDK changes are limited to Phase 3 core contracts, tests and documentation; `git diff --check` passed.
- Remaining work and next eligible phase (not automatically authorized): the provenance amendment records user-authorized selective export of project-owned sampling, policy and video code. Phase 4A may now preserve/characterize that cleared code without importing the model. Phase 4B remains blocked for real inference until exact model asset rights are evidenced.

## Planning revision record — 2026-09-17

- Replaced broad delivery stages with bounded phases, dependencies, source/target boundaries, acceptance gates, common verification and a reusable Terra / High prompt.
- Read the tracked README/plan and source repository guidance; verified the source baseline, relevant filenames, trust-policy coupling and temporal policy constants through read-only inspection.
- Validation: `git diff --check` passed; SDK status shows only `docs/PROJECT_PLAN.md` modified. Source status still shows only the two pre-existing edits recorded above. Gradle was not run because this revision changes documentation only.
- Modified only this plan. No SDK source/model import, source-repository edit, Android build, commit, publication or deployment was performed. Full provenance review, full source characterization and implementation remain future phase work.

### Phase 1 provenance amendment — 2026-09-17

- The user explicitly authorized extraction of the camera/gallery application work. The inspected source remote is under the user's account, and Git history for the selected spatial, runtime, photo and video candidates contained only `mendypan` or `Mendi Yuda` author names.
- `docs/PROVENANCE.md` and `docs/EXTRACTION_MANIFEST.md` now clear selective export of those project-owned code/policy/benchmark candidates while preserving source revision/hash records and exclusions. The exact model asset, private SakOS upstream provenance, unreviewed third-party material, private corpus/media and artwork remain blocked.
- This amendment enables cleared algorithm/capture work in Phases 4A and 5–7. It does not authorize model import, Phase 4B execution, remote publication or public release.
