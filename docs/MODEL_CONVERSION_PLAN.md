# Model evidence recovery and reproducible conversion plan

Date: 2026-10-05/06. Status: **float32 conversion selected and bundled; attribution/license conditions satisfied; non-device repository gate passed; device tests omitted by owner request**.

The owner requested searching `private camera/gallery source project` for the original conversion and
distribution evidence, then preparing this plan if either remained incomplete.
The project owner subsequently authorized implementation, replacement with the
reproducible model, and a GitHub feature-branch push. The owner asked to omit
device testing; the non-device repository gate has passed.

## Result of the investigation

Source checkout: `private camera/gallery source project` at `source revision omitted`.
SDK audit baseline: `52969bb` (the prior attribution changes are committed).
The detailed evidence and source links are in [PROVENANCE](PROVENANCE.md).

| Question | Recovered evidence | Remaining gap |
| --- | --- | --- |
| Which file did we inherit? | Original March 17 import, unchanged source/SDK SHA-256, recorded private SakOS prebuilt path | Private SakOS source revision and original `MODEL.md` contents |
| Which converter/settings? | Embedded TensorFlow 2.20.0, API 2, Keras input, dynamic-range quantization, no custom/Select TF ops | Complete environment and executable conversion command |
| Which trained weights? | OpenNSFW2/Yahoo lineage; a later benchmark pins OpenNSFW2 0.15.2 | Original input weight file, hash, and exact OpenNSFW2 version |
| What is the distribution basis? | Public Yahoo BSD, OpenNSFW2 MIT, official weight release lineage, intermediate Marc Dietrichstein BSD notice | Bind the selected input bytes to the documented sources and complete notice inventory |

The history search covered 951 unique relevant document/script blobs across all
locally reachable refs. No conversion recipe or raw weight filename was found.
The source's documented model cache contains only `.gitkeep`.

Initial recommendation: preserve the old model while preparing a reproducible
conversion from official OpenNSFW2 weights. After the float32 conversion passed
the predeclared source tolerance, the owner directed replacing the old model. The
new model has its own documented source chain and does not rewrite the history of
the first imported file.

## Boundaries

- Keep `private camera/gallery source project` read-only. Work in the SDK and an isolated conversion environment.
- Preserve the current asset and its complete versioned configuration as rollback.
- Keep the Android SDK/AGP/Kotlin/LiteRT versions, preprocessing, crop strategies,
  thresholds, and video policy unchanged for this work.
- Conversion means changing the model file format/optimization; no retraining
  or private media corpus is needed to implement the conversion pipeline.
- Do not publish packages, change repository visibility, deploy the site, or
  contact upstream maintainers as part of implementing the local pipeline.
- Add only public source/license metadata and synthetic verification to the repo.
  Keep intermediate binaries, caches, raw logs, and any private validation outside
  tracked source. Broader efficacy remains a disclosed validation backlog.

## Phase 1 — establish the selected weights and license chain

Use the public source below for the new candidate. Do not represent it as the
historical source of the inherited TFLite file. The read-only `private camera/gallery source project`
provenance and history search found no original weights or conversion script;
its later benchmark pin is a candidate source selection only.

Proposed public selection:

- OpenNSFW2 `0.15.2`, Git tag revision
  `19530b8f08aac12479a901fe18763c0392c8bd8c`.
- Official [pretrained weight release](https://github.com/bhky/opennsfw2/releases/tag/v0.1.0):
  `open_nsfw_weights.h5` (reported size 24,221,200 bytes).
- [Pinned downloader](https://github.com/bhky/opennsfw2/blob/19530b8f08aac12479a901fe18763c0392c8bd8c/opennsfw2/_download.py)
  identifies the asset URL. Download only that declared asset; never silently
  accept a user's existing cache or trigger the package's default downloader.
- Package wheel SHA-256 reported by [PyPI](https://pypi.org/project/opennsfw2/0.15.2/):
  `fd9e4a5dd792bf9e702aa87b03a48b38def9992672678a86b8ee5df2c22e515a`.
  This is the package checksum, **not** the trained-weight checksum.

Create `docs/model-conversion/SOURCE_MANIFEST.json` and
`docs/model-conversion/LICENSE_REVIEW.md`. Record repository revisions, release
asset identifier, URL, retrieval date, size, HDF5 signature, and computed weight
SHA-256. The release supplied no asset digest at this audit: record that limit,
pin the acquired bytes before conversion, and reject any future hash mismatch.

Retain complete MIT/BSD texts and document their scope. The new weight lineage
passes through [TensorFlow Open NSFW](https://github.com/mdietrichstein/tensorflow-open_nsfw/blob/ead9f4d1748e8bc80ab14bf0a36f696a5fe4109d/LICENSE),
whose license includes both Yahoo and Marc Dietrichstein copyrights. Plan an
additional retained license file and updated notice/inspection inventory for
that lineage. TensorFlow/tool dependencies belong in the build-tool inventory;
their installation alone does not mean their code is shipped inside the AAR.

Gate: the selected input file is identified and pinned; the source license chain
permits binary redistribution with its stated conditions; the complete notice
inventory is in the repo and artifact metadata. The license review records how
each condition is met.

## Phase 2 — lock the conversion environment

`tools/model-conversion/pyproject.toml`, `uv.lock` with package hashes, and
`tools/model-conversion/README.md` now define the isolated environment.

Start with isolated Python 3.12, TensorFlow `2.20.0` (matching embedded metadata),
and OpenNSFW2 `0.15.2` (the source project's documented benchmark selection).
Use `KERAS_BACKEND=tensorflow`, not the benchmark's JAX backend. These choices
are proposed reproduction inputs, not a claim about the original environment.

Resolve the supported Keras, NumPy, HDF5, FlatBuffers, and other transitive
versions once, check compatibility, and commit their exact versions/hashes before
the first candidate conversion. Record OS, architecture, Python patch version,
CPU/thread settings, wheel hashes, and the lock hash. Confirm packages provide
wheels for the chosen platform; do not change the Android toolchain to make this work.

Gate: the locked environment can be recreated; its package/source identities
match the manifest. Future runs install from the lock with hash verification
and fail on undeclared dependency or input changes.

## Phase 3 — implement conversion and inspection

Conversion implementation files:

- `scripts/fetch-model-weights.py`: fetch/verify the manifest's explicit input.
- `scripts/convert-opennsfw2-model.py`: verify input/package identities, load
  `make_open_nsfw_model(weights_path=<verified file>)`, and convert it.
- `scripts/inspect-model-metadata.py`: inspect tensors, operator versions,
  conversion metadata, optimization and constant types.
- `docs/model-conversion/CONVERSION_MANIFEST.json`: public, reproducible recipe.

Write candidates under ignored `build/model-conversion/`, never directly into
`safety-opennsfw2/src/main/assets/model/`.

Use the official [TensorFlow conversion API](https://developers.google.com/edge/litert/conversion/tensorflow/convert_tf).
Produce a float32 reference first, then the dynamic-range candidate matching the
current optimization class. Both retain the float32 batch-1 input `[1,224,224,3]`
and float32 `[1,2]` SFW/NSFW output contract. Preprocessing remains outside the model:
BGR with subtraction `[104,117,123]`. Do not embed a second preprocessing step.

Set `supported_ops` to `TFLITE_BUILTINS` and disable custom ops. Start with
`from_keras_model`, no optimizations for the reference, and `Optimize.DEFAULT`
for the dynamic-range candidate. Inspect the result to verify it is actually
dynamic-range quantized; the flags alone are not sufficient evidence. No full
integer quantization, representative private dataset, or Select TF fallback is
part of this plan. Fail conversion on unsupported ops and investigate explicitly.

The manifest records the exact CLI and script revision/hash, input weight hash,
package/environment lock hash, conversion options, shapes/types, operator
versions, runtime metadata, size and SHA-256 of each output.

Gate: both outputs have valid finite probability contracts, retain the required
external tensor contract, and are supported by the existing Android LiteRT 1.4.2.
Verify compatibility by running that runtime; converter/runtime version numbers
alone do not establish compatibility.

## Phase 4 — compare and reproduce before selecting a model

`scripts/compare-model-conversions.py` uses deterministic, generated
inputs: solid colors, channel ramps, gradients, and seeded noise over the
SDK's input domain. Use identical prepared tensors for the Keras source model,
float32 reference, dynamic-range candidate, and current bundled TFLite model.
Also cover the SDK's resize/channel/preprocessing path with generated bitmaps.

Record finite/range/sum checks, both class probabilities, maximum/mean numerical
differences, output order, threshold-boundary effects, and latency under recorded
conditions. Declare numerical tolerances before examining the results. Proposed
initial maximum absolute errors against Keras: `1e-4` for float32 and `1e-2`
for dynamic range. These are engineering acceptance proposals, not measured
results; a failure requires investigation, not automatically widened tolerances.

Run the locked conversion twice in separate clean output directories. Prefer
identical SHA-256. If exported metadata creates differences, identify the cause
and compare graph/constants as well as inference; record the reproducibility
limit instead of claiming identical binaries.

Decision:

- If the output reproduces the current SHA-256, document the reproducible recipe
  and source/license basis. No model replacement or digest change is needed.
- If bytes differ, keep it as a separate candidate. Document model-to-model
  score differences and any decision changes; investigate before selecting it.
  Preserve thresholds during this task. Retuning policy is separate work.
- Synthetic comparisons demonstrate conversion mechanics and numerical behavior.
  They do not establish real-world detection efficacy. Do not copy the previous
  model's unmeasured efficacy claims onto the candidate.

Gate: input provenance, license inventory, reproducibility and comparison report
are reviewable; no unexplained numerical/contract failure remains. Model adoption
requires a deliberate selection after this evidence is available.

## Phase 5 — integrate the selected candidate and verify artifacts

The owner selected the float32 candidate after it passed the predeclared source
tolerance. The asset, size/digest, and digest-derived model version were updated
together:

- `safety-opennsfw2/src/main/assets/model/sakos_nudity_model.tflite`
- `safety-opennsfw2/src/main/java/org/sakos/camera/safety/opennsfw2/OpenNsfw2ModelPreflight.kt`
- `safety-opennsfw2/src/test/java/org/sakos/camera/safety/opennsfw2/OpenNsfw2ModelPreflightTest.kt`
- `scripts/inspect-local-candidate.py`: model checksum/size, configuration version,
  and all notice comparisons for the expanded license inventory.
- `third_party/NOTICE.md`, applicable `third_party/licenses/` files,
  `docs/PROVENANCE.md`, `docs/MODEL_CARD.md`, `docs/EXTRACTION_MANIFEST.md`,
  and current release/validation documentation.

Keep `docs/BASELINE.md` and historical BUILD_NOTES as historical records. Add a
new source-bound verification entry; do not rewrite earlier evidence as though
it tested the new bytes. Preserve policy/preprocessing identifiers unless their
implementation changes. Verify approvals from the old model configuration are
not accepted under the new configuration.

Run the existing non-device verifier:

```powershell
.\scripts\verify-local-candidate.ps1
```

It performs clean JVM/debug/test-APK/lint builds, local Maven publication,
minified-consumer builds and artifact/notice/model inspections. No device or
emulator run is required for this replacement, as requested. Regenerate artifact
checksums and the source-bound manifest after the verifier using
`inspect-local-candidate.py`.

Adoption gate: the selected float32 model passes the predeclared `1e-4` source
tolerance, matches the recorded input/output contract, and is bound to its new
digest-derived configuration identity. The dynamic-range alternative remains
rejected because it exceeds its predeclared `1e-2` tolerance. The published
license conditions are recorded as met in `LICENSE_REVIEW.md`; all applicable
full notices are in Maven metadata and the model AAR/source JAR.

## Rollback and release boundary

Keep the original asset available from the audit baseline and retain its digest
`051A21BF697858C1E2537354A99BE09A48D26BBFBA0C35216B340F16DE7528D7`.
Rollback restores the asset, size, digest-derived version, affected tests,
inspection constants, and current documentation as one coherent change. Rebuild
local artifacts after rollback; do not mix either model with the other's manifest.

The selected model's license conditions and notice requirements are satisfied in
the repository artifacts. External delivery still follows the existing
[release checklist](RELEASE_CHECKLIST.md) for an approved version and target,
verified artifacts, and accurate limitations. Broader efficacy/device coverage
remain disclosed follow-up work for an experimental release.

## Execution ledger

| Phase | Status | Evidence |
| --- | --- | --- |
| Investigation and plan | Completed locally | Read-only source/history/cache and binary audit; 951 historical blobs; public release evidence |
| 1. Selected weights and licenses | Completed | SHA-256 pinned; OpenNSFW2, Yahoo and Marc Dietrichstein terms reviewed; binary redistribution conditions and complete notices satisfied |
| 2. Environment lock | Completed | Python 3.12.15, TensorFlow 2.20.0, OpenNSFW2 0.15.2; package hashes in `uv.lock` |
| 3. Conversion and inspection | Completed | Float32 and dynamic-range candidates generated twice; both use built-in ops and preserve tensor interface |
| 4. Comparison/reproduction | Completed; float32 selected | Byte-identical repeat runs; float32 passes `1e-4`; dynamic-range exceeds `1e-2` on generated black input; no threshold changes |
| 5. Adoption and packaging | Passed | Float32 asset and digest-derived identity installed; full notices and Maven license entries verified in packages; no device testing per owner request |
