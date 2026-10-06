# Provenance and redistribution record

Status: the historical imported model has been replaced by a reproducible float32 conversion. The selected model's source attribution and published license conditions are documented as satisfied with the required notices. The historical model bytes are absent from this candidate's reachable history; only hash-based provenance remains.

## Historical imported model — superseded

An earlier SDK revision contained a TFLite asset at `safety-opennsfw2/src/main/assets/model/sakos_nudity_model.tflite`. A record from a prior private camera/gallery application project attributed it to OpenNSFW2 and Yahoo Open NSFW, but did not identify the exact input weights or export recipe.

| Property | Historical record |
| --- | --- |
| Git blob | `1b10d5c3378aa7c837918c5051f328177d329f2e` |
| Bytes | 6,128,536 |
| SHA-256 | `051a21bf697858c1e2537354a99be09a48d26bbfba0c35216b340f16de7528d7` |
| Reported model ID | `opennsfw2_resnet50_v1` |
| Reported lineage | OpenNSFW2 and Yahoo Open NSFW Model V1 |
| Reported tensor contract | float32 `[1,224,224,3]`, BGR mean subtraction `[104,117,123]`; float32 `[1,2]` output |

The exact OpenNSFW2 revision, input-weight digest, complete conversion environment, and executable conversion recipe were not recovered. The source project identifiers, private paths, and source revisions are omitted. The historical binary itself is not present in the current asset or reachable candidate history. Its recorded lineage is historical evidence, not the basis for the selected model below.

## Reproducible selected model — 2026-10-06

The selected asset was independently reproduced from the official OpenNSFW2 `v0.1.0` HDF5 weights release. The upstream release publishes no digest or signature. The acquisition tool checked the pinned HTTPS URL, published size (`24,221,200` bytes), and HDF5 signature, then recorded a local first-acquisition SHA-256 of `14ca261f48bdd88c1eecba96a761bd1579b523adae1b749b0a4ffd8b7ed8babe`. This local pin identifies the acquired file; it is not an upstream signature.

The locked converter environment uses Python 3.12.15, OpenNSFW2 0.15.2, and TensorFlow 2.20.0. Exact packages and hashes are in [`uv.lock`](../tools/model-conversion/uv.lock). The recipe wraps the Keras model at batch size 1 and emits float32 and dynamic-range TFLite candidates using built-in operators only. BGR mean subtraction stays outside the model.

| Candidate | Bytes | SHA-256 | Minimum runtime metadata | Constant types |
| --- | ---: | --- | --- | --- |
| Selected float32 | 23,608,404 | `bea35dc93c86f074ae9a047638773aff9eb84c05e6ead8d785af5c8ddde05518` | `1.6.0` | 108 FLOAT32, 2 INT32 |
| Rejected dynamic-range | 6,135,528 | `80729f14520115e583bc3a102a7083682b183d2378629e71d32cc5102979c2f8` | `2.17.0` | 54 INT8, 54 FLOAT32, 2 INT32 |

The selected model has no custom or Select TF operators and preserves float32 input `[1,224,224,3]` and output `[1,2]`. Two conversions in the locked CPU environment reproduced identical bytes. On eight generated tensor patterns, float32 maximum absolute error against the source Keras model was `4.85e-7`, below the predeclared `1e-4` limit. Dynamic-range maximum error was `0.028303`, above its `0.01` limit, so it was rejected. These synthetic comparisons assess conversion behavior, not classifier accuracy, real-world efficacy, broad parity, or device coverage.

The current SDK asset's SHA-256 is `bea35dc93c86f074ae9a047638773aff9eb84c05e6ead8d785af5c8ddde05518`, matching both selected-candidate and current-asset entries in [`CONVERSION_MANIFEST.json`](model-conversion/CONVERSION_MANIFEST.json). Its source inputs are pinned in [`SOURCE_MANIFEST.json`](model-conversion/SOURCE_MANIFEST.json), and the recipe and measurements are in [`MODEL_CONVERSION_PLAN.md`](MODEL_CONVERSION_PLAN.md).

## License review and notices

For this selected replacement only, the repository's review records the OpenNSFW2 MIT, Yahoo Open NSFW BSD-2-Clause, and TensorFlow Open NSFW BSD-2-Clause conditions as satisfied. The complete license texts are retained in [`third_party/licenses/`](../third_party/licenses/), and [`third_party/NOTICE.md`](../third_party/NOTICE.md), the model library POM, and model-bearing AAR/source JAR carry the required notices. This is the repository's assessment of the published terms, not an independent legal opinion. No redistribution claim for the historical imported binary is needed or made.

## Release controls and limitations

SDK distribution remains gated on an approved immutable version and delivery target, verified versioned artifacts, and accurate limitations. Broad device coverage, classifier efficacy, and independent validation remain disclosed follow-up work. The model conversion does not change preprocessing, thresholds, or runtime policy. No accuracy, safety certification, or broad-compatibility claim follows from the synthetic comparisons.

The project-wide Apache-2.0 license applies to SakOS-owned material and does not replace third-party notices. Any future model, preprocessing, policy, or third-party-material change requires renewed provenance, notice inventory, and artifact verification.