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

## Phase 3 — bundled model runtime — 2026-09-17

The `safety-opennsfw2` focused local verification and the common clean debug
build passed after adding the authorized model asset, integrity preflight,
LiteRT Bitmap runtime, and fail-closed evaluator adapter. The asset matched
its recorded size and SHA-256.

## Earlier local phases

The safety policy, CameraX photo bridge, video staging/finalization bridge,
local Maven consumer, documentation, and static-site preview each completed
their recorded local checks. Those outcomes do not establish device behavior,
external publication, or release readiness.
