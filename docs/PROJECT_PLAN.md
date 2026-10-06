# SakOS Camera SDK implementation plan

## Model source evidence and conversion planning — 2026-10-05

The owner requested reviewing the historical model's original conversion and
distribution evidence, then authorized a reproducible replacement and the local
verification recorded here. Private source-project names, revisions, and workstation
paths are omitted from this publication copy.

Execution plan:

1. Inspect current source files, reachable Git history, model-cache locations,
   and embedded model metadata. Check the public upstream weight source and
   licenses referenced by the source project.
2. Record confirmed findings and remaining gaps in `docs/PROVENANCE.md` and
   `docs/MODEL_CARD.md`; save `docs/MODEL_CONVERSION_PLAN.md` with source/rights,
   dependency locking, conversion, comparison, integration, and rollback gates.
3. Add a locked, isolated converter, explicit pinned weight acquisition,
   complete attribution, deterministic comparison, and FlatBuffer inspection.
4. Run the full local SDK gate and both isolated API 36 runtime suites; record
   the measured model choice and results before committing and pushing.

Completed: historical source records reported OpenNSFW2/Yahoo lineage for the old import but did not identify its original weights or conversion recipe. A new
public-source float32 conversion was pinned, locked, reproduced twice, and passed
the predeclared source-Keras tolerance. It now replaces the inherited asset.
Attribution and the published MIT/BSD-2-Clause license conditions are recorded
and included in the model AAR/source JAR and Maven POM. The clean non-device
repository gate passed against the replacement: 185 JVM tests, zero lint errors,
24 inspected artifacts, and a passing minified consumer build. No device test
was run, as requested. Current hashes, licensing basis, and gate results are
recorded in `docs/model-conversion/` and `docs/BUILD_NOTES.md`.

## Model origin and attribution audit — 2026-10-05

The owner requested documenting the historical model attribution in this SDK repository. The related source project was left unchanged; private names, revisions, and paths are omitted.

Execution plan:

1. Verify the source provenance/model contract, original asset import revision,
   and SHA-256 identity against the SDK asset; cross-check the upstream credits
   and retained license wording.
2. Update `third_party/NOTICE.md`, `docs/PROVENANCE.md`, `docs/MODEL_CARD.md`,
   and `README.md` with the upstream authors, source links, import chain, and
   distinction between confirmed lineage and unrecorded conversion details.
3. Check local links and the diff. Build only the four library release AARs and
   source JARs needed to inspect the updated notices, using the existing offline
   toolchain. Verify exact notice/license bytes and the unchanged bundled model.
4. Record the checks here. No release publication or site deployment is needed.

Acceptance: clear upstream credits, complete retained license texts, a distinct historical-import record, and unchanged model bytes at the time of that audit. Private source identifiers are omitted from this publication copy.

Completed: historical source records and the original import were checked against the recorded size and SHA-256. Private project revisions and paths are omitted. The retained license wording matches the checked upstream texts;
both license files are unchanged. Explicit credits, upstream links, the import
chain, and unrecorded conversion details are documented in the planned files.

The offline library packaging build passed. All eight AAR/source archives retain
the updated notice and complete licenses byte for byte (32 file comparisons);
the model-bearing AAR retains the exact model digest. Local documentation links
and `git diff --check` passed. The historical source checkout remained unchanged.
Exact commands and private check-output paths are recorded in `docs/BUILD_NOTES.md`.
At the time of this audit, the old model's conversion details and distribution
basis were unrecorded. The follow-up conversion and license review below replace
that asset and document the selected model's reproducible source and license terms.

## Repository integration and helper-site launch — 2026-10-05

The owner authorized correcting the documentation, integrating the latest worktree
into `main`, pushing the repository branches, reducing publication gates to the
minimum for each deliverable, and deploying the static helper site with the
signed-in Wrangler account. This authorization supersedes older merge/push/site
deployment restrictions below. SDK package publication, release tagging, production
signing and repository visibility changes are separate future actions.

Execution plan:

1. Correct CHANGELOG and RELEASE_EVIDENCE for the October 2 Samsung result;
   align README, release documents, contribution guidance, model/integration
   limits, candidate-manifest metadata and site copy with the minimum gates.
2. Keep helper-site publication independent of model redistribution and broader
   validation. For an experimental SDK, retain distribution rights/notices,
   versioned artifact verification and accurate limitations; move broad OEM/API,
   efficacy and independent validation into a disclosed validation backlog.
3. Check local links, text consistency, Python syntax and `git diff --check`.
   Preserve existing artifact/test evidence; no production Android change or
   redundant Android rebuild is planned.
4. Commit the documentation/metadata changes, fast-forward `main`, and push all
   local branches to the existing origin without force-pushing or deleting refs.
5. Use Wrangler 4.147.0 and the verified account to create/reuse a Pages project
   for `website`, deploy only the static payload, and verify HTTPS routes/assets.
   The active `sakosproject.org` zone is in the same account. If DNS automation
   is unavailable, deliver the Pages hostname and exact dashboard domain steps.
6. Record deployment and verification results here and in website/README; commit
   and push the final evidence so both worktrees finish at the same revision.

Acceptance: both worktrees clean and at current `main`; remote refs match local;
corrected claims and minimum publication requirements agree; hosted HTML/CSS and
navigation pass checks; no credentials, Android artifacts or model bytes are in
the site deployment; apex status or manual attachment steps are recorded.

Progress: plan recorded before implementation. Wrangler authentication and Pages
write permission verified. The target zone is active on the same account's Free
plan. No SakOS Pages project existed in the account at this audit.

Pre-integration checks passed: 26 documentation/site files and 41 local links,
zero broken links; Python candidate-inspection and PowerShell deployment-script
syntax; Wrangler configuration parsing; `git diff --check`. Current text audit
inspected 136 text files with zero secret candidates or media/signing filenames.
The prior review verified all 24 existing artifact hashes and the model digest.
Only prose, release metadata and static deployment tooling changed; existing
Android verification evidence is retained without another Android/device suite.

Repository integration completed at `75a3545`: `main` fast-forwarded to the
implementation branch and all local branches were pushed atomically to origin.
Wrangler 4.147.0's default Pages-to-Workers delegation failed before creating a
project/deployment. Installed CLI code confirms `--force` selects actual Pages;
the create/upload commands were corrected for the owner's requested Pages target.

The explicit Pages project creation succeeded. The first upload was rejected
before deployment because Pages config does not support `account_id`; account
selection was moved to the script's temporary process environment. Existing
Pages projects upload directly, so `--force` is retained only for initial creation.

Initial production deployment succeeded at `e691942`, deployment
`03e28380-f1d9-4368-bb2a-d01cf3ae9c5e`. All four HTTPS routes at
`https://sakosproject.pages.dev` returned 200 and matched the upload byte-for-byte.
The project has only its Pages hostname; `sakosproject.org` has no apex A/AAAA
record. The current OAuth scopes omit DNS write, so the owner-requested fallback
is a working Pages address plus the exact Custom domains dashboard steps in
website/README. Footer navigation was made relative so it works before and after
domain attachment. A final static-only deployment records that small link change.

Final helper-site gate passed: production deployment
`85674b0d-8103-475c-b4d3-916cbd58b489` from `6b4441a`, with only three HTML files
and CSS. All four stable-host HTTPS routes returned 200 and were byte-identical
to the committed upload; local checks cover 26 files/44 links with zero broken
links. Deployment-script syntax passed after account-selection repair. All
24 local branches are retained on origin. The final evidence-only commit is
fast-forwarded into both active worktrees and pushed as the completion sequence.
No Android implementation, model, signing or repository visibility changed.
The only remaining helper-site action is owner dashboard attachment of the apex,
using website/README's steps; the working Pages hostname is already public.

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
