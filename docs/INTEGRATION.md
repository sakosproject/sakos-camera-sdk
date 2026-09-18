# Integration guide

## Current supported verification path

The only verified install path is the provisional local Maven flow:

```powershell
.\scripts\verify-local-consumer.ps1
```

It publishes `org.sakos.camera:*:0.0.0-local` under `build/local-maven` and
builds the separate, minified `integration-tests/consumer` app. It does not
publish remotely. See `docs/validation/CONSUMER_REPORT.md` for the exact scope.

## Modules

| Module | Current responsibility |
| --- | --- |
| `safety-core` | Capture/configuration identities, decisions, failures, and fail-closed approval tokens. |
| `safety-opennsfw2` | Policy, score mapping, spatial sampling, and model preflight. No model asset or live inference is bundled. |
| `capture-camerax` | Injected in-memory photo review that delivers only capture-bound Allow results to an approved-output sink. |
| `capture-video` | Temporal planning/aggregation, private-staging state machine, and injected managed promotion seam. |

Evaluation-only callers own their inputs and outputs. The managed photo path
writes only after a matching Allow approval. The managed video path requires an
application-provided private staging store and approved-output promoter; see
`docs/VIDEO_STORAGE.md`.

## Video behavior

Video review is sampled, not every-frame coverage. The temporal engine preserves
the source policy's isolated, uncorroborated, non-extreme final-block Allow rule
only when no review, unresolved high-risk, or unresolved base-crop evidence is
present. Decode/evaluation failures are Failure and unsupported duration is
Review. These rules have synthetic tests only.

## Runtime limits

The exact OpenNSFW2 asset and conversion provenance are unresolved. The SDK
therefore has no real inference runtime, usable live camera flow, independent validation,
physical-device validation, or remote distribution. A host can bypass an
app-level SDK; managed-path guarantees do not control other applications or a
modified host.
