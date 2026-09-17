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
