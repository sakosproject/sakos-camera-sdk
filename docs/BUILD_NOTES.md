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
