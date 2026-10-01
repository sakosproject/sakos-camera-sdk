# Spatial policy characterization

This is the retained Phase 4A historical record. Later authorized work bundled
the exact model and added a Bitmap runtime. Later expanded work executes both
Fixed14/default and Adaptive14/optional source-derived drivers. Statements below about
exclusion describe Phase 4A only. Current synthetic mechanics checks establish
neither source/runtime parity nor real-world efficacy; see BUILD_NOTES and
MODEL_CARD for current status.

Phase 4A characterizes the user-authorized spatial policy at source commit
`historical source revision omitted`. It is not model, device, camera,
or device parity evidence.

## Imported policy surface

| Source candidate | Source SHA-256 | SDK treatment |
| --- | --- | --- |
| `GateSamplingProfile.kt` | `1c2f16364637f39ae7a598fe1f7b1832181ca985c3022a0147beb62fe82c2181` | Selectively exported unchanged in behavior under the SakOS package |
| `IntegratedOpenNsfw2Strategy.kt` | `a74699f25abe87b1b83d84c1310176e057f6789ec57a145b4d3ce1e4b629b145` | Selectively exported Fixed14/Adaptive14 identifiers and labels |
| `IntegratedStillGatePolicyDefaults.kt` | `5b7dd848b0a24e14a5d6a40721b79bf9a13c805918383ef7e8cbdd3a58851631` | Selectively exported policy loading and fallback values |
| `SakosCompatibleScoreMapping.kt` | `ce6dfa38daa71ff1d52ba5b26d04671fb672e9515b655310e87598cd584156fc` | Preserved score/result mapping with independent result identity |
| `SakosCompatibleMultiCropEvaluation.kt` | `f007ed5c3a7707976f062c8aa99dbcd1d364a33ae98b43422811a486268b1d47` | Selectively exported fixed/adaptive geometry and evidence aggregation |

The policy source working-copy hash was
`d06a495fa9f777321a240516c0dd624033f13fbec1be002d75310bf9f5417583`.
The SDK checkout uses Git-normalized LF line endings; normalized source content
and the SDK policy asset both hash to
`4e347ada472d81713d2629e2a90c9b23f91f644cd0b584eae61838a5c07f50ff`.
No policy value changed.

## Characterized behavior

- Fixed14 retains full/context views, portrait torso/chest crops, overlapping
  half-tiles, early block short-circuit and corroborated crop evidence.
- Adaptive14 retains its three stages: contextual sweep, targeted escalation,
  and broader fallback. Portrait inputs retain side torso/chest sentinels,
  ambiguity crops and lower-lateral refinement.
- Supported ratio families retain tall-phone, 16:9, 4:3 and square selection.
  Unsupported ratios use the conservative fallback grid.
- Evidence uses the source policy values: high-tier threshold 0.75, raw floor
  0.45, elevated detection floor 0.24, supportive floor 0.30, corroborated
  anchor 0.45, extreme floor 0.85, and the recorded adaptive-stage values.

## Deliberate Phase 4A differences

- The SDK package is `org.sakos.camera.safety.opennsfw2`.
- The blocked host application trust/model contract is replaced by
  `OpenNsfw2CheckResult` and `OpenNsfw2ModelContract`. They identify policy
  results only; they do not bundle a model, authorize capture output, or
  establish host trust.
- `LiveSakosNudityRuntime.kt` and the `.tflite` are not included. No Phase 4A
  API executes inference or represents a model result as device validation.

## Regression evidence

The imported/sanitized suite ran 28 deterministic tests: 26 spatial-policy
tests and 2 score-mapping tests. Float score comparisons use an absolute
tolerance of `0.0001f`, appropriate because they compare fixed in-memory Float
fixtures rather than runtime model outputs. The suite covers ratio boundaries,
bounded windows, Fixed14/Adaptive14 order, portrait sentinels, ambiguity pack,
lower-lateral evidence, corroboration, early allow/block conditions and score
mapping.

Real-model equivalence, classifier accuracy, latency and false-accept/false-
reject evidence remain later gates. External model redistribution remains gated
by the provenance review.

## Expanded live driver mechanics - 2026-10-01

The source-derived live driver now calls the retained helpers in their staged
order, skips duplicate bounds and stops at documented Allow/Block exits. Thirteen
deterministic simulated-score tests cover Fixed14 short circuit/full safe sweep,
Adaptive14 landscape/portrait early Allow/Block, sentinel and ambiguous portrait
stages, refinement, targeted escalation/fallback, duplicates and invalid outputs.
Both strategies also execute on generated benign Bitmaps in Android runtime and
minified local consumer checks. These establish control flow/identity mechanics,
not classifier efficacy or numerical source/model parity. Thresholds and BGR
preprocessing remain unchanged; alternate policy identity prevents receipt reuse.
