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

The source evidence does **not** pin the exact OpenNSFW2 release/commit used for
conversion, input weight digest, converter version/options, or a reproducible
conversion command. The private SakOS repository's original revision was not
recorded in the source documents. Attribution and the inherited bytes are
verified; those conversion details and external redistribution clearance remain
open. The SDK asset was copied unchanged, with no conversion or retraining here.

## Notices

The source record identifies the OpenNSFW2 wrapper lineage as MIT and the
Yahoo Open NSFW model lineage as BSD-2-Clause. The unmodified notice texts are
retained in:

- [OpenNSFW2 MIT license](../third_party/licenses/opennsfw2-MIT.txt)
- [Yahoo Open NSFW BSD-2-Clause license](../third_party/licenses/yahoo-open-nsfw-BSD-2-Clause.txt)

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
