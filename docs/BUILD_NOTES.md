# Local build notes

## Final expanded private candidate gate - 2026-10-01

The full verifier passed twice from clean source commit
`6d37cb88e519705a7edb5c28d1ce607fa6685d81`:
`scripts/verify-local-candidate.ps1 -Serial <isolated-emulator-serial>`.
The final evidence/inspection-only ledger commit changes no artifact code.
The manifest records artifact-build and generation revisions separately.

| Check | Verified result |
| --- | --- |
| Clean source/debug/test APK build | Passed with installed offline Gradle 8.13 / AGP 8.13.2 / Kotlin 2.0.21 / JBR 21.0.10 |
| JVM tests | 185 passed; core 19, model/policy 46, camera 68, video 49, sample 3; zero failures/errors/skips |
| Synthetic Android tests | 12 passed; decoder/private playback/recovery 5, bundled strategies 2, sample permission/capture/calibration/front-back/recreation/orientation/cancel 4, minified Maven consumer 1 |
| Lint | Zero errors; 43 warnings: core 2, camera 15, sample 20, consumer 6; model/video 0 |
| Local Maven/minified consumer | Four AAR/source/POM/module sets, unsigned release and test-key localRuntime/test APKs passed; both strategies, calibration serialization and standalone private save/preview/authorized fake destination exercised |
| Artifact/notices/model inspection | 24 artifacts/checksums; exact notice bytes in all eight AAR/source archives; exact 6,128,536-byte model hash retained |
| Repeat comparison | All 16 Maven artifacts and 23/24 overall hashes identical; only the test-key localRuntime APK differed; both executions passed |
| Permissions | No network, microphone or public-media/storage permission in sample/consumer APKs |
| Current heuristic text audit | 134 current source text files; zero secret candidates and zero media/signing filenames; not rights or historical clearance |
| Local docs/static links | 26 prospective-public prose files, 38 local links, zero broken links; no network used |
| Sanitized logs/diff | Raw workspace/user paths absent in candidate logs; git diff --check passed |

Warnings are dependency/version/catalog advisories and sample/consumer
backup/icon/KTX/English-text/touch-accessibility suggestions. They do not
establish API-range, OEM backup, accessibility or physical-device coverage.

An earlier clean attempt exposed a fail-closed directory-commit failure.
Atomic directory move with bounded recoverable retries and repeated cancellation/
host guards resolved it; collision cleanup/retry has a deterministic regression.
No non-atomic copy fallback is used. No meaningful test was removed or skipped.
The requested offline Gradle connected-test attempt remained blocked by uncached
UTP runner components. The verifier compiles the instrumented APKs and executes
the same AndroidJUnitRunner suites directly against the explicitly provided
isolated API 36 x86_64 emulator. No physical device was accessed.

Ignored local outputs: `build/private-candidate/manifest.json`, `SHA256SUMS.txt`,
sanitized build/test logs and retained notices; Maven files in `build/local-maven`;
APKs/mapping in module outputs. A same-machine/toolchain repeat is recorded;
universal signed-APK byte reproducibility is not claimed. Current tests use
benign generated geometry/solids, mocked decisions/scores and the isolated
emulator scene only. No accuracy, numerical/source parity, universal hardware,
legal redistribution, public intake or external delivery is established.
Owner/legal weight/conversion/dependency rights, physical/API-range coverage,
real-world efficacy, independent-validation status and all external decisions
remain open. Independent validation is deferred/unverified.

During final interruption recovery, a surviving verifier had reassembled the
same implementation with evidence-only edits present. It was stopped before
another complete verification run. All 23 artifacts other than the test-key
localRuntime APK match the preserved second passed candidate byte for byte.
The final manifest records this recovery separately from the two clean passed
gates. The reassembled localRuntime APK is inspected packaging of the same
tested source; execution of that particular signed byte variant is not claimed.
Passed Android logs are retained from the completed gates, not the interrupted
attempt. No implementation changed and no additional suite was run for the
ledger. Restored clean-build provenance derives from the preserved first manifest.

## Earlier expanded implementation gate - 2026-10-01

The owner expanded completion after `d1f24fe` to require source-derived first-run
camera tooling, live Adaptive14 execution and standalone gallery privacy/save.
TOOLING_PARITY pins the read-only code inventory and deliberate SDK differences.
That intermediate source gate passed 184 JVM tests (core 18, model/policy 46, camera
68, video 49, sample 3), with no failures/errors/skips. Eleven source Android
tests pass: decoder/private playback/recovery 5, bundled strategies 2 and sample
permission/capture/calibration/front-back/recreation/orientation/cancellation 4.
The separate minified Maven consumer also passes both real runtime strategies,
calibration serialization and simulated-approval private save/preview/export.
The earlier 94-JVM/six-Android snapshots below are retained historical build
records and are superseded by this expanded implementation.

The final clean candidate and Maven repeat evidence is recorded above.
These checks used generated benign geometry/solid colors, simulated
scores, mocks and the isolated emulator scene only. They establish managed-path,
ownership/cache/serialization/runtime mechanics; no accuracy, numerical parity,
physical camera/API-range or legal redistribution conclusion follows.

The final ownership gate adds unconditional calibration unlock after cleanup
exceptions, full probe-contract validation, shared private-root write ownership,
active playback leases across clients, retryable orphan cleanup and export
counts/committed IDs that preserve a successful save after terminal cleanup fails.
Standalone details are in CAMERA_TOOLING and REVIEWED_LIBRARY.

## Earlier authorized completion — 2026-10-01 — bridge implementation gate

The installed Gradle 8.13 / AGP 8.13.2 / Kotlin 2.0.21 / JBR 21.0.10
toolchain completed `testDebugUnitTest assembleDebug assembleDebugAndroidTest`
offline. This compiles the functional sample and synthetic Android test APKs.
JVM tests cover capture/configuration mismatch, cancellation after simulated
Allow, decoder release failure, recording stop failure and interrupted metadata
recovery in addition to the existing suites. No accuracy or physical-device
result follows from this gate. Android execution and final candidate checks
are recorded separately below when complete.

The Windows sandbox denied a Java ZIP archive operation inside the installed
Gradle cache; local build execution used the existing toolchain outside that
sandbox. The wrapper attempted a download before explicit GRADLE_USER_HOME was
set; subsequent checks use the already installed offline Gradle distribution.

These notes contain sanitized current synthetic build/runtime evidence and
dated earlier local outcomes. Independent validation details remain outside
the repository.

## Earlier functional sample gate — 2026-10-01

Direct instrumentation on the coordinator-provided isolated API 36 x86_64
emulator passed two decoder/recovery tests, one bundled-runtime test and two
sample tests. The sample flow covers initially denied camera permission,
grant/recreation, photo review, silent video review, cancel, background cleanup,
approved-store receipt matching and interrupted pending-write recovery. A later
run also exercises the approved photo viewer. Inputs are locally generated
benign shapes/solid-color frames, simulated decisions and the emulator scene.
These outcomes do not establish model accuracy, physical camera behavior or
source parity. No physical device was accessed.

Gradle connected-test execution required uncached UTP runner components. The
repeat verifier instead compiles instrumented APKs with the installed offline
toolchain and executes the same AndroidJUnitRunner tests directly through adb.
Every command targets the explicitly supplied emulator serial and verifies
emulator identity. The verifier resets only its own synthetic sample package.

## Earlier complete private candidate gate — 2026-10-01

`scripts/verify-local-candidate.ps1 -Serial <isolated-emulator-serial>` completed
successfully with the installed offline toolchain, then passed a complete repeat
from clean reviewed commit `e76718e`. The final ledger-only commit changes no
artifact code; the generated manifest records build and generation commits separately.

| Check | Result |
| --- | --- |
| Clean debug build and Android test APK compilation | Passed |
| All JVM tests | 94 passed, zero failures/errors/skips: core 4, model/policy 33, photo 8, video 46, sample state 3 |
| Synthetic Android instrumentation | 6 passed: decoder/recovery 2, bundled runtime 1, sample permission/capture/cancel/background/approved-viewer/store 2, minified Maven consumer runtime 1 |
| Lint | Zero errors; 19 warnings across root modules and consumer (version/catalog, sample English strings/icon/KTX, consumer backup/icon/toolchain advisories) |
| Local Maven and separate R8 consumer | Four AAR/source/POM/module publications; unsigned release plus test-key-only localRuntime build passed |
| Artifact inspection | 24 artifact/hash entries including test APKs; model bytes and AAR/source notices verified |
| APK permission inspection | No network, microphone or public-media/storage permission |
| Current private heuristic text audit | 111 source text files; zero secret candidates and zero tracked media/signing filenames |
| Prospective-public text and static site links | No audited stale current claims or broken local site links |

Candidate outputs are ignored under `build/private-candidate`: manifest,
SHA256SUMS, sanitized build/test logs and retained notices. Actual Maven binaries
remain under `build/local-maven`; APKs and R8 mapping remain in their module
output directories. No real release signing or external delivery occurred.

The repeat verifier records the exact build-source commit separately from the
manifest-generation commit and flags tracked dirty state. Source JARs and AAR
Java resources carry module-specific notice paths. The localRuntime consumer
keeps Kotlin/tracing test-runner support; the unsigned release uses normal R8
consumer rules. Optional annotation warning suppression is test-APK-only.

These are current synthetic mechanics checks on one API 36 x86_64 emulator.
API 26/physical-camera coverage, real-world efficacy/parity, owner/legal model
and dependency clearance, independent-validation status, intake and all external
release decisions remain open. Historical private wording findings stay outside
this repository and are not evidence that historical validation was performed.

The repeat compared all 24 inventoried hashes with the first complete candidate:
23 matched, including every one of the 16 Maven AAR/source/POM/module files.
Only the test-key localRuntime APK differed. The current exact hash is in
SHA256SUMS and the manifest; this observation does not establish universally
reproducible APK bytes or reproducibility on another toolchain/machine.

## Historical Phase 3 — bundled model runtime — 2026-09-17

The `safety-opennsfw2` focused local verification and the common clean debug
build passed after adding the authorized model asset, integrity preflight,
LiteRT Bitmap runtime, and fail-closed evaluator adapter. The asset matched
its recorded size and SHA-256.

## Earlier local phases

The safety policy, CameraX photo bridge, video staging/finalization bridge,
local Maven consumer, documentation, and static-site preview each completed
their recorded local checks. Those outcomes do not establish device behavior,
external publication, or release readiness.
