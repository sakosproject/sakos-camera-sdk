# Provenance and redistribution record

Status: **model import authorized by the project owner** as of 2026-09-17.

The exact imported bytes may be used for the authorized private local candidate
checks. Owner import authorization and checksum identity do not clear the
complete upstream weight/conversion provenance or legal redistribution rights.
Those remain explicit owner/legal gates before external delivery.

## Imported model

The project owner explicitly authorized importing the exact asset from the
local camera/gallery application source project. The source project's
`docs/UPSTREAM_PROVENANCE.md` records this origin:

| Item | Record |
| --- | --- |
| Original prebuilt source | `historical model source path omitted` |
| Immediate source | `private source path omitted` |
| SDK path | `safety-opennsfw2/src/main/assets/model/sakos_nudity_model.tflite` |
| Model ID | `opennsfw2_resnet50_v1` |
| Size | 6,128,536 bytes |
| SHA-256 | `051A21BF697858C1E2537354A99BE09A48D26BBFBA0C35216B340F16DE7528D7` |
| Lineage | OpenNSFW2 and Yahoo Open NSFW Model V1 |
| Input / output | float32 `[1, 224, 224, 3]` BGR mean subtraction `[104, 117, 123]`; float32 `[1, 2]` SFW/NSFW probabilities |

The SDK’s bundled-asset integrity check enforces the recorded size and
checksum before LiteRT opens the asset. The source checkout remains read-only;
its unrelated working-tree changes were not included.

## Origin evidence verified on 2026-10-05

The recorded chain is **Yahoo Open NSFW → OpenNSFW2 → SakOS prebuilt TFLite
asset → camera/gallery application → this SDK**. The first two links describe the
documented upstream lineage; the exact conversion steps are not independently
reproduced by this SDK.

The read-only `private camera/gallery source project` checkout was inspected at
`source revision omitted`. Its evidence is:

- `docs/UPSTREAM_PROVENANCE.md`, the reused-model row and license notes: records
  the private SakOS prebuilt path, SHA-256, and OpenNSFW2/Yahoo lineage reported
  by `historical model source path omitted`.
- `docs/AI_MODEL_INTEGRATION.md`, the observed source and model-selection sections:
  records the imported asset and source contract.
- `private source path omitted`:
  records `opennsfw2_resnet50_v1`, lineage, tensor/preprocessing contract, and
  MIT/BSD-2-Clause license notes.
- Asset history: the original import is in commit
  `source revision omitted` (2026-03-17,
  `Checkpoint: Skeleton work and Sakos reuse`). Both current source and SDK
  assets are 6,128,536 bytes and match the SHA-256 above.

The public upstream sources confirm that [OpenNSFW2](https://github.com/bhky/opennsfw2)
implements [Yahoo Open NSFW](https://github.com/yahoo/open_nsfw) in Keras.
The original Yahoo model is a thin ResNet-50 classifier trained by Jay Mahadeokar
in collaboration with Sachin Farfade, Amar Ramesh Kamat, Armin Kappeler, and others.
The copyright holders and retained licenses are:

| Upstream | Copyright | License | License source checked on 2026-10-05 |
| --- | --- | --- | --- |
| OpenNSFW2 (`bhky/opennsfw2`) | Copyright (c) 2021 Bosco Yung | MIT | [LICENSE at `660e981`](https://github.com/bhky/opennsfw2/blob/660e981f60f48505c03d2c041e0263767b6811c2/LICENSE) |
| Yahoo Open NSFW (`yahoo/open_nsfw`) | Copyright 2016, Yahoo Inc. | BSD-2-Clause | [LICENSE.md at `a4e1393`](https://github.com/yahoo/open_nsfw/blob/a4e13931465f4380742545932657eeea0a10aa48/LICENSE.md) |

The retained license wording matches those upstream texts after normalizing
whitespace; the existing local license files were preserved. These checked
upstream revisions identify the attribution audit sources, not the versions used
to generate the bundled TFLite file.

The source documents do **not** pin the exact OpenNSFW2 release/commit used for
conversion, input weight digest, complete conversion environment, or a reproducible
conversion command. The private SakOS repository's original revision was not
recorded in the source documents. The deeper audit below recovered converter
version and high-level options from the asset itself. The SDK asset was copied
unchanged, with no conversion or retraining here.

## Deeper source and binary audit — 2026-10-05

The read-only search covered current source documentation/scripts, relevant file
history, all locally reachable Git objects, and the documented model cache.
The history content search examined 951 unique document/script blobs under
`docs/`, `scripts/`, and `README.md`. It found no TFLite conversion API call or
raw `.h5`/`.caffemodel` weight filename. The only converter wording was unrelated
PowerShell `BitConverter` usage. `historical model source path omitted` is referenced
but its contents are not vendored in the source project.

A targeted search of ignored `build/` and `build-logs/` material also found no
conversion script, raw weight file, or reference to an original weight download.
Private media/benchmark report contents and production signing files were excluded.

`nudity-test-pipeline/model-cache/` contains only `.gitkeep`; the benchmark Python
environment directories named by `scripts/nudity-test-pipeline.ps1` are absent.
No original raw weights, weight checksum, or model-export script was found in
the inspected checkout/history. This does not establish that they are absent
from the original private SakOS repository, other machines, or backups.

The later benchmark pins `opennsfw2==0.15.2` in
`private source path omitted`. Its runner sets
`OPENNSFW2_HOME`, selects a JAX backend, and calls `make_open_nsfw_model()` to
download default weights. The benchmark was added at `d811991` on 2026-04-08,
after the model import on 2026-03-17. It is a useful candidate source selection,
but does not prove the bundled model's original package version or weights.

A read-only FlatBuffer inspection of the unchanged model recovered:

| Embedded field | Value |
| --- | --- |
| Description | `MLIR Converted.` |
| TensorFlow converter version | `2.20.0` |
| Converter API version | `2` |
| Original model format | `KERAS_MODEL` (`2`) |
| Optimization | `PTQ_DYNAMIC_RANGE` (`1002`) |
| Custom / Select TF / forced Select TF ops | All `false` |
| Minimum runtime metadata | `2.17.0` |
| External tensor contract | float32 `[1,224,224,3]` input; float32 `[1,2]` output |
| Constant tensor types | 54 INT8, 54 FLOAT32, 2 INT32 |

Field/enumeration interpretation was checked against the TensorFlow 2.20
[model schema](https://github.com/tensorflow/tensorflow/blob/v2.20.0/tensorflow/compiler/mlir/lite/schema/schema.fbs)
and [conversion metadata schema](https://github.com/tensorflow/tensorflow/blob/v2.20.0/tensorflow/compiler/mlir/lite/schema/conversion_metadata.fbs).
These are embedded self-reported conversion facts, not a recovered command or
proof of the exact input weights. Float32 input/output does not imply float32
weight storage. The runtime metadata number is not an Android Maven version;
it does not invalidate the SDK's existing LiteRT 1.4.2 runtime test evidence.

Public upstream evidence supplies a prospective weight source:

- OpenNSFW2's [v0.1.0 weight release](https://github.com/bhky/opennsfw2/releases/tag/v0.1.0)
  identifies `open_nsfw_weights.h5` as a conversion of Yahoo's pretrained weights
  through the earlier TensorFlow implementation. The release API reports
  24,221,200 bytes and no published asset digest at this audit.
- OpenNSFW2 [v0.15.2 download code](https://github.com/bhky/opennsfw2/blob/19530b8f08aac12479a901fe18763c0392c8bd8c/opennsfw2/_download.py)
  points to that release asset. Its [MIT license](https://github.com/bhky/opennsfw2/blob/19530b8f08aac12479a901fe18763c0392c8bd8c/LICENSE)
  credits Bosco Yung.
- Yahoo's repository at `a4e13931465f4380742545932657eeea0a10aa48` distributes
  `nsfw_model/resnet_50_1by2_nsfw.caffemodel` alongside its existing root
  BSD-2-Clause license. The inspected tree contains no separate per-model license.
- The intermediate [TensorFlow Open NSFW license](https://github.com/mdietrichstein/tensorflow-open_nsfw/blob/ead9f4d1748e8bc80ab14bf0a36f696a5fe4109d/LICENSE)
  retains Yahoo's terms and an additional BSD-2-Clause notice for Marc Dietrichstein
  (2017). A new conversion using this weight lineage must account for that notice
  in its license inventory.

This supports a documented attribution basis for a new conversion. It does not
identify the exact weights behind the inherited binary, and attribution alone
does not settle redistribution rights. The full combined TensorFlow Open NSFW
license is retained at
[`third_party/licenses/tensorflow-open-nsfw-LICENSE.txt`](../third_party/licenses/tensorflow-open-nsfw-LICENSE.txt).

## Reproducible candidate conversion — 2026-10-05/06

The missing original input and export recipe were not recovered from the
read-only source project. A separate candidate was therefore generated from
the official OpenNSFW2 `v0.1.0` weights release; this new record does not
rewrite the inherited model's history.

The acquisition tool fetched `open_nsfw_weights.h5` from the pinned HTTPS
release URL on `2026-10-06T00:23:23Z`. It verified the published size
(`24,221,200` bytes) and HDF5 signature, then recorded SHA-256
`14ca261f48bdd88c1eecba96a761bd1579b523adae1b749b0a4ffd8b7ed8babe` before
conversion. The publisher did not provide an asset digest: this SHA-256 is a
local first-acquisition content pin, not an upstream signature.

The isolated lock uses Python 3.12.15, OpenNSFW2 0.15.2, and TensorFlow 2.20.0;
the exact package hashes and transitive versions are in
[`tools/model-conversion/uv.lock`](../tools/model-conversion/uv.lock). The
converter creates a fixed batch-1 Keras wrapper and exports float32 and
dynamic-range TFLite files with built-in operators only. External input/output
remain float32 `[1,224,224,3]` and `[1,2]`; BGR mean subtraction stays outside
the model. Both conversions were run twice on the same locked Windows AMD64
CPU environment and reproduced identical candidate SHA-256 values:

| Candidate | Bytes | SHA-256 | Embedded minimum runtime | Constant types |
| --- | ---: | --- | --- | --- |
| Float32 | 23,608,404 | `bea35dc93c86f074ae9a047638773aff9eb84c05e6ead8d785af5c8ddde05518` | `1.6.0` | 108 FLOAT32, 2 INT32 |
| Dynamic range | 6,135,528 | `80729f14520115e583bc3a102a7083682b183d2378629e71d32cc5102979c2f8` | `2.17.0` | 54 INT8, 54 FLOAT32, 2 INT32 |

The dynamic-range candidate has no custom or Select TF operators and retains the
inherited model's interface and dynamic-range constant types. On eight
deterministically generated tensor patterns, the float32 candidate's maximum
absolute error against the source Keras model was `4.85e-7` (within the
predeclared `1e-4` limit). The dynamic-range candidate's maximum was `0.028303`
on generated black pixels, above the predeclared `0.01` limit; its mean was
`0.004849`. The dynamic-range candidate matched the inherited TFLite outputs
exactly for those eight patterns and changed no decisions at the tested `0.45`,
`0.75`, or `0.85` thresholds. This supports conversion repeatability and
limited synthetic behavioral comparison, not broad parity or accuracy.

**Decision:** keep the current bundled asset unchanged at SHA-256
`051A21BF697858C1E2537354A99BE09A48D26BBFBA0C35216B340F16DE7528D7`. The
dynamic-range candidate failed the predeclared Keras tolerance and is not
selected for SDK integration. The float32 candidate is only a diagnostic
reference; it is not selected either. No thresholds, preprocessing, or runtime
policy changed. The full source selection, scripts, and sanitized measurement
record are in the [conversion plan](MODEL_CONVERSION_PLAN.md),
[source manifest](model-conversion/SOURCE_MANIFEST.json),
[license record](model-conversion/LICENSE_REVIEW.md), and
[conversion manifest](model-conversion/CONVERSION_MANIFEST.json).

## Notices

The source record identifies the OpenNSFW2 wrapper lineage as MIT and the
Yahoo Open NSFW model lineage as BSD-2-Clause. The candidate's intermediate
TensorFlow Open NSFW license also retains Yahoo and Marc Dietrichstein BSD
notices. The complete unmodified notice texts are retained in:

- [OpenNSFW2 MIT license](../third_party/licenses/opennsfw2-MIT.txt)
- [Yahoo Open NSFW BSD-2-Clause license](../third_party/licenses/yahoo-open-nsfw-BSD-2-Clause.txt)
- [TensorFlow Open NSFW combined license](../third_party/licenses/tensorflow-open-nsfw-LICENSE.txt)

[Third-party notices](../third_party/NOTICE.md) name both upstream projects and
copyright holders. The existing `prepareArtifactNotices` task in
`build.gradle.kts` includes the notice and complete license files in each
library AAR's `classes.jar` and source JAR at `META-INF/sakos/<module>/`.
Model-bearing distributions must preserve applicable notices and license texts
in their accompanying materials.

The SakOS wrapper and gate repositories are recorded by the source project as
Apache-2.0. The project-wide Apache-2.0 license applies only to SakOS-owned
material and does not replace third-party notices.

## Change control

A model replacement, checksum change, preprocessing change, policy change, or
additional third-party material requires an update to this record,
`docs/MODEL_CARD.md`, `docs/EXTRACTION_MANIFEST.md`, and the notice inventory.
Model-bearing distribution still requires confirmed permission to redistribute
the exact weights/conversion and applicable code/dependencies, retained notices,
versioned verified artifacts and an approved release target. Broad device testing,
efficacy and independent validation are disclosed follow-up work for an experimental
release. They are not prerequisites for publishing the status-only helper site.
