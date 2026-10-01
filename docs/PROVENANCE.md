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
| Source path | `historical model source path omitted` |
| SDK path | `safety-opennsfw2/src/main/assets/model/sakos_nudity_model.tflite` |
| Model ID | `opennsfw2_resnet50_v1` |
| Size | 6,128,536 bytes |
| SHA-256 | `051A21BF697858C1E2537354A99BE09A48D26BBFBA0C35216B340F16DE7528D7` |
| Lineage | OpenNSFW2 and Yahoo Open NSFW Model V1 |
| Input / output | float32 `[1, 224, 224, 3]` BGR mean subtraction `[104, 117, 123]`; float32 `[1, 2]` SFW/NSFW probabilities |

The SDK’s bundled-asset integrity check enforces the recorded size and
checksum before LiteRT opens the asset. The source checkout remains read-only;
its unrelated working-tree changes were not included.

## Notices

The source record identifies the OpenNSFW2 wrapper lineage as MIT and the
Yahoo Open NSFW model lineage as BSD-2-Clause. The unmodified notice texts are
retained in:

- `third_party/licenses/opennsfw2-MIT.txt`
- `third_party/licenses/yahoo-open-nsfw-BSD-2-Clause.txt`

The SakOS wrapper and gate repositories are recorded by the source project as
Apache-2.0. The project-wide Apache-2.0 license applies only to SakOS-owned
material and does not replace third-party notices.

## Change control

A model replacement, checksum change, preprocessing change, policy change, or
additional third-party material requires an update to this record,
`docs/MODEL_CARD.md`, `docs/EXTRACTION_MANIFEST.md`, and the notice inventory.
No model-bearing artifact may be published until the remaining release gates,
including device verification, notices, security intake, and release approval,
are complete.
