# Model attribution and redistribution review

## Selected model

The SDK now bundles `opennsfw2_resnet50_v1` generated from the pinned
OpenNSFW2 `v0.1.0` HDF5 weights. OpenNSFW2's release notes identify these as
the Yahoo Open NSFW model's original pretrained weights migrated through
TensorFlow. Yahoo's repository contains the corresponding Caffe model file next
to its BSD-2-Clause license. The TensorFlow Open NSFW port carries BSD-2-Clause
terms for Yahoo Inc. and Marc Dietrichstein. The OpenNSFW2 implementation is
MIT-licensed.

The input bytes are pinned in [`SOURCE_MANIFEST.json`](SOURCE_MANIFEST.json).
The upstream release does not publish a digest; the recorded local SHA-256
identifies the exact acquired file and supports repeatable builds. It is not
represented as an upstream signature.

## Redistribution determination

The reviewed upstream terms permit source and binary redistribution subject to
retaining the relevant copyright notices, conditions, and disclaimers. This
SDK complies with those conditions for the bundled model:

- The full OpenNSFW2 MIT license, Yahoo BSD-2-Clause license, and combined
  TensorFlow Open NSFW BSD-2-Clause license are retained under `third_party/licenses/`.
- `third_party/NOTICE.md` identifies the model lineage and copyright holders.
- The model library's Maven POM lists Apache-2.0 for SDK code and all three
  applicable upstream license sources for the bundled model.
- Release AARs and source JARs include the complete notice and license texts;
  the artifact inspector verifies them byte for byte.

**Disposition:** attribution and the stated license conditions for redistributing
this model are satisfied in the repository and its model-bearing AAR. A release
still needs an approved version, delivery target, verified artifacts, and accurate
limitations; those release controls do not leave model redistribution rights open.

The Python/TensorFlow conversion environment is only a build tool. Its packages
are locked in `tools/model-conversion/uv.lock` and are not included in Android
runtime artifacts.
