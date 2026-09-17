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
| 4A | Spatial sampling and model-specific policy | 3; relevant Phase 1 clearance | Complete |
| 4B | Bundled inference runtime | 4A; exact model clearance | Blocked: exact conversion provenance host unavailable |
| 5 | Managed in-memory photo capture | 4B preflight complete; real model pending | Complete for injected evaluator; device/model pending |
| 6 | Temporal video review engine | 4B preflight complete; real model pending | Complete for injected decoder/evaluator; Android/model pending |
| 7A | Private video staging and cleanup state machine | 6 | Complete for injected private-store boundary; Android file-store pending |
| 7B | CameraX recording, promotion and recovery integration | 7A | Complete for managed seam/recording adapter; real CameraX/file-store pending |
| 8 | Usable Compose sample and approved-media viewer | 5, 7B | Complete for contract demonstrator; live capture/viewer pending |
| 9 | Corpus comparison and performance report | 8; authorized local corpus | Blocked: comparator complete; real model and authorized corpus unavailable |
| 10 | Device and lifecycle fault validation | 8; suitable devices | Blocked: pending physical devices and functional runtime |
| 11 | Local Maven artifacts and separate minified consumer | 8; 9/10 required before release readiness | Complete for local verification; release readiness blocked by 9/10 |
| 12 | Developer documentation and release presentation assets | 9, 10, 11 for verified claims | Complete for verified local documentation; public assets/release claims pending |
| 13 | Local responsive website | 12; brand/hosting approach recorded | Complete for local static preview; deployment and public assets remain pending |
| 14 | Reviewed release preparation | 1–13 gates passed | Blocked: private preparation, evidence audit and candidate templates complete; required release gates unresolved |
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

### Phase 4A — 2026-09-17 — in progress

- Checkout/branch/HEAD and source revision/status: SDK branch `codex/phase-4a-spatial-policy`, based on the provenance-amendment commit `0cb5b39`; source reference remains read-only at `historical source revision omitted` with its two pre-existing unrelated edits.
- Prerequisites and provenance clearance: Phase 3 core contracts are complete. The provenance amendment clears the listed project-owned spatial/policy code and its policy asset at their pinned hashes; it continues to block the model asset and any live inference.
- Pre-edit exact source sections / target files / implementation steps: selectively export the cleared source files `GateSamplingProfile.kt`, `IntegratedOpenNsfw2Strategy.kt`, `IntegratedStillGatePolicyDefaults.kt`, `SakosCompatibleScoreMapping.kt`, and `SakosCompatibleMultiCropEvaluation.kt`, plus `policy/opennsfw2_still_gate_policy.json`, into `safety-opennsfw2` under the `org.sakos.camera.safety.opennsfw2` package. Add a small independent result identity type instead of importing blocked host application trust/model contracts. Port sanitized score-mapping and crop/policy tests, add policy-asset consistency tests and `docs/BEHAVIOR_PARITY.md`; update build notes and this record. Do not add the `.tflite`, `LiveSakosNudityRuntime.kt`, `SakosCompatibleGatePolicy.kt`, host trust logic, Firebase, telemetry, UI or camera capture.
- Acceptance criteria and planned checks: Fixed14/Adaptive14 view order, supported-ratio geometry, unsupported fallback, portrait sentinels, ambiguity/lower-lateral paths, evidence corroboration, early Allow/Block thresholds and score mapping match the pinned source fixtures exactly with float tolerance `0.0001f` for deterministic score comparisons. The policy asset must match the recorded source hash. Run `:safety-opennsfw2:testDebugUnitTest`, common clean debug build, asset/hash and source-boundary inspection, and `git diff --check`; record results before committing. This is spatial-policy characterization only, not real-model/corpus parity.
- Changes actually made and intentional behavioral differences: selectively exported the cleared spatial profile, strategy, policy defaults, score mapping, multicrop/evidence aggregation and policy asset into `safety-opennsfw2`, plus 28 sanitized deterministic tests. Replaced blocked host application trust/model contracts with a small result identity used only by policy mapping. The asset content is unchanged after Git LF normalization; its working-copy byte hash changes only because the source checkout uses CRLF. No `.tflite`, live runtime, host authorization, camera, UI, telemetry or storage code was added.
- Commands, exit codes, logs/reports, device/runtime versions: focused `:safety-opennsfw2:testDebugUnitTest` and common clean debug build both exited 0. The focused report recorded 26 spatial-policy and 2 score-mapping tests, all passing. Exact command/results, normalized asset comparison and recurring non-failing build messages are in `docs/BUILD_NOTES.md`; raw logs are ignored under `build-logs/`.
- Gate results (passed / failed / pending), limitations and blockers: algorithmic characterization passed with `0.0001f` deterministic fixture tolerance. It proves source-policy geometry/order/evidence behavior, not the blocked model asset, actual inference, corpus parity, device behavior, latency or safety accuracy. Phase 4B is blocked until exact model redistribution/conversion evidence is added to `docs/PROVENANCE.md`.
- Source worktree preservation and SDK diff review: source remains read-only with only its two pre-existing modifications. SDK changes are limited to the allowlisted Phase 4A package, policy asset, sanitized tests and documentation; `git diff --check` passed.
- Remaining work and next eligible phase (not automatically authorized): model runtime Phase 4B is blocked by the exact-model provenance gate. Phase 5/6 capture work needs a functioning evaluator and therefore must not bypass the fail-closed unavailable evaluator. The next independent work is resolving the exact model rights/conversion chain or building non-model test seams without representing capture as functional.

### Phase 4A formatting correction — 2026-09-17

- The initial Phase 4A commit included trailing blank lines in seven imported Kotlin/test files. The immediately following scoped formatting commit removes only those blank lines; `git diff --check` then passed. The prior focused tests and clean build remain valid because this correction changes no executable content.

### Phase 4B provenance preflight — 2026-09-17 — blocked

- Verified public OpenNSFW2 MIT and Yahoo Open NSFW BSD-2-Clause license texts and added them under `third_party/licenses/`; the provenance record distinguishes these upstream redistribution terms from the unverified exact local model conversion.
- The authorized private provenance host was unreachable during a read-only Git check. No private repository, model asset, conversion script or credentials were copied, changed or exposed.
- The Phase 4B model/runtime gate remains blocked pending a reviewable exact conversion chain and notices for SHA-256 `051A21BF697858C1E2537354A99BE09A48D26BBFBA0C35216B340F16DE7528D7`. Later capture phases must retain fail-closed behavior and cannot substitute a different model without a separately versioned evaluation.

### Phase 4B model preflight — 2026-09-17 — in progress, asset import blocked

- Checkout/branch/HEAD and source revision/status: SDK branch `codex/phase-4b-model-preflight`, based on `b84a5a0`; source remains read-only and retains only its two pre-existing unrelated edits.
- Prerequisites and provenance clearance: public upstream notices are retained, but the exact local model asset and conversion chain remain blocked. This phase may implement only fail-closed preflight code without importing `.tflite` bytes or a real interpreter.
- Pre-edit exact source sections / target files / implementation steps: create `OpenNsfw2ModelPreflight.kt` and matching unit tests in `safety-opennsfw2`, plus documentation/build records. Pin the model ID, asset path, expected byte size, SHA-256, tensor shape and BGR preprocessing contract from the evidence record. Implement streaming size/SHA preflight and a `SafetyEvaluator` that returns `ModelUnavailable` or `ModelIntegrityFailure` rather than Allow. Do not copy `LiveSakosNudityRuntime.kt`, import the model asset, construct LiteRT, add a model download, or alter sampling/policy constants.
- Acceptance criteria and planned checks: absent asset, wrong size, mismatched digest and a closed evaluator cannot approve managed output; configuration identity binds the known model/preprocessing/policy components. Run `:safety-opennsfw2:testDebugUnitTest`, common clean debug build, static source-boundary inspection and `git diff --check`, then record results before committing. This preflight does not satisfy the actual-model Phase 4B gate.
- Changes actually made and intentional behavioral differences: added a streaming asset size/SHA verifier, complete unbundled model/preprocessing/policy identity, and a fail-closed evaluator. A missing asset and an otherwise verified-but-unimplemented runtime report `ModelUnavailable`; malformed asset status reports `ModelIntegrityFailure`; a closed evaluator reports `EvaluatorClosed`. No status can return Allow. Added a provisional model card that accurately states no model is bundled.
- Commands, exit codes, logs/reports, device/runtime versions: focused preflight tests and the common clean debug build both exited 0. Sanitized outcomes are in `docs/BUILD_NOTES.md`; raw logs are ignored under `build-logs/`. The existing Android Studio OpenJDK 21.0.10 environment and non-failing SDK XML/debug-native packaging messages remain unchanged.
- Gate results (passed / failed / pending), limitations and blockers: fail-closed preflight gate passed. Actual-model gate remains blocked: no `.tflite`, LiteRT interpreter, runtime tensor validation, real inference, model/corpus parity, device validation or publication evidence exists.
- Source worktree preservation and SDK diff review: source remains untouched with only its two pre-existing edits. SDK changes are limited to model contract/preflight code, unit tests and documentation; `git diff --check` passed.
- Remaining work and next eligible phase (not automatically authorized): Phase 4B cannot complete until the exact asset conversion/redistribution evidence is reviewable. The next safe implementation work is a generic injected-evaluator test seam for later capture modules; do not claim a functional camera pipeline until actual evaluator evidence exists.

### Phase 5 — 2026-09-17 — in progress

- Pre-edit scope: create an independently authored `capture-camerax` in-memory review pipeline, tests and photo integration guide. The pipeline accepts caller-provided in-memory conversion, `SafetyEvaluator`, and approved-output sink; it never writes raw/rejected input. It closes a frame in all terminal paths and exposes a CameraX `ImageProxy` adapter. Tests use injected evaluators and sinks because the actual model runtime remains unavailable. No source Activity, media provider, EXIF/location behavior, thumbnail, host application integration or disk staging is imported.
- Acceptance checks: Block, Review, Failure and Cancelled outcomes produce no sink write; Allow for the matching capture writes once; duplicate/in-flight capture IDs never write again; sink failure is visible; frames close on success/failure. Run focused unit tests and clean debug build before commit.
- Result: implemented injected in-memory review with a CameraX `ImageProxy` adapter and capture-bound approval token. Focused tests and clean build passed. The actual model and physical CameraX device paths remain pending; no rejected input is written by this pipeline.

### Phase 6 — 2026-09-17 — complete for injected decoder/evaluator; Android/model pending

- Checkout/branch/HEAD and source revision/status: SDK branch `codex/phase-6-temporal-review`, based on `eb1c6f2`; source was read only at `historical source revision omitted` with its two pre-existing unrelated edits preserved.
- Pre-edit scope: selectively export cleared `VideoTemporalSamplePlanner.kt`, `VideoGatePolicy.kt`, and sanitized planner regression tests into `capture-video`; preserve 30-second base interval, 35 decoded-sample cap and temporal plan semantics. Extract the runner's aggregation behavior into an independently authored injected decoder/evaluator seam. Do not import `MediaMetadataRetriever`, host application trust, live-model wiring, a video file, frame payload, or source application code.
- Changes actually made and intentional behavioral differences: exported the planner/policy and its ten source-derived regression tests under the SakOS package. Added `VideoTemporalReviewEngine`, a decoder that owns a closeable frame wrapper, an injected evaluator, finite 0..1 result validation, and explicit Allow/Block/Review/Failure results. The engine releases each decoded frame and closes the decoder. It preserves context/crop evidence, escalation selection, temporal corroboration, the 35-frame cap, and the source's isolated non-extreme final-block Allow rule. Unsupported duration is Review; decode/evaluation errors are Failure. The source's MediaMetadataRetriever/retriever rotation, host application trust, live inference and file ownership concerns remain outside this phase.
- Commands, exit codes, logs/reports, device/runtime versions: after one source-level Kotlin expression-body compile failure was corrected, `.\\gradlew.bat --no-daemon :capture-video:testDebugUnitTest` exited 0 in 23 seconds. The report recorded 18 passing tests: 10 planner regressions and 8 synthetic temporal-review cases. Raw logs are ignored under `build-logs/`; sanitized results are in `docs/BUILD_NOTES.md`.
- Gate results (passed / failed / pending), limitations and blockers: synthetic source-policy plan/decision characterization passed for short/long/unsupported durations, confirmation planning, decode failure, context extreme, crop extreme with temporal support, Review, cap, and isolated evidence. A real Android decoder, actual model result, MediaMetadataRetriever lifecycle, device behavior, corpus parity and recording/staging ownership remain pending and are not represented as passed.
- Source worktree preservation and SDK diff review: no source write/build/reset occurred. Static boundary review found no host application, `MediaMetadataRetriever`, temporary-video, Camera Activity, live-runtime, model-asset, or Firebase reference in `capture-video`; `git diff --check` passed before the clean build.
- Remaining work and next eligible phase (not automatically authorized): Phase 7A may use this injected temporal engine to implement private staging and cleanup. Phase 4B remains independently blocked on model conversion/redistribution evidence; a real decoder/model integration requires its own Android/device verification.

### Phase 7A — 2026-09-17 — complete for injected private-store boundary; Android file-store pending

- Checkout/branch/HEAD and source revision/status: SDK branch `codex/phase-7a-video-staging`, based on Phase 6 commit `3afd87c`; source remains read only at `historical source revision omitted` with its two pre-existing unrelated edits.
- Pre-edit exact target files / implementation steps: add an independently authored `VideoStagingSessionManager` plus focused JVM tests under `capture-video`, and `docs/VIDEO_STORAGE.md`. Model storage as an injected private-store boundary with explicit recording, reviewing, promoting, completed and cleanup-failed states. Implement rejected/failed/cancelled cleanup, cleanup retry, new-recording blocking while cleanup is unresolved, and startup recovery that preserves completed outputs while purging abandoned staging. Do not add a public MediaStore target, CameraX recording integration, thumbnails, backups, real file paths, source temporary-video code, model code or sample UI.
- Acceptance criteria and planned checks: tests must cover successful discard of staged clip plus sidecars, delete/metadata failure, retry, blocked new recording, duplicate/out-of-order events and recovery with completed sessions. The manager must not represent non-Allow cleanup as optional. Run `:capture-video:testDebugUnitTest`, common clean debug build, static storage-boundary search and `git diff --check`; update `docs/BUILD_NOTES.md` and this record before commit.
- Changes actually made and intentional behavioral differences: added an injected `VideoPrivateStagingStore`, durable session states, transition guardrails, cleanup retry/blocking, and startup recovery. A successful discard removes content and metadata; cleanup failure transitions to `CleanupFailed` and blocks starts. Completed promotion deletes staging and retains only a completed record. This phase owns no Android file implementation, MediaStore, backup flag, thumbnail, promoted output URI, CameraX recording, parent-review retention, or model decision; Phase 7B must provide the real integration.
- Commands, exit codes, logs/reports, device/runtime versions: after adding explicit duplicate-discard coverage, `.\\gradlew.bat --no-daemon :capture-video:testDebugUnitTest` exited 0 in 21 seconds. The report recorded 25 passing tests: the 18 Phase 6 temporal tests plus 7 staging-state tests. The common `.\\gradlew.bat --no-daemon --warning-mode all clean assembleDebug` build also exited 0 in 37 seconds (161 tasks). Raw logs are ignored under `build-logs/`; sanitized results are in `docs/BUILD_NOTES.md`.
- Gate results (passed / failed / pending), limitations and blockers: the fake-store suite proves state transitions, cleanup blocking/retry and recovery decisions, not physical/private Android filesystem behavior, backup exclusion, actual file deletion, promotion atomicity, CameraX finalization or process death. Those remain Phase 7B/10 validation gates.
- Source worktree preservation and SDK diff review: no source write/build/reset occurred. Static review found no MediaStore, public export path, thumbnail, backup, CameraX recording, host application, live model, or Firebase use in this phase; `git diff --check` passed before commit.
- Remaining work and next eligible phase (not automatically authorized): Phase 7B may bind a real Android-private file store, CameraX finalization, temporal review and approved-output promotion to these states. The Phase 4B exact-model asset/provenance blocker remains unchanged.

### Phase 7B — 2026-09-17 — complete for managed seam/recording adapter; real CameraX/file-store pending

- Checkout/branch/HEAD and source revision/status: SDK branch `codex/phase-7b-managed-video-pipeline`, based on Phase 7A commit `d29289d`; source remains read only at `historical source revision omitted` with its two pre-existing unrelated edits.
- Pre-edit exact target files / implementation steps: add a managed-video orchestration layer and synthetic integration tests under `capture-video`. It will run recovery before starting a session, bind a temporal review result to the staged session, invoke a caller-owned promoter only after Allow, and discard/retry through the Phase 7A manager on Block, Review, Failure, cancellation, or promotion failure. Add only a narrow CameraX recording lifecycle adapter where it has no file/configuration responsibility. Do not add public output paths, MediaStore, sample UI, model/runtime asset, source activity code, Firebase, or a claim of physical-camera validation.
- Acceptance criteria and planned checks: injected tests must prove approved-only promotion, non-Allow cleanup, promotion-failure cleanup, startup recovery before start, and stop/release before an abandonment cleanup. Run `:capture-video:testDebugUnitTest`, common clean debug build, static boundary review and `git diff --check`; update this plan and build notes before commit.
- Changes actually made and intentional behavioral differences: added a managed capture pipeline which runs recovery before a start, binds temporal review to its exact staging session, and invokes the approved-output promoter only after Allow. Block, Review, Failure and promoter exceptions call discard. Added a narrow `Recording` adapter for stop/close lifecycle only; it accepts no file path or output configuration. The pipeline stops and closes a recording handle before abandonment cleanup. It intentionally does not start CameraX, construct a recorder, map an output file, provide a real decoder/model evaluator, or guarantee atomic output promotion.
- Commands, exit codes, logs/reports, device/runtime versions: `.\\gradlew.bat --no-daemon :capture-video:testDebugUnitTest` exited 0 in 21 seconds. The report recorded 30 passing tests: the 25 prior video tests plus 5 managed-pipeline tests. Raw logs are ignored under `build-logs/`; sanitized results are in `docs/BUILD_NOTES.md`.
- Gate results (passed / failed / pending), limitations and blockers: synthetic integration passed for Allow-only promotion, block cleanup, promotion failure cleanup, recovery-before-start and handle release ordering. A real CameraX `Recorder` start/finalize lifecycle, Android private file store, output promotion atomicity, model/decoder integration, actual device recording and process death remain pending. Phase 4B's exact-model asset gate remains blocked.
- Source worktree preservation and SDK diff review: no source write/build/reset occurred. The new CameraX adapter is limited to `Recording.stop()` and `close()`; static review found no MediaStore, public path, sample UI, host application, live runtime, model asset, or Firebase implementation. `git diff --check` passed before commit.
- Remaining work and next eligible phase (not automatically authorized): Phase 8 can demonstrate the injected managed photo/video APIs in a sample UI, but cannot claim live capture until the pending CameraX/file-store/model gates are closed.

### Phase 8 — 2026-09-17 — complete for contract demonstrator; live capture/viewer pending

- Checkout/branch/HEAD and source revision/status: SDK branch `codex/phase-8-sample-app`, based on Phase 7B commit `8ac59d7`; source remains read only at `historical source revision omitted` with its two pre-existing unrelated edits.
- Pre-edit exact target files / implementation steps: replace the setup-only Compose surface with a small mode selector, transparent integration-status card, capture-status view, approved-media count/viewer placeholder, and cleanup-retry status. Add a pure reducer/state test seam and usage README. The sample will demonstrate only the contracts currently implemented: injected photo/video review and cleanup behavior. Do not request camera/microphone permission or add live capture controls until a functional model, private file store and CameraX recorder are wired; do not create mock approval, fake viewer content, network permission, Firebase, location, share, cloud sync or host application integration.
- Acceptance criteria and planned checks: UI states must accurately represent unavailable runtime, cleanup retry, and zero approved media without suggesting that raw media was retained. The manifest remains network-permission-free and backup-disabled. Run sample unit tests, merged-manifest inspection, common clean debug build and `git diff --check`; record the limitations before commit.
- Changes actually made and intentional behavioral differences: the setup screen now has Photo/Video mode selection, a runtime/cleanup status card, disabled managed capture control until the required runtime is wired, and an approved-media panel that begins empty and states that non-approved outcomes are not displayed. A small reducer covers mode/cleanup state. The README explains injected use and current limitations. The sample deliberately has no mock approvals, camera/microphone/network permission, live CameraX flow, viewer items, location, sharing, cloud sync or host application dependency.
- Commands, exit codes, logs/reports, device/runtime versions: `.\\gradlew.bat --no-daemon :sample-app:testDebugUnitTest` exited 0 in 23 seconds; 3 reducer tests passed. Manifest inspection confirmed `android:allowBackup="false"` and no `<uses-permission>` declaration. Raw logs are ignored under `build-logs/`; sanitized results are in `docs/BUILD_NOTES.md`.
- Gate results (passed / failed / pending), limitations and blockers: contract-demo UI and manifest gates passed. No device/screenshot/accessibility validation, Android permission flow, actual photo/video capture, model evaluation, file-store/promotion or approved-media viewer exists yet; those requirements remain blocked by or dependent on the pending runtime integrations.
- Source worktree preservation and SDK diff review: no source write/build/reset occurred. The sample has no `uses-permission`, Firebase, host application, public-media, location, share or cloud-sync implementation. `git diff --check` passed before commit.
- Remaining work and next eligible phase (not automatically authorized): Phase 9 needs an authorized local corpus and real model runtime; Phase 10 needs suitable devices plus actual capture wiring. Neither can be inferred from this contract demonstration.

### Phase 9 — 2026-09-17 — blocked: comparator complete; real model and authorized corpus unavailable

- Checkout/branch/HEAD and source revision/status: SDK branch `codex/phase-9-benchmark-tooling`, based on Phase 8 commit `82fda46`; source remains read only at `historical source revision omitted` with its two pre-existing unrelated edits.
- Pre-edit exact target files / implementation steps: add an independently authored `tools/benchmark/compare_results.py`, its standard-library unit tests, a JSONL schema/command README, and a zero-metrics `docs/validation/PARITY_REPORT.md` template. The comparator will consume only precomputed sanitized reference/SDK records keyed by opaque capture ID, compute denominator/mismatch/false-accept/false-reject/unresolved categories and score/view/timestamp/rationale/latency deltas, and reject duplicate or misaligned input. Do not import media, manifests, source execution logic, model bytes, network downloads, corpus paths, private benchmark outputs or device data.
- Acceptance criteria and planned checks: a synthetic self-test must demonstrate expected metrics and mismatch accounting; input schema must make missing labels/latency/model identity explicit rather than treating them as zero. Run Python unit tests and `git diff --check`; no Android build is required unless Android files change.
- Changes actually made and intentional behavioral differences: added a standard-library comparator that consumes only sanitized JSONL records and records decision, score, selected-view, timestamp, rationale and latency differences. It rejects duplicate/misaligned IDs. The accompanying report template marks all actual corpus/model/runtime metrics unavailable; no source script, host runner, corpus asset, raw output or model was copied.
- Commands, exit codes, logs/reports, device/runtime versions: `python tools/benchmark/compare_results.py --self-test` printed `self-test: OK`; `python -m unittest tools/benchmark/test_compare_results.py` passed 3 tests; `git diff --check` passed. No Android build was required because this phase changes only portable tooling and documentation.
- Gate results (passed / failed / pending), limitations and blockers: comparator tooling and synthetic accounting passed. The Phase 9 comparison gate is blocked because no authorized labeled corpus/matched source output and no real SDK model runtime exist. Counts in `PARITY_REPORT.md` are intentionally unavailable, not zero. This is not corpus, model, performance, safety accuracy or device parity evidence.
- Source worktree preservation and SDK diff review: source remained read only with only its two pre-existing edits. SDK changes are limited to portable benchmark tooling and sanitised documentation; no raw data/model path is tracked.
- Remaining work and next eligible phase (not automatically authorized): provide an authorized corpus outside this repository plus matched pinned source and SDK output after Phase 4B's model gate to run the comparator. Phase 11 local Maven packaging can proceed independently, but release readiness remains dependent on Phases 9 and 10.

### Phase 10 — 2026-09-17 — blocked: pending physical devices and functional runtime

- Checkout/branch/HEAD and source revision/status: SDK branch `codex/phase-10-device-matrix`, based on Phase 9 commit `f5edd10`; source remains read only at `historical source revision omitted` with its two pre-existing unrelated edits.
- Pre-edit exact target files / implementation steps: add `docs/validation/DEVICE_MATRIX.md` with reproducible device/fault scenarios, required evidence fields, stop conditions and every result marked pending. Do not add a fake device, emulator claim, model, permission flow, media fixture, APK upload, source edit or runtime workaround.
- Acceptance criteria and planned checks: matrix must cover the plan's fresh-install, offline, no-host application, API range, front/back, orientation, repeat, resource, model fault, permission, cancellation/background, storage/cleanup, process-death/recovery and exposure/backup scenarios. Verify the matrix's internal completeness and run `git diff --check`; no Android build applies to documentation-only work.
- Changes actually made and intentional behavioral differences: added a sanitised matrix with all required scenarios, device/configuration evidence fields, stop conditions and a stepwise execution outline. Every row is pending. No device, emulator, model, media, actual permission flow, file-store inspection or source operation occurred.
- Commands, exit codes, logs/reports, device/runtime versions: matrix coverage search and `git diff --check` exited 0. No Android build ran because this phase changes only documentation.
- Gate results (passed / failed / pending), limitations and blockers: device gate remains blocked. The worktree has neither suitable authorized physical devices nor a functional model/private-store/CameraX path to exercise. The matrix is a required runbook, not validation evidence.
- Source worktree preservation and SDK diff review: no source write/build/reset occurred; its two pre-existing edits remain untouched. SDK changes are confined to the matrix and phase record.
- Remaining work and next eligible phase (not automatically authorized): execute the pending rows on authorized physical devices after Phase 4B and actual capture integration are functional. Phase 11 local Maven packaging may proceed, but release readiness remains blocked on this matrix and Phase 9.

### Phase 11 — 2026-09-17 — complete for local verification; release readiness blocked by 9/10

- Checkout/branch/HEAD and source revision/status: SDK branch `codex/phase-11-local-maven`, based on Phase 10 commit `88e5dc7`; source remains read only at `historical source revision omitted` with its two pre-existing unrelated edits.
- Pre-edit exact target files / implementation steps: add `maven-publish` release publications for the four libraries using provisional `org.sakos.camera:*:0.0.0-local` coordinates and `build/local-maven`; add source jars/POM licenses/developer/SCM metadata without claiming a remote release. Create `integration-tests/consumer/` as a separate Android build that resolves those coordinates from the repository-local Maven directory and normal Android dependency repositories, then compiles a minified release against public APIs. Add `scripts/verify-local-consumer.ps1` and `docs/validation/CONSUMER_REPORT.md`. Do not use signing credentials, `publishToMavenLocal`, a remote publication repository, project/composite dependencies in the consumer, model assets, Firebase, source checkout paths or publication/upload actions.
- Acceptance criteria and planned checks: inspect generated POM/AAR/source artifacts and verify the consumer's dependency report has Maven modules, not project substitutions. Run the exact verification script, focused consumer build and common SDK clean build; record that an unavailable model prevents runtime/model-asset proof and Phases 9/10 still block release readiness.
- Changes actually made and intentional behavioral differences: added release AAR/source publications for all four libraries to `build/local-maven`, with provisional coordinates, POM license/developer/SCM metadata and no remote repository. Added a separate Android consumer build that uses Maven coordinates only, AndroidX, and minified release compilation against public types. Its initial verifier run exposed a missing consumer-local `android.useAndroidX=true` setting; the scoped fix enabled AndroidX and the exact verifier then passed. No signing, remote upload, `publishToMavenLocal`, model asset, or release coordinate was added.
- Commands, exit codes, logs/reports, device/runtime versions: `scripts/verify-local-consumer.ps1` exited 0 after publishing four release AARs and compiling the separate minified consumer release in 34 seconds; its publish pass took 15 seconds. Consumer `releaseRuntimeClasspath` showed Maven coordinates and transitive dependencies with no project/composite substitution. POMs exposed `org.sakos.camera:*:0.0.0-local`, Apache metadata and internal artifact dependencies; the safety AAR contained `assets/policy/opennsfw2_still_gate_policy.json`. The common clean SDK build exited 0 in 39 seconds (161 tasks).
- Gate results (passed / failed / pending), limitations and blockers: local Maven and minified-consumer gates passed. The consumer validates compile/minification and metadata resolution only; it does not prove real model presence/inference, offline runtime behavior, physical-device behavior, corpus parity, remote publication, or release readiness. Phases 9 and 10 remain blocked and prevent a release-ready claim.
- Source worktree preservation and SDK diff review: no source write/build/reset occurred. The consumer has no `project(...)`, composite-build substitution, production checkout path, signing configuration, or remote publication target. Generated local Maven output remains under ignored `build/` and was removed by the later clean build.
- Remaining work and next eligible phase (not automatically authorized): Phase 12 may document only verified local coordinates and limitations; it must not advertise a remote install/release. Phase 9/10 external gates remain required before release preparation.

### Phase 12 — 2026-09-17 — complete for verified local documentation; public assets/release claims pending

- Checkout/branch/HEAD and source revision/status: SDK branch `codex/phase-12-developer-docs`, based on Phase 11 commit `e601867`; source remains read only at `historical source revision omitted` with its two pre-existing unrelated edits.
- Pre-edit exact target files / implementation steps: update `README.md`, `docs/VIDEO_STORAGE.md`, and `docs/MODEL_CARD.md`; add a local-consumer quick-start and integration guide, contribution guidance, a security-status document that does not advertise an unverified reporting channel, a changelog, and a third-party notice index. Record only the verified `0.0.0-local` local Maven path and present runtime/model/corpus/device/remote-release claims as pending. Do not create screenshots, sample media, security contact addresses, badges, package installs, release notes for public delivery, external messaging, or a website deployment.
- Acceptance criteria and planned checks: runnable documentation command must match Phase 11 verifier; module/API/storage documentation must match current source and explicit limitations; link/reference and prohibited-claim search must pass. Documentation-only work does not require Gradle.
- Changes actually made and intentional behavioral differences: updated the README's current status/local consumer quick-start, corrected the storage guide to reflect the narrow existing CameraX lifecycle adapter, and expanded the model card with sampled-video/isolated-evidence limitations. Added module/integration guidance, contributing guidance, a non-endpoint security-status file, an unreleased changelog and third-party notice index. No screenshot, sample media, badge, public security contact, external service, remote install coordinate, model, metric or public release claim was added.
- Commands, exit codes, logs/reports, device/runtime versions: explicit local link-target verification passed for all documented local references; the prohibited-claim search found only existing plan text that prohibits such claims; `git diff --check` passed. No Gradle task ran because this phase changes documentation only.
- Gate results (passed / failed / pending), limitations and blockers: verified local documentation gate passed. Brand visuals/screenshots, actual model/corpus/device evidence, a verified private security-reporting channel, remote install coordinates and public-release material remain pending. Docs do not make those claims.
- Source worktree preservation and SDK diff review: no source write/build/reset occurred; its two pre-existing edits remain untouched. SDK changes are documentation/notice files only.
- Remaining work and next eligible phase (not automatically authorized): Phase 13 can implement a local responsive site with only these verified claims. Phase 14 remains blocked until the unresolved model, corpus, device, security-channel and release-target gates are closed.

### Phase 13 — 2026-09-17 — complete for local static preview; deployment and public assets pending

- Checkout/branch/HEAD and source revision/status: SDK branch `codex/phase-13-local-site`, based on Phase 12 commit `f2e0300`; source remains read only at `historical source revision omitted` with its two pre-existing unrelated edits.
- Stack/design decision: implement a dependency-free static site under `website/` using HTML, CSS and small local JavaScript only; routes are `/`, `/camera/`, and `/docs/camera/` as nested static documents. Preview with `python -m http.server`; no package installation, analytics, contact form, service worker, deployment configuration, DNS action, production host, or generated raster asset is required. The visual concept uses true white, ink navy, deep teal, pale-blue diagram panels, a simple header, hero, explanatory visual bands and a developer-status surface; the concept is not a product screenshot or a public claim.
- Pre-edit exact target files / implementation steps: add shared `website/assets/site.css`/`site.js`, three local route documents, and a site README. Implement keyboard-visible navigation, responsive grids, code-native photo/video diagrams, local links, and content limited to verified local Maven/contract status. Do not add a download/install button, fake metric, sampled-media demo, user account, externally loaded font/script/image, model/runtime claim, public security contact, Firebase or deployment artifacts.
- Acceptance criteria and planned checks: run a local server, inspect desktop/tablet/mobile routes and keyboard focus in a browser, check local links/assets, confirm no analytics/external asset requests or unsupported claims, and record screenshots/design comparison. No Android Gradle task applies.
- Changes actually made and intentional behavioral differences: added a dependency-free `website/` preview with Home, Camera and Developer guide static routes; shared local styling provides the white/navy/teal visual system, code-native photo/video diagrams, responsive grids, a visible keyboard focus treatment and a skip link. The pages describe only the current policy/capture contracts and verified local Maven consumer flow. They deliberately contain no hosted asset, model demo, capture control, form, telemetry, remote script/font/image, download claim, remote install coordinate, public security contact or deployment configuration. A generated design concept was used only as a local visual reference; it was not copied into the repository or presented as product evidence.
- Commands, exit codes, logs/reports, device/runtime versions: `python -m http.server 4173 --directory website` served the preview locally. `Invoke-WebRequest` returned HTTP 200 for `/`, `/camera/` and `/docs/camera/`. Local link/asset inspection found only relative references; a scoped unsupported-feature search found no external URL, script, form, download control, analytics or unsupported metric claim in the website content. No Android Gradle task applies to this static site.
- Gate results (passed / failed / pending), limitations and blockers: browser review passed on desktop, 753px tablet and 375px mobile layouts. Each inspected route had no horizontal overflow; the mobile header stacks cleanly, and Tab reaches the visible Skip to content link. The responsive diagrams are code-native and retain their bounds without image cropping. Local static-preview and accessibility-navigation gates passed. Hosting, DNS, public assets, a functional model/camera runtime, corpus parity, physical-device results and any public delivery remain pending.
- Source worktree preservation and SDK diff review: no source write/build/reset occurred; its two pre-existing unrelated edits remain untouched. The SDK change is limited to static local website files and this phase ledger. `git diff --check` passed before commit.
- Remaining work and next eligible phase (not automatically authorized): Phase 14 can prepare a private release checklist/runbook, but its completion remains blocked until model provenance, authorized corpus parity, physical-device validation, a verified security-reporting channel and concrete release/hosting targets are available. Phase 15 requires separate explicit public-launch authorization.

### Phase 14 — 2026-09-17 — blocked: private release preparation complete; required release gates unresolved

- Checkout/branch/HEAD and source revision/status: SDK branch will be `codex/phase-14-release-prep`, based on Phase 13 commit `b91c837`; source remains read only at `historical source revision omitted` with its two pre-existing unrelated edits.
- Pre-edit exact target files / implementation steps: add `docs/RELEASE_CHECKLIST.md` and `docs/RELEASE_RUNBOOK.md`, then update this phase ledger. The checklist will inventory each required gate, concrete evidence, owner decision and current stop condition. The runbook will sequence only private preflight/review work and place explicit stops before signing, tagging, remote publishing, repository visibility changes, website deployment, DNS or public messaging. It will record unspecified registry/version/hosting/DNS/security-channel decisions as unresolved, not as inferred targets. Do not generate a candidate model, artifact, key, tag, release, upload, website host configuration, external communication or public-facing release note.
- Acceptance criteria and planned checks: documents must agree with current provenance/model/security/local-consumer/device/parity records, clearly separate observed local evidence from missing gates, include rollback/incident escalation placeholders without inventing contacts, and contain no credential, production identifier, remote target or launch claim. Run focused cross-reference and prohibited-action searches plus `git diff --check`; documentation-only work does not run Gradle.
- Changes actually made and intentional behavioral differences: added a private release checklist that traces every model, runtime, corpus, device, security, destination, notice and authorization gate to its evidence and stop condition. Added a private preparation runbook that accepts only review inputs and explicitly stops before each external action. The files deliberately do not create a release candidate, artifact manifest, signing configuration, target endpoint, contact, version, tag, upload, deployment, DNS action or public release material.
- Commands, exit codes, logs/reports, device/runtime versions: `git diff --check` exited 0. Focused existence/cross-reference checks passed for the checklist, provenance, model card, parity template, device matrix and root security-status file. A scoped scan found no endpoint or secret; the only key/token/credential terms are instructions not to put them in source. No Gradle task ran because this phase changes only private documentation.
- Gate results (passed / failed / pending), limitations and blockers: the private checklist/runbook preparation gate passed. Phase 14 remains blocked because the exact model conversion/redistribution clearance and real runtime evidence are absent; no authorized corpus parity or physical-device validation is available; the security-reporting channel, package/registry/version/signing decision, website/DNS target and release authorization are unresolved. Local Maven verification and the local static preview do not close any of these gates.
- Source worktree preservation and SDK diff review: no source write/build/reset occurred; its two pre-existing unrelated edits remain untouched. The SDK diff is confined to two private release-preparation documents and this ledger. `git diff --check` passed before commit.
- Remaining work and next eligible phase (not automatically authorized): obtain and review the missing model provenance/runtime, corpus, device, security-channel and concrete release-target evidence before reopening Phase 14. Phase 15 requires a completed Phase 14 and a separate explicit user instruction that names each external launch action.

### Phase 4B — 2026-09-17 — blocked: refreshed source-only candidate audit

- Checkout/branch/HEAD and source revision/status: SDK branch `codex/phase-4b-provenance-recheck`, based on Phase 14 commit `f3e2d19`; source remains read only at `historical source revision omitted` with its two pre-existing unrelated edits.
- Pre-edit exact target files / implementation steps: update only `docs/PROVENANCE.md` and this ledger with a read-only candidate-identity and source-provenance recheck. Do not copy, load, package, hash into an artifact, convert, alter or redistribute the source model; do not add inference code or substitute a different model.
- Acceptance criteria and planned checks: verify the source-only file size and SHA-256 against the recorded candidate; inspect the selected provenance record for a conversion chain, redistribution notice and approval; confirm source status and authorized attached-device availability. Record what the checks can and cannot prove. Documentation-only work does not run Gradle.
- Changes actually made and intentional behavioral differences: added refreshed source-only candidate evidence to `docs/PROVENANCE.md`. No SDK model asset, runtime implementation, benchmark corpus, device result or source modification was added.
- Commands, exit codes, logs/reports, device/runtime versions: `Get-FileHash` confirmed 6,128,536 bytes and SHA-256 `051A21BF697858C1E2537354A99BE09A48D26BBFBA0C35216B340F16DE7528D7` for the source-only candidate. The selected source provenance record contained zero matches for conversion, redistribution, notice and approval. The source GitHub remote responded to `git ls-remote`; `adb devices -l` reported no attached device. No Gradle task ran.
- Gate results (passed / failed / pending), limitations and blockers: identity recheck passed, but the Phase 4B release/runtime gate remains blocked. A matching source-only file and upstream lineage references do not prove conversion provenance or redistribution rights. No real model inference is authorized, and no device is available for the downstream physical-device gate.
- Source worktree preservation and SDK diff review: source remained untouched with its two pre-existing edits. The SDK diff is documentation only; no `.tflite`, generated binary, benchmark corpus, credential, release artifact or inference implementation was introduced.
- Remaining work and next eligible phase (not automatically authorized): obtain reviewable private Gitea conversion/notice/authorization evidence before importing the exact model and running Phase 4B runtime work. After that runtime is complete, obtain an authorized corpus and physical devices to resume Phases 9 and 10.

### Phase 14 — 2026-09-17 — in progress: claim/evidence audit

- Checkout/branch/HEAD and source revision/status: SDK branch `codex/phase-14-claim-audit`, based on Phase 4B recheck commit `4e7fdd6`; source remains read only at `historical source revision omitted` with its two pre-existing unrelated edits.
- Pre-edit exact target files / implementation steps: add `docs/RELEASE_EVIDENCE.md`, update `README.md` only where existing wording can read as a current model/runtime/capture capability, and update this ledger. Build a claim-to-evidence table for the README, model card, changelog, security status and three local website routes. Classify each claim as locally verified, accurately pending, planned/future, or blocked; link supporting local records and record artifact/site state. Do not add release notes for public delivery, package artifacts, model bytes, signing, external targets, contact information, tags, uploads or deployment configuration.
- Acceptance criteria and planned checks: every retained present-tense claim must have current local evidence; planned capabilities must be visibly future tense; all artifact, publication and website hosting statements must match the tracked repository. Run focused claim/reference searches and `git diff --check`; documentation-only work does not run Gradle.
- Changes actually made and intentional behavioral differences: added `docs/RELEASE_EVIDENCE.md` with a claim-to-evidence table spanning the README, model card, integration guide, changelog, security status and local website routes. Reworded the README headline and introduction so they describe the current contract-focused development state; the existing capability table remains explicitly planned. No release note, artifact, signing path, endpoint, target, model, website deployment or public claim was created.
- Commands, exit codes, logs/reports, device/runtime versions: `git diff --check` exited 0. Focused existence/cross-reference checks passed for all cited documents and website routes. A tracked-artifact scan found no `.aar`, `.apk`, `.aab` or `.tflite` deliverable; the tracked Gradle wrapper JAR is build tooling only. Claim searches confirmed the README's bundled-model wording appears under the `Planned experience` column and current-state text records its absence. No Gradle task ran because this phase changes documentation only.
- Gate results (passed / failed / pending), limitations and blockers: the private claim/evidence audit passed. Phase 14 remains blocked because there is no cleared model/runtime, authorized corpus parity result, physical-device matrix result, verified security intake, package registry/version/signing decision, website/DNS target or external release authorization. The audit makes no claim that local Maven output or a local website preview closes these gates.
- Source worktree preservation and SDK diff review: source remained read only with its two pre-existing unrelated edits. The SDK diff is a README wording correction, private evidence table and plan ledger only. `git diff --check` passed before commit.
- Remaining work and next eligible phase (not automatically authorized): reopen the required implementation and validation phases when reviewable model authorization, corpus evidence and physical-device access are supplied. Complete concrete security/registry/hosting decisions before reopening release preparation. Phase 15 still requires a separate explicit user instruction for each external action.

### Phase 14 — 2026-09-17 — in progress: non-public candidate templates

- Checkout/branch/HEAD and source revision/status: SDK branch `codex/phase-14-candidate-templates`, based on Phase 14 claim-audit commit `42adc8e`; source remains read only at `historical source revision omitted` with its two pre-existing unrelated edits.
- Pre-edit exact target files / implementation steps: add `docs/RELEASE_MANIFEST_TEMPLATE.md` and `docs/RELEASE_NOTES_DRAFT.md`, then update this ledger. The manifest will enumerate the mandatory candidate fields, evidence links and `UNSET` placeholders; the release-note draft will be explicitly non-public and state only verified development status/known limits. Include local static-site preview instructions as a review artifact, not a deployment plan. Do not select version/registry/hosting targets, generate an artifact, sign, tag, push, upload, deploy, modify DNS, issue a public note or create an external release.
- Acceptance criteria and planned checks: templates must make all unverified values visibly unset, link the checklist/runbook/evidence audit, contain no release URL or contact/credential, and distinguish local preview from hosted website validation. Run focused placeholder/prohibited-claim checks and `git diff --check`; documentation-only work does not run Gradle.
- Changes actually made and intentional behavioral differences: added a non-public candidate-manifest template with mandatory identity, artifact, validation, website and sign-off fields marked `UNSET`. Added a private release-note draft that confines itself to verified development work and explicit limits. The local website command is included solely to review the static preview; neither file configures hosting or represents a candidate as built.
- Commands, exit codes, logs/reports, device/runtime versions: `git diff --check` exited 0. Focused template checks confirmed every release-specific identity/sign-off field is `UNSET`, all checklist/runbook/evidence/website references exist, and no endpoint, contact, secret or credential appears. The only URL is `http://localhost:4173/`, identified as a local non-hosted preview. No Gradle task ran because this phase changes documentation only.
- Gate results (passed / failed / pending), limitations and blockers: the candidate-template preparation gate passed. Phase 14 remains blocked: there is no candidate artifact or approved version/registry/host, and the model/runtime, corpus, device, security-intake and authorization gates remain unresolved. These templates cannot be filled or published from local evidence alone.
- Source worktree preservation and SDK diff review: source remained read only with its two pre-existing unrelated edits. The SDK diff is documentation/templates and this ledger only; no model, binary artifact, signing material, release tag, remote action or deployment configuration was introduced. `git diff --check` passed before commit.
- Remaining work and next eligible phase (not automatically authorized): obtain the missing external evidence and decisions listed in `docs/RELEASE_CHECKLIST.md`, then populate and privately review a real candidate manifest. Phase 15 requires a separate explicit user instruction naming each external launch action after Phase 14 has passed.

## Planning revision record — 2026-09-17

- Replaced broad delivery stages with bounded phases, dependencies, source/target boundaries, acceptance gates, common verification and a reusable Terra / High prompt.
- Read the tracked README/plan and source repository guidance; verified the source baseline, relevant filenames, trust-policy coupling and temporal policy constants through read-only inspection.
- Validation: `git diff --check` passed; SDK status shows only `docs/PROJECT_PLAN.md` modified. Source status still shows only the two pre-existing edits recorded above. Gradle was not run because this revision changes documentation only.
- Modified only this plan. No SDK source/model import, source-repository edit, Android build, commit, publication or deployment was performed. Full provenance review, full source characterization and implementation remain future phase work.

### Phase 1 provenance amendment — 2026-09-17

- The user explicitly authorized extraction of the camera/gallery application work. The inspected source remote is under the user's account, and Git history for the selected spatial, runtime, photo and video candidates contained only `mendypan` or `Mendi Yuda` author names.
- `docs/PROVENANCE.md` and `docs/EXTRACTION_MANIFEST.md` now clear selective export of those project-owned code/policy/benchmark candidates while preserving source revision/hash records and exclusions. The exact model asset, private SakOS upstream provenance, unreviewed third-party material, private corpus/media and artwork remain blocked.
- This amendment enables cleared algorithm/capture work in Phases 4A and 5–7. It does not authorize model import, Phase 4B execution, remote publication or public release.
