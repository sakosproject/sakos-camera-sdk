# Release claim and evidence audit

Status: **private audit only; not release-ready**. This document evaluates the
claims currently stored in this repository. It does not establish runtime
behavior, license clearance, package availability, website hosting, or public
delivery.

## Claim classification

| Surface | Claim or statement | Classification | Evidence and limit |
| --- | --- | --- | --- |
| README | The SDK is in development and lacks a bundled model, file-store adapter, recorder start/finalize path and remote coordinates. | Accurately pending | `README.md`, `docs/INTEGRATION.md`, and the Phase 4B/7B records agree. This is not a runtime result. |
| README | Adaptive sampling, temporal review and controlled delivery are project goals. | Planned/future | The capability table is explicitly headed “Planned experience.” Current modules provide policy and injected-contract seams only. |
| README | The local Maven verifier can publish `0.0.0-local` artifacts to `build/local-maven`. | Locally verified | `docs/validation/CONSUMER_REPORT.md` records the separate minified-consumer check. It is neither a remote package nor an install guarantee. |
| Model card | No model is bundled and the expected candidate cannot be used until provenance is cleared. | Accurately blocked | `docs/PROVENANCE.md` records the source-only candidate identity and missing conversion/redistribution evidence. |
| Integration guide | Photo/video behavior is represented by injected review, staging and promotion seams. | Locally verified with limits | Unit tests cover the documented seams. No real inference, private Android file store, CameraX recording or device result exists. |
| Changelog | The release is unreleased and excludes model/runtime, corpus/device validation and remote publication. | Accurately pending | `CHANGELOG.md` matches the plan and release checklist. |
| Security status | No verified security-reporting channel is advertised. | Accurately blocked | `SECURITY.md` identifies the missing prerequisite and offers no endpoint. |
| Website home, camera and guide | The static pages describe local contracts and the local consumer verifier, with runtime/device limits. | Locally verified with limits | `website/` is a dependency-free local preview reviewed in Phase 13. It is not hosted and has no download, demo, telemetry or remote install flow. |

## Candidate-artifact inventory

No release candidate exists. The tracked tree contains no SDK `.aar`, `.apk`,
`.aab`, `.jar` release artifact, or `.tflite` model asset. The Gradle wrapper
JAR is build tooling, not a deliverable. Local Maven output under
`build/local-maven` is generated and ignored; it was validated only during the
recorded local consumer check.

No package registry, namespace, immutable release version, signing procedure,
release tag, hosted release, website host, DNS record, public security endpoint
or release-note destination has been selected for this project.

## Claims that must remain absent until new evidence exists

- Model inference, detection accuracy, latency, safety certification or
  every-frame video coverage.
- Real camera capture, private media persistence, cleanup atomicity or
  physical-device behavior.
- Remote package installation, availability, signing, publication or support.
- Website deployment, live domain behavior, analytics, user accounts or public
  security reporting.

## Release gate trace

The exact gate/stop-condition table is in
[RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md). The next review needs the missing
model conversion and redistribution record, authorized parity inputs/results,
physical-device matrix results, verified security intake, and concrete package
and hosting decisions. Until then, use the local preview and local Maven result
only as internal evidence.
