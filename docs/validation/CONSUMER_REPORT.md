# Local Maven consumer report

The authorized private verifier creates four provisional artifacts:

```text
org.sakos.camera:safety-core:0.0.0-local
org.sakos.camera:safety-opennsfw2:0.0.0-local
org.sakos.camera:capture-camerax:0.0.0-local
org.sakos.camera:capture-video:0.0.0-local
```

The repository-local Maven directory is `build/local-maven`. Each publication
includes an AAR, sources, POM, Gradle module metadata and retained notices.
`integration-tests/consumer` resolves these coordinates without project/composite
substitution or a production checkout dependency. Root clean removes the local
Maven output, so the verifier recreates it before building the consumer.

The separate `release` build uses R8 and remains unsigned. An additional
`localRuntime` minified variant uses only the standard Android debug/test key for
isolated-emulator integration; this is not real release signing. Its
AndroidJUnitRunner test executes the Maven-packaged Bitmap runtime on generated
benign blue pixels. It asserts mechanics, not a particular content decision or
accuracy. Test-only support rules preserve shared Kotlin/tracing classes needed
by instrumentation, and suppress two optional compile-only Error Prone annotation
warnings only in the test APK. The unsigned release keeps normal consumer rules;
no SDK/LiteRT-specific keep rules are added to it.

Run `scripts/verify-local-consumer.ps1 -Serial <isolated-emulator-serial>` for the
local build and synthetic runtime check, or omit Serial for packaging only.
The full candidate script additionally inspects artifact model/notices/POMs,
permissions and hashes. See BUILD_NOTES for exact executed results and limits.

Local consumption and synthetic runtime evidence do not establish accuracy,
source parity, physical-device/API-range behavior, legal redistribution clearance,
remote availability or release readiness. The 2026-09-17 Phase 11 result was a
compile-only earlier milestone; current runtime evidence is recorded separately.
