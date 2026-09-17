# Build notes

This file records sanitized local build evidence for Android-affecting phases.
Raw logs are kept under ignored `build-logs/` and are not committed because
they can include machine-specific paths.

## Phase 2 — 2026-09-17

Environment: Windows, Android SDK at the configured local SDK path, and
OpenJDK 21.0.10 from Android Studio. Gradle wrapper: 8.13. Android baseline:
compile/target SDK 36, min SDK 26, AGP 8.13.2, Kotlin 2.0.21, Java bytecode 11.

| Check | Result | Evidence |
| --- | --- | --- |
| Generate wrapper | Passed | `gradle-8.13/bin/gradle.bat --no-daemon wrapper --gradle-version 8.13 --distribution-type bin`; `build-logs/phase-2-wrapper.log`; 31 seconds |
| Inspect sample runtime graph | Passed | `.\\gradlew.bat --no-daemon :sample-app:dependencies --configuration debugRuntimeClasspath`; `build-logs/phase-2-dependencies.log`; 12 seconds. It resolves the four planned SDK modules through project dependencies. |
| Clean debug build | Passed | `.\\gradlew.bat --no-daemon --warning-mode all clean assembleDebug`; `build-logs/phase-2-clean-debug.log`; 61 seconds; 153 actionable tasks. |
| Diff whitespace | Passed | `git diff --check` |

Observed non-failing build messages:

- SDK processing reported XML version 4 while this tool understands up to 3,
  indicating an Android command-line-tools/SDK metadata version mismatch.
- Debug packaging retained several native CameraX/LiteRT libraries because they
  could not be stripped. This is expected for the unimplemented skeleton and
  must be revisited when release packaging is introduced.

This is a local debug-build result only. It does not prove model execution,
camera behavior, device operation, local Maven consumption, or release
readiness.

## Phase 3 — 2026-09-17

| Check | Result | Evidence |
| --- | --- | --- |
| Safety-core contract tests | Passed | `.\\gradlew.bat --no-daemon :safety-core:testDebugUnitTest`; `build-logs/phase-3-focused.log`; 16 tasks, 23 seconds. Tests cover invalid values, capture-bound Allow approval, Block/Review/cancelled non-approval, and unavailable/closed evaluator failure. |
| Clean debug build | Passed | `.\\gradlew.bat --no-daemon --warning-mode all clean assembleDebug`; `build-logs/phase-3-clean-debug.log`; 155 tasks, 30 seconds. |
| Core boundary review | Passed | source search found no host application, Rega capture trust, CameraX, Compose, LiteRT, tensor, threshold or Firebase use in `safety-core` implementation. |
| Diff whitespace | Passed | `git diff --check` |

The same non-failing SDK XML metadata warning and unstripped debug native
library message observed in Phase 2 occurred during the clean build. They do
not change the Phase 3 contract result and remain release-packaging follow-up
items.

## Phase 4A — 2026-09-17

| Check | Result | Evidence |
| --- | --- | --- |
| Spatial policy regression tests | Passed | `.\\gradlew.bat --no-daemon :safety-opennsfw2:testDebugUnitTest`; `build-logs/phase-4a-focused.log`; 28 tests, 25 seconds. |
| Policy content comparison | Passed | Source raw working-copy SHA-256 `d06a495fa9f777321a240516c0dd624033f13fbec1be002d75310bf9f5417583`; source and SDK LF-normalized content SHA-256 `4e347ada472d81713d2629e2a90c9b23f91f644cd0b584eae61838a5c07f50ff`. |
| Source boundary review | Passed | no `.tflite`, host application, trust snapshot, Firebase, Google Services or Crashlytics content in `safety-opennsfw2`. |
| Clean debug build | Passed | `.\\gradlew.bat --no-daemon --warning-mode all clean assembleDebug`; `build-logs/phase-4a-clean-debug.log`; 157 tasks, 35 seconds. |
| Diff whitespace | Passed | `git diff --check` |

The Phase 2 SDK XML metadata and unstripped debug-native-library messages
recurred without failing the build. Phase 4A does not execute a model or close
runtime/device/corpus parity gates.

## Phase 4B model preflight — 2026-09-17

| Check | Result | Evidence |
| --- | --- | --- |
| Model preflight tests | Passed | `.\\gradlew.bat --no-daemon :safety-opennsfw2:testDebugUnitTest`; `build-logs/phase-4b-preflight-focused.log`; 27 actionable tasks, 25 seconds. |
| Clean debug build | Passed | `.\\gradlew.bat --no-daemon --warning-mode all clean assembleDebug`; `build-logs/phase-4b-preflight-clean-debug.log`; 157 tasks, 36 seconds. |
| Diff whitespace | Passed | `git diff --check` |

The preflight tests cover missing asset, short asset, correct-length/wrong-
digest asset, a closed evaluator, model/preprocessing/policy identity, and the
absence of an approval path. They do not test an actual model, tensor shapes at
runtime, LiteRT, camera input, corpus accuracy, device behavior, or release
packaging. The recurring SDK XML and debug-native-library messages remain
non-failing environment/package observations.

## Phase 5 — 2026-09-17

Focused `:capture-camerax:testDebugUnitTest` passed in 22 seconds. It covers
non-Allow no-write, Allow single delivery, duplicate suppression, output
failure and frame closure. `--warning-mode all clean assembleDebug` passed in
36 seconds (159 tasks). This is injected-evaluator pipeline evidence only;
real CameraX/device/model behavior remains pending.

## Phase 6 — 2026-09-17

| Check | Result | Evidence |
| --- | --- | --- |
| Temporal planner and aggregation tests | Passed | `.\\gradlew.bat --no-daemon :capture-video:testDebugUnitTest`; `build-logs/phase-6-focused.log`; 18 tests, 23 seconds. |
| Static source-boundary review | Passed | No host application, `MediaMetadataRetriever`, temporary-video, Camera Activity, live-runtime, model-asset, or Firebase reference in `capture-video`. |
| Clean debug build | Passed | `.\\gradlew.bat --no-daemon --warning-mode all clean assembleDebug`; `build-logs/phase-6-clean-debug.log`; 161 tasks, 37 seconds. |
| Diff whitespace | Passed | `git diff --check` |

The test suite uses only synthetic closeable frames and injected evaluations. It
does not validate Android decoding, model inference, corpus parity, recording,
private staging, device behavior, or release packaging. The recurring SDK XML
metadata warning and unstripped debug-native-library message remain
non-failing environment/package observations.

## Phase 7A — 2026-09-17

| Check | Result | Evidence |
| --- | --- | --- |
| Staging state-machine tests | Passed | `.\\gradlew.bat --no-daemon :capture-video:testDebugUnitTest`; `build-logs/phase-7a-focused.log`; 25 tests, 21 seconds. |
| Clean debug build | Passed | `.\\gradlew.bat --no-daemon --warning-mode all clean assembleDebug`; `build-logs/phase-7a-clean-debug.log`; 161 tasks, 37 seconds. |
| Diff whitespace | Passed before commit | `git diff --check` |

The focused test uses an injected in-memory store to verify cleanup, retry,
transition rejection, blocking, and recovery. It is not Android filesystem,
backup, CameraX, media-provider, process-death, or release evidence.

## Phase 7B — 2026-09-17

| Check | Result | Evidence |
| --- | --- | --- |
| Managed video pipeline tests | Passed | `.\\gradlew.bat --no-daemon :capture-video:testDebugUnitTest`; `build-logs/phase-7b-focused.log`; 30 tests, 21 seconds. |
| Clean debug build | Passed | `.\\gradlew.bat --no-daemon --warning-mode all clean assembleDebug`; `build-logs/phase-7b-clean-debug.log`; 161 tasks, 37 seconds. |
| Diff whitespace | Passed before commit | `git diff --check` |

These injected tests do not validate a real CameraX recorder/finalize callback,
Android file store, actual decoder/model evaluator, output promotion atomicity,
physical camera, or process-death recovery.

## Phase 8 — 2026-09-17

| Check | Result | Evidence |
| --- | --- | --- |
| Sample reducer tests | Passed | `.\\gradlew.bat --no-daemon :sample-app:testDebugUnitTest`; `build-logs/phase-8-focused.log`; 3 tests, 23 seconds. |
| Manifest inspection | Passed | `android:allowBackup="false"`; no `<uses-permission>` declaration, including network/camera/microphone. |
| Clean debug build | Passed | `.\\gradlew.bat --no-daemon --warning-mode all clean assembleDebug`; `build-logs/phase-8-clean-debug.log`; 161 tasks, 38 seconds. |
| Diff whitespace | Passed before commit | `git diff --check` |

The sample is a contract demonstrator. It does not prove a camera/permission
flow, model execution, real photo/video capture, approved-media viewer,
accessibility/device behavior, or release packaging.

## Phase 9 — 2026-09-17

| Check | Result | Evidence |
| --- | --- | --- |
| Portable comparator self-test | Passed | `python tools/benchmark/compare_results.py --self-test`; `self-test: OK`. |
| Portable comparator unit tests | Passed | `python -m unittest tools/benchmark/test_compare_results.py`; 3 tests. |
| Diff whitespace | Passed | `git diff --check` |

No corpus, source-reference result, model runtime, device identity, or latency
measurement was available. The comparator is ready for sanitized external
inputs, but it is not parity or performance evidence by itself.

## Phase 10 — 2026-09-17

`docs/validation/DEVICE_MATRIX.md` was added as a documentation-only device
runbook. Its coverage search and `git diff --check` passed. No Android build,
emulator, physical device, model runtime, capture flow, media fixture, or
device evidence was used; every validation row remains pending.

## Phase 11 — 2026-09-17

| Check | Result | Evidence |
| --- | --- | --- |
| Local publish + minified consumer | Passed after consumer AndroidX fix | `.\\scripts\\verify-local-consumer.ps1`; `build-logs/phase-11-local-consumer.log`; four provisional release AARs published locally, separate consumer `assembleRelease` passed. |
| Consumer Maven dependency graph | Passed | `.\\gradlew.bat --no-daemon -p integration-tests\\consumer :app:dependencies --configuration releaseRuntimeClasspath`; Maven coordinates and transitive dependencies, no project substitution. |
| POM/AAR inspection | Passed | Four source/AAR/POM/module artifacts observed before clean; POMs include provisional coordinates and Apache metadata; safety policy asset present in safety AAR. |
| Clean debug build | Passed | `.\\gradlew.bat --no-daemon --warning-mode all clean assembleDebug`; `build-logs/phase-11-clean-debug.log`; 161 tasks, 39 seconds. |
| Diff whitespace | Passed before commit | `git diff --check` |

The first separate-consumer run correctly failed because its AndroidX setting
was absent; `integration-tests/consumer/gradle.properties` fixes only that
consumer build setting. The later verifier passed. Local Maven verification is
not remote publication, model/runtime proof, corpus parity, device validation,
or release readiness.

## Phase 12 — 2026-09-17

Documentation-only verification passed: all explicit local documentation targets
exist, the prohibited-claim search found no new advertising claim, and
`git diff --check` passed. No Gradle build ran. The documentation deliberately
does not create a public security-reporting endpoint, model/runtime claim,
corpus/device metric, remote install coordinate, or release claim.

## Phases 7A/7B private-store continuation — 2026-09-17

| Check | Result | Evidence |
| --- | --- | --- |
| Private staging and CameraX-output tests | Passed | `.\gradlew.bat --no-daemon :capture-video:testDebugUnitTest`; 38 actionable tasks, 23 seconds. The suite covers persisted/backup sessions, clip/sidecar cleanup, orphan/malformed metadata recovery, path-traversal rejection, CameraX output preparation, and the existing review/promotion lifecycle. |
| Common clean debug build | Passed | `.\gradlew.bat --no-daemon --warning-mode all clean assembleDebug`; 161 tasks, 41 seconds. |
| Static boundary review | Passed | `capture-video` uses `Context.noBackupFilesDir` and CameraX `FileOutputOptions`/`PendingRecording`; no MediaStore, external/public-path, network, model or permission addition was found. |

The new `AndroidVideoPrivateStagingStore` roots production staging in
`Context.noBackupFilesDir`; its filesystem behavior is exercised with JVM temp
directories. This does not constitute device filesystem, CameraX finalization,
real model, promotion-atomicity, process-death, corpus, or physical-device
evidence.

## Phase 5 CameraX callback continuation — 2026-09-17

| Check | Result | Evidence |
| --- | --- | --- |
| Managed photo callback tests | Passed | `.\gradlew.bat --no-daemon :capture-camerax:testDebugUnitTest`; 27 actionable tasks, 20 seconds. Covers CameraX error reporting with no evaluation/save and success-path pipeline delegation with exactly-once proxy closure. |
| Common clean debug build | Passed | `.\gradlew.bat --no-daemon --warning-mode all clean assembleDebug`; 161 tasks, 40 seconds. |
| Static boundary review | Passed | CameraX `OnImageCapturedCallback`, `ImageProxy` and `takePicture` are the only new integration APIs; no MediaStore, file output, EXIF/location, network, model or permission addition was found. |

The bridge compiles against CameraX `OnImageCapturedCallback` and `takePicture`
signatures. It is not device, optics/orientation, permission, lifecycle,
model-runtime, storage, corpus, or release evidence.
