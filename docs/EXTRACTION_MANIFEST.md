# Extraction manifest

This manifest identifies source-derived SDK material and its permitted scope.
The source project remains read-only and its unrelated working-tree changes
were not included.

| Source item | SDK destination | Status |
| --- | --- | --- |
| `private source path omitted` | `safety-opennsfw2/src/main/assets/model/sakos_nudity_model.tflite` | Owner-authorized private local import; external rights review remains open; see `docs/PROVENANCE.md` |
| OpenNSFW2 `v0.1.0` HDF5 weights and TensorFlow `2.20.0` conversion tools | Ignored local `build/model-conversion/` candidates only | Public source pinned and fully attributed; candidate not selected or included in the SDK asset |
| `scripts/fetch-model-weights.py`, `scripts/convert-opennsfw2-model.py`, `scripts/compare-model-conversions.py`, `scripts/inspect-model-metadata.py` | `scripts/` | Reproducible acquisition, export, synthetic comparison, and FlatBuffer inspection tools; no private corpus input |
| Model metadata and runtime design | `safety-opennsfw2` model contract and LiteRT runtime | Adapted within the independent SDK boundary |
| Spatial policy and score mapping | `safety-opennsfw2` | Previously cleared project-owned export |
| Photo and video capture seams | `capture-camerax`, `capture-video` | Previously cleared project-owned export |
| Source live spatial driver | `safety-opennsfw2/OpenNsfw2StrategyDriver.kt` | Pinned code-only source adaptation; exact policy, synthetic mechanical tests |
| Camera discovery, calibration, graph selection, controls and probes | `capture-camerax`, `capture-video`, minimal sample | Current/pinned code-only inventory and SDK differences in TOOLING_PARITY |
| Camera reviewed repositories and gallery client/repository/exporter | `safety-core` library/export contracts; Android adapters | Approval-bound private save, named approved access, explicit authorized save; standalone host interfaces replace app coupling |

The SDK excludes application identity, entitlement integration, signing,
production diagnostics/telemetry, source build configuration, and non-code private
material. Reusable camera measurement diagnostics are included. Exact revisions
and normalized code snapshot hashes are recorded in TOOLING_PARITY.
