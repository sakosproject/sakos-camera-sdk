# Local build notes

## Model provenance/conversion candidate and fresh API 36 gates — 2026-10-05/06

The read-only `private camera/gallery source project` investigation found the inherited TFLite asset,
its recorded OpenNSFW2/Yahoo lineage, and embedded conversion metadata, but no
original HDF5 weights, weight digest, or executable conversion recipe. The SDK
now records that evidence, credits the upstream authors, retains the applicable
license texts, and keeps a separate source/input/conversion manifest for a new
public-source candidate. See `docs/PROVENANCE.md`,
`docs/MODEL_CONVERSION_PLAN.md`, and `docs/model-conversion/`.

Two isolated CPU conversions from the pinned OpenNSFW2 release weights were
byte-identical across repeat runs. The float32 candidate met the predeclared
`1e-4` Keras comparison tolerance. The dynamic-range candidate exceeded its
predeclared `1e-2` maximum-error tolerance on generated black pixels, despite
matching the inherited TFLite output on the eight generated patterns. Neither
candidate was selected; the existing 6,128,536-byte model remains at SHA-256
`051A21BF697858C1E2537354A99BE09A48D26BBFBA0C35216B340F16DE7528D7`.
Synthetic comparisons and emulator checks do not establish broad model parity,
accuracy, or classifier efficacy.

The full local gate completed with:

```powershell
.\scripts\verify-local-candidate.ps1
```

It performed a clean `clean testDebugUnitTest assembleDebug assembleDebugAndroidTest lint`
build (185 JVM tests; `BUILD SUCCESSFUL`), local Maven publication for all four
libraries, a separate minified consumer build, and inspection of 24 artifacts.
The inspector verified the unchanged model digest and the complete five-file
notice inventory in all applicable AAR/source archives. Link checking passed
for 28 files and 72 local links; `git diff --check` passed.

On the newly created isolated API 36 x86_64 emulator with front and back cameras
enabled as emulated hardware, these suites passed:

```powershell
.\scripts\run-synthetic-instrumentation.ps1 -Serial emulator-5554
.\scripts\run-synthetic-instrumentation.ps1 -Serial emulator-5554 -ConsumerOnly
```

The first run passed 5 video-runtime tests, 2 model-runtime tests, and 5 sample
flow tests. The second passed the minified consumer's 1 packaged-runtime test.
The sample runner's prior-absent-package reset was made conditional so a fresh
emulator is supported. The candidate manifest at
`build/private-candidate/manifest.json` records 12 SDK/sample tests and 1
consumer test. These tests load the unchanged bundled model; they do not run the
ignored newly converted candidate files. No physical device or media corpus was
used. Broader device/API coverage, efficacy, independent validation, and external
redistribution clearance remain open.

## Model attribution packaging verification — 2026-10-05

Only attribution documentation and the packaged `third_party/NOTICE.md` changed.
The retained license files, model bytes, Android code, and build configuration
are unchanged. A targeted release-library packaging build verified notice
retention; the prior runtime/device test evidence was not rerun or replaced.

The installed Gradle 8.13 toolchain was invoked through the existing helper:

```powershell
. .\scripts\local-toolchain.ps1
$taskPackagingArgs = @(':safety-core:assembleRelease', ':safety-core:sourceReleaseJar', ':safety-opennsfw2:assembleRelease', ':safety-opennsfw2:sourceReleaseJar', ':capture-camerax:assembleRelease', ':capture-camerax:sourceReleaseJar', ':capture-video:assembleRelease', ':capture-video:sourceReleaseJar')
Invoke-LocalGradle -Arguments $taskPackagingArgs -Log 'build-logs/model-attribution-packaging.log'
```

The helper passes `--offline --no-daemon --console plain`. Result: **BUILD
SUCCESSFUL**, 126 executed tasks. An archive inspection verified the updated
notice, repository license, and both upstream license files against their source
bytes in all four AAR `classes.jar` files and all four release source JARs:
eight archives and 32 exact file comparisons. The model-bearing AAR contains the
unchanged 6,128,536-byte asset with SHA-256
`051A21BF697858C1E2537354A99BE09A48D26BBFBA0C35216B340F16DE7528D7`.
The other AARs contain no model. The ignored inspection report is
`build-logs/model-attribution-archive-check.json`.

`python scripts/check-local-links.py` passed for 26 files and 53 local links,
with zero broken links. `git diff --check` passed. The source `private camera/gallery source project`
checkout remained clean at `source revision omitted`. License
wording was checked against pinned public upstream revisions in PROVENANCE;
exact conversion provenance and external redistribution clearance remain open.

## Final current-artifact validation - 2026-10-02

The latest clean artifact build at `0ad514d` passed 185 JVM tests, debug and
test-APK assembly, zero-error lint, local Maven/minified builds, notices/model/
permissions/link/text/diff inspection. Source repairs after `6d37cb8` affect
synthetic-test reset and transient ADB installation only; production SDK
implementation is unchanged. The repaired focused source Android suite passed
11 tests. All 16 current Maven files match the earlier clean implementation
candidate byte for byte.

After the existing owned-emulator tunnel was restored, the exact current
minified consumer and test APKs passed `PackagedRuntimeTest` (one test) at
`2026-10-02T00:36:22.2743980Z`. APK hashes stayed unchanged during execution:

- Consumer SHA-256: `81afc6caba2b9511522dd73cb0d7846e4cf60c33d93de1c8fa9ec3e09147bc1c`.
- Test APK SHA-256: `05891663224e0711d7d9f3e67d1630f26c14a9a3f00b5647def41cb3871d33a0`.

The ignored candidate's `current-consumer-validation.json` and matching log
bind this post-build check to the exact current variant. Manifest generation
records `post_build_android_validation` separately from the preserved full
gate evidence and from the latest clean non-emulator build. This closes the
current-variant gap without another build or whole-suite run. No accuracy,
physical/API-range, universal-hardware or legal/publication claim follows.

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

## Final local candidate continuation — 2026-10-01

Clean commit `0ad514d` passed `scripts/verify-local-candidate.ps1` without an
emulator serial: 185 JVM tests, clean debug and Android-test APK compilation,
zero lint errors, four local Maven release publications, the separate minified
consumer build, 24 inspected artifacts, exact model/notices inspection, APK
permission inspection, `git diff --check`, and offline static-link inspection
(26 files and 38 links; zero broken). The generated manifest records a clean
tracked worktree. Its private text audit scanned 134 current text files and
found zero secret candidates and zero tracked media/signing filenames. It
recorded 122 current and 734 historical wording-review candidates under
redacted paths; these are review prompts, not findings of real-data testing or
historical validation, and their details remain only in the private temporary
audit output.

Before the local ADB transport became unavailable, the supplied isolated
emulator completed the synthetic suite after the sample cleanup repair: five
video-decoder tests, two bundled-runtime tests, four sample permission,
lifecycle, capture, cleanup, viewer and recovery tests, plus a separate
minified consumer runtime test. The runner now self-cleans only its own
generated no-backup state and retries a transient local ADB daemon disconnect
during test-APK installation. The existing localhost tunnel to the owned emulator was subsequently
restored. The exact current consumer APK passed its one synthetic runtime
test, exercising both strategies, calibration serialization and approved
private save/preview/authorized fake-destination export. The result is recorded
below. No physical device, media corpus or external service was used. No new
whole-candidate serial run is claimed; the owner requested avoiding redundant
builds after the implementation and artifacts had been verified.

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

## Focused physical-device packaging and synthetic verification - 2026-10-02

Samsung SM-G781W, Android 13/API 33, ARM64, passed ten focused synthetic tests:
five video, two runtime, two sample private-store/calibration-cache and one
exact minified consumer. Inputs were generated geometry/solid colors and
locally encoded solid YUV, with simulated approvals and fake export endpoints.
Live camera permission/preview/capture was not used. Six freshly installed
owned packages were removed after execution; preexisting apps were preserved.

The original library instrumentation APKs compiled against API 36 but targeted
26, unlike the sample/consumer and read-only host application camera/gallery apps,
which compile and target 36 with minimum 26. The generated library test
manifests confirmed the missing explicit target. The phone rejected the old
test APK with Play Protect's older-Android privacy warning and install
verification failure. Commit `fb95ee0c2c1dcd4c443b282693aaab5d09f7aee0` sets
`android.testOptions.targetSdk = 36` in the two affected libraries and adds an
effective APK compile/target/minimum guard to the candidate verifier. Only
their Android-test APKs were rebuilt, offline, in 27 seconds; six tasks ran and
132 were up to date. All seven packaged APKs, including unsigned release
consumer, inspect as compile 36 / target 36 / minimum 26. Both merged test
manifests explicitly target 36. Normal installation then succeeded without
selecting Install anyway, disabling verification or changing security/network
settings. The phone OS remains Android 13.

The initial physical video attempt failed three of five methods during
synthetic encoder configuration. Qualcomm's AVC encoder advertises minimum
width/height 128 and reports 64x64 unsupported. Test-only commit
`65d2475b409e7a1feb24814674da397ed68cd71e` chooses an encoder advertising a
complete conservative 320x240 format, aligns capture metadata/buffer geometry,
and asserts decoded dimensions. Its final single-APK offline incremental build
passed in 25 seconds with four executed tasks and 105 up to date. All five
video tests then passed on the device; independent two runtime/two sample/one
consumer checks passed without redundant reruns. No production capture,
classifier, threshold, preprocessing, model asset or SDK minimum changed.

Original complete-candidate evidence and the two replaced test APKs were
preserved privately before rebuilding. Current artifacts have mixed build
provenance: only the video test APK comes from `65d2475`, the runtime test APK
from `fb95ee0`, and the other 22 candidate artifacts retain the `0ad514d`
baseline. All sixteen Maven artifacts remain byte-identical. Current manifest
entries record each build source and focused physical proof separately from
the earlier full emulator/JVM/lint gates. No new full candidate, full JVM/lint
or emulator rerun is claimed for this follow-up.

Live first-run calibration and camera controls/capture/lifecycle still require
the benign blank-surface confirmation described in DEVICE_MATRIX. One device's
synthetic checks do not close physical-camera, API-range/OEM, efficacy, rights
or external-release gates. No push, publication or release action occurred.

## Authorized Samsung live-camera flow - 2026-10-02

Following explicit operator approval for office floor/ceiling views, the same
Samsung SM-G781W/API 33 passed one opt-in hardware flow in **51.774 seconds**.
Only the sample Android-test APK was rebuilt; the existing API36 sample APK was
reused unchanged. Test build source `9663eb10482aed36340d0c9866b5209dd8324b49`
passed the final offline incremental APK build in 26 seconds (four executed,
123 up-to-date tasks). The previous ten synthetic phone tests and full
JVM/emulator/lint/Maven/consumer builds were not repeated. No production SDK
defect was revealed or production camera/classifier code changed.

Fresh-install permission gating, required front/back photo/video calibration,
probe discard and encrypted ready-profile reuse passed. SDK discovery returned
four camera entries; selected default rear/front graphs actually captured
3024x4032 and 2448x3264 photos. Derived rear Low captured 1500x2000 (3MP).
The High probe matched Normal's dimensions and remained hidden by eligibility
rules. Both default video probes measured 638x1280 and completed their runtime
mechanics gate with proven cleanup. Rear photo/video zoom 1/2/5x verified;
0.6x measured unsupported on the selected graph and remained hidden.

Rear flash Auto/On/Off and an actual On-flash photo, product zoom cycling,
front selection/selfie-1x/flash availability, and owned-preview AF/AE tap
submission passed. Back video pause/resume/back protection, front/back actual
review/private save, activity recreation/landscape capture, explicit recording
cancellation and background cancellation passed without late promotion or
unresolved staging. Five photos and two videos were approved privately and
deleted by the test; approved photo viewing opened/closed with owned bitmap
cleanup. No image/clip bytes were copied off-device. Two freshly installed
owned packages were removed, their app-private calibration/media data erased,
and both used camera clients were closed. Preexisting apps/media and device
security/network/OS settings were unchanged.

The test is opt-in through `liveCameraConsent=office-floor-ceiling`, limited to
the selected model and intended for an explicitly consenting operator. Normal
synthetic/CI runs skip it before launching a camera. Do not supply that opt-in
without corresponding authorization for the actual device and views.

This is a bounded Default-Fixed14 live hardware mechanics result. Adaptive14
remains separately synthetic-tested. Physical sensor rotation, focus sharpness,
additional camera-ID-specific selectors, actual public export, long-run thermal
or latency distributions, broad API/OEM coverage and classifier efficacy remain
unverified. The observed Allow results on these approved views do not establish
content-detection accuracy or external release readiness.
