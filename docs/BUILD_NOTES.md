# Local build notes

## Authorized completion — 2026-10-01 — bridge implementation gate

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

## Functional sample gate — 2026-10-01

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

## Complete private candidate gate — 2026-10-01

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
