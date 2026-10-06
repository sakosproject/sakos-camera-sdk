# SakOS Camera SDK

**Experimental Android candidate for managed photo and sampled-video review.**
This is not a released SDK; there are no remote installation coordinates.

[Project site](https://sakosproject.org/) ·
[GitHub repository](https://github.com/sakosproject/sakos-camera-sdk) (currently private) ·
[Apache-2.0 license](LICENSE)

The four libraries and functional sample exercise local workflows independently
of host application accounts, entitlements, signing or providers. The repository remains
private while redistribution rights for the bundled model are unresolved.

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

Automated tests use generated benign geometric/solid-color inputs, mocks, simulated
scores and an isolated emulator scene. A separate opt-in live camera mechanics
flow passed on one Samsung Galaxy S20 FE (SM-G781W, Android 13/API 33), using
operator-approved views of office floors and ceilings. That single-device
capture/control/storage result does not test classifier accuracy or establish
broad hardware compatibility. Historical characterization records are dated
and do not establish current runtime parity.

Fixed14 remains the default and short-circuits on policy Block. Optional Adaptive14
executes the source contextual, portrait/sentinel/refinement, targeted escalation
and fallback stages with its own configuration/approval identity.
Sampled video review does not inspect every frame. The preserved temporal policy
can allow an isolated, uncorroborated, non-extreme final block under its documented
conditions. A modified host can bypass a library's managed path.

## Model, notices and remaining gates

The bundled model's recorded lineage is [OpenNSFW2](https://github.com/bhky/opennsfw2)
by Bosco Yung (MIT), based on [Yahoo Open NSFW](https://github.com/yahoo/open_nsfw)
by Yahoo Inc. (BSD-2-Clause). It was inherited unchanged from the SakOS prebuilt
asset through camera/gallery application. Credits and full license texts are retained in
[third-party notices](third_party/NOTICE.md) and packaged with the libraries.

The exact asset identity and import authorization are retained in
[PROVENANCE](docs/PROVENANCE.md). Owner import authorization does not clear the
full conversion/weight provenance or external redistribution rights. Project
material uses [Apache-2.0](LICENSE); upstream MIT/BSD notices remain under
`third_party` and inside candidate library artifacts.

A separate conversion from the publicly pinned OpenNSFW2 weights now has a
locked recipe and repeatable candidate hashes. The dynamic-range candidate
matched the inherited model on eight generated tensors but exceeded the
predeclared Keras parity limit on black input, so it was not selected. The
bundled TFLite bytes remain unchanged; complete source, license, and result
records are in the [conversion plan](docs/MODEL_CONVERSION_PLAN.md) and its
[recorded candidate manifest](docs/model-conversion/CONVERSION_MANIFEST.json).

Experimental SDK distribution requires confirmed redistribution rights/notices,
a versioned verified artifact set, accurate limitations and an approved release
target. Broader camera/API coverage, efficacy, source parity and independent
validation remain disclosed follow-up work; they do not block the helper site
or automatically block an experimental release. No accuracy, broad parity,
certification, broad device coverage or verified private reporting intake is claimed.
The [customer-facing project site](https://sakosproject.org/) is a dependency-free
status helper with no SDK or model downloads. Its reciprocal link points to the
[GitHub repository](https://github.com/sakosproject/sakos-camera-sdk), which is
currently private. The site README records the Cloudflare Pages deployment and
active custom-domain status. Site publication is separate from SDK/model
distribution.
