# Baseline record

This historical record summarizes observations used for the initial SDK extraction. It is not a license grant; current model attribution and notice status are recorded in `PROVENANCE.md` and `model-conversion/LICENSE_REVIEW.md`.

## SDK foundation

- Repository commit before Phase 1: `651b05f5289a0ecaf58829bdf691c6bf06c94724`.
- Phase 1 branch: `codex/phase-1-provenance`.
- The repository contained documentation, an Apache-2.0 license, and ignore
  rules only. It did not contain Android source, a Gradle wrapper, model
  assets, test media, or production configuration.

## Read-only source snapshot

A related Android application source snapshot was reviewed on 2026-09-17. It contained unrelated local changes that were not included in this SDK. Private source-project names, revisions, and paths are omitted. The comparison is behavioral context only and does not establish exact source parity.

## Retained Android baseline for Phase 2

| Setting | Observed value | Observed source |
| --- | --- | --- |
| Compile SDK | 36 | library and application Gradle files |
| Target SDK | 36 | application Gradle files |
| Minimum SDK | 26 | library and application Gradle files |
| Android Gradle Plugin | 8.13.2 | version catalog |
| Kotlin | 2.0.21 | version catalog |
| Java bytecode target | 11 | module Gradle files |
| Gradle wrapper | 8.13 | wrapper properties |
| CameraX | 1.5.3 | version catalog |
| LiteRT | 1.4.2 | version catalog |

Phase 2 verified the Gradle execution JDK as OpenJDK 21.0.10 from the Android
Studio runtime. Bytecode target 11 remains the compiled-library target; it
does not imply that Gradle runs on JDK 11.

The Phase 2 wrapper was generated from the official cached Gradle 8.13 binary
distribution, not copied from the source repository. Its `distributionUrl`
points to the Gradle 8.13 binary ZIP; the generated wrapper JAR SHA-256 is
`81A82AAEA5ABCC8FF68B3DFCB58B3C3C429378EFD98E7433460610FECD7AE45F`.
The distribution's Apache-2.0 license file had SHA-256
`9536D88EA948603D18E232A13F5958D67807CD80828036B082BFF171D2CF0703`.
The repository already carries the full Apache-2.0 text at `LICENSE`; the
release audit must still include Gradle wrapper notices in its package review.

## Model fingerprint and declared contract

The prior SDK model asset
was measured locally at the pinned source snapshot:

| Property | Value |
| --- | --- |
| Size | 6,128,536 bytes |
| SHA-256 | `051A21BF697858C1E2537354A99BE09A48D26BBFBA0C35216B340F16DE7528D7` |
| Declared model ID | `opennsfw2_resnet50_v1` |
| Declared input | float32 `[1, 224, 224, 3]` |
| Declared input processing | YUV420 to RGB resize, BGR channel order with mean subtraction `[104, 117, 123]` |
| Declared output | float32 `[1, 2]`, `[sfw_probability, nsfw_probability]` |

The source policy asset fingerprint is
`d06a495fa9f777321a240516c0dd624033f13fbec1be002d75310bf9f5417583`.
It declares policy version 1, high-tier threshold 0.75, raw NSFW floor 0.45,
and the other current spatial-policy constants. These values are facts about
the source snapshot, not independently validated suitability claims.

## Known behavior that requires characterization

- Still evaluation combines fixed/adaptive spatial sampling, including
  portrait-specific probes, ambiguity work, corroboration, and early exits.
- Video policy declares a 30-second base interval and a maximum of 35 decoded
  samples.
- The source video aggregation contains an intentional
  `singleUncorroboratedNonExtremeBlockAllow` path. Preserve and characterize
  it before deciding whether to change it.
- The source has a host application trust snapshot and a legacy fail-open policy
  option. Neither belongs in the independent SDK's managed capture path.
- The source's normal non-Allow video path may retain staging. The independent
  SDK must introduce explicit cleanup/recovery in later phases; that is an
  intentional lifecycle difference, not evidence of source parity.

## Expanded code-only snapshot - 2026-10-01

The reusable tooling inventory was compared with a historical source snapshot. Private revision identifiers and source-file digests are omitted. Current camera activity and gallery save/review behavior were inspected as code only; this supplements the extraction baseline and includes no source media or production configuration.
