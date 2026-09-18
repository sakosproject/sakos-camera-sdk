# Extraction manifest

This manifest identifies source-derived SDK material and its permitted scope.
The source project remains read-only and its unrelated working-tree changes
were not included.

| Source item | SDK destination | Status |
| --- | --- | --- |
| `private source path omitted` | `safety-opennsfw2/src/main/assets/model/sakos_nudity_model.tflite` | Cleared by project-owner authorization; see `docs/PROVENANCE.md` |
| Model metadata and runtime design | `safety-opennsfw2` model contract and LiteRT runtime | Adapted within the independent SDK boundary |
| Spatial policy and score mapping | `safety-opennsfw2` | Previously cleared project-owned export |
| Photo and video capture seams | `capture-camerax`, `capture-video` | Previously cleared project-owned export |

The SDK excludes application identity, entitlement integration, signing,
diagnostics, source build configuration, and non-code private material.
