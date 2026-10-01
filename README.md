# SakOS Camera SDK

Private local Android SDK candidate for managed photo and sampled video review.
Four libraries and a functional sample run independently of host application accounts,
entitlements, signing or providers. No remote installation coordinates or public
release are available.

| Module | Implemented boundary |
| --- | --- |
| safety-core | Capture/configuration identities, fail-closed outcomes, matching Allow approvals, private reviewed library and authorized export interfaces. |
| safety-opennsfw2 | Exact bundled OpenNSFW2-lineage model, integrity/tensor preflight, BGR preprocessing and Fixed14/default or Adaptive14/optional Bitmap runtime. |
| capture-camerax | In-memory photo review, advertised capability discovery, source-derived calibration/profile/selector tooling, private preview and optional authorized MediaStore saving. |
| capture-video | No-backup staging, CameraX start/finalization, Android timestamp decoding, bounded temporal review, real graph probes and approval-bound private promotion. |

The sample requests camera permission, completes first-run usable-graph calibration,
provides back/front, quality, verified zoom, focus and flash controls, captures photos
in memory, records silent video into private staging and displays only approved
sample-owned output. It requests no microphone, storage or network permission.
The approved viewer uses app-private no-backup storage and has no import/export
or gallery scanning flow.

## Local verification

```powershell
.\scripts\verify-local-candidate.ps1 -Serial <isolated-emulator-serial>
```

Without `-Serial`, local compilation, JVM tests, lint, Maven packaging and the
separate minified consumer build still run. An explicitly provided emulator
serial enables synthetic instrumentation. The script never creates an emulator
or changes a VM. Output stays under ignored local build directories.

For packaging/consumer verification alone:

```powershell
.\scripts\verify-local-consumer.ps1
```

Provisional coordinates are `org.sakos.camera:<module>:0.0.0-local` in
`build/local-maven`. They are local review artifacts, not remote availability.
See [build evidence](docs/BUILD_NOTES.md), [integration](docs/INTEGRATION.md),
[camera tooling](docs/CAMERA_TOOLING.md), [reviewed library/save](docs/REVIEWED_LIBRARY.md),
[source tooling map](docs/TOOLING_PARITY.md), [model card](docs/MODEL_CARD.md) and
[release gates](docs/RELEASE_CHECKLIST.md).

## Privacy and limits

Non-approved photos produce no SDK-written image file. Video bytes touch private
temporary disk storage during recording/review. Cancellation, rejection, failure
and abandoned-session recovery request cleanup; a durable cleanup failure blocks
new recording until retry succeeds. Deletion is not forensic erasure.

Current tests use generated benign geometric/solid-color inputs, mocks, simulated
scores and an isolated emulator scene. Bundled-runtime checks verify mechanics,
not accuracy. This statement describes the current suite only. Historical
characterization records are dated and do not establish current runtime parity.
Fixed14 remains the default and short-circuits on policy Block. Optional Adaptive14
executes the source contextual, portrait/sentinel/refinement, targeted escalation
and fallback stages with its own configuration/approval identity.
Sampled video review does not inspect every frame. The preserved temporal policy
can allow an isolated, uncorroborated, non-extreme final block under its documented
conditions. A modified host can bypass a library's managed path.

## Model, notices and remaining gates

The exact asset identity and import authorization are retained in
[PROVENANCE](docs/PROVENANCE.md). Owner import authorization does not clear the
full conversion/weight provenance or external redistribution rights. Project
material uses [Apache-2.0](LICENSE); upstream MIT/BSD notices remain under
`third_party` and inside candidate library artifacts.

Owner/legal clearance, physical-camera/API-range verification, real-world
classifier efficacy, independent-validation status, dependency notice review,
security intake and all external release decisions remain open. No accuracy,
parity, certification, physical-device coverage or public intake is claimed.
The [website](website/README.md) is a local static preview only.
