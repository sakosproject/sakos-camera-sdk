# Extraction manifest

Status: Phase 1 inventory, 2026-09-17. All source paths are relative to the
pinned source commit `historical source revision omitted`.

`Cleared` means the named work may enter this repository under the stated
authorization and provenance condition. `Blocked` means no copy, adaptation,
asset import, or generated derivative may enter this repository until the
provenance record is cleared. `Excluded` means the item is out of SDK scope.

## Cleared independent work

| Item | Target | Status | Verification |
| --- | --- | --- | --- |
| Independently authored Gradle settings, version catalog and module build files at the recorded baseline | repository root and five planned modules | Cleared | Phase 2 dependency/build inspection |
| Independently authored generic contracts, tests and sample scaffolding | `safety-core`, `sample-app` | Cleared | Phase 3+ unit tests and clean build |
| Gradle wrapper generated from an official Gradle 8.13 distribution, with its own verified notices | repository root | Cleared with notice review | Phase 2 wrapper checksum/license record |
| Documentation, sanitized benchmark interface and validation reports | `docs/`, `tools/benchmark/` | Cleared | no corpus, paths, outputs or private metadata |

## Blocked source-derived candidates

| Source file | SHA-256 | Proposed destination/role | Status and required evidence | Planned verification |
| --- | --- | --- | --- | --- |



















| `scripts/nudity-model-benchmark.py` | `1683fa1501f34fe895229d0f377ea7928e9c116dc4f0f86b3426000f5b4a7f2d` | portable benchmark design | Cleared: user-authorized algorithm export; exclude corpus/manifests/outputs | sanitized tool smoke test |

## Source test references

The following tests are user-authorized behavioral references. Extract only
sanitized assertion logic and independently named fixtures; do not copy a
private corpus, result output, device path or environment-specific setup:
`SakosCompatibleScoreMappingTest.kt`
(`9809bba86a97a37ab36aeb5917d7524831016db628ae6b7a7ee7083af95dcd8c`),
`SakosCompatibleMultiCropEvaluationTest.kt`
(`69c2416c1511fa75f88208a232a179f46047aee492969397cd94520f4e541829`),
`RegaAppCaptureGateTest.kt`
(`7965cf46edeb4373ca47a4f0b5a1c4ad9a4be39b2bcceca89113626985f8d136`),
and `VideoTemporalSamplePlannerTest.kt`
(`a7d09e7f2c6676aba9e76054aa7862e67c8a2684d4714019607dbc5a342c5a28`).

Create new test cases from documented requirements and independently designed
fixtures; never import a private corpus, original fixture, device path, or
result file.

## Excluded material

- `CameraActivity.kt` as a whole, camera UI/theme/state, Gallery implementation,
  and all application manifests: extraction must use small independently
  authored components.
- host application entitlement, signature, provider, managed-session and access-gate
  code; Firebase/Crashlytics, Google Services configuration, diagnostics,
  release scripts, reviewer-token logic, signing configuration and keystores.
- Test corpus, manifests, private media, screenshots, build logs, calibration
  data, local paths, exports, caches and generated outputs.
- host application and camera application product branding, artwork and installation claims.
