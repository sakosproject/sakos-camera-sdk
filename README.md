# SakOS Camera SDK

**Managed camera-review contracts for child-oriented Android projects.**

Part of **SakOS — Safe Kids OS**. We are developing a modular Android SDK intended to combine on-device review, adaptive image sampling, temporal video review, and controlled capture delivery.

[Project plan](docs/PROJECT_PLAN.md) · [SakOS on GitHub](https://github.com/sakosproject) · [Apache-2.0 license](LICENSE)

> **In development.** The repository contains extracted contract and policy seams, local-only Maven verification, and a sample-app contract demonstrator. A real bundled model, Android file-store adapter, CameraX recorder start/finalize integration, and remote installation coordinates are not available yet.

## What we are building

| Capability | Planned experience |
| --- | --- |
| Offline evaluation | A bundled model runs on the device without an account or runtime download. |
| Adaptive image review | Contextual and targeted crops combine evidence, with additional work for ambiguous captures. |
| Video review | Timeline sampling and focused follow-up checks produce a clip-level decision. |
| Controlled saving | The managed capture pipeline delivers media only after approval. |
| Modular integration | Use the evaluation engine with an existing camera, or adopt the managed capture modules. |
| Working example | A Kotlin sample app demonstrates photos, video, and an approved-media viewer. |

The SDK is being extracted from existing camera/gallery application work. host application installation, entitlement, signing, and provider requirements will not be required by the independent SDK.

## Privacy boundaries

The planned managed photo pipeline evaluates captures in memory before saving. Rejected photos will not produce SDK-written image files.

Video uses private temporary disk storage while recording and reviewing. The planned lifecycle deletes non-approved clips and recovers abandoned staging files after interruption. It does not promise that rejected video bytes never reach disk or that deletion provides forensic erasure.

Detection is probabilistic, and sampled video review cannot guarantee detection of every unsafe moment. These controls apply to the SDK-managed workflow, not to other camera apps or a modified host application.

## Model and attribution

The existing detection work uses OpenNSFW2/Yahoo Open NSFW lineage together with additional capture, sampling, and policy logic. Exact model redistribution rights, conversion provenance, and third-party notices must be verified before any model is included here.

Project-owned material is licensed under [Apache License 2.0](LICENSE). Future third-party code and model assets retain their applicable licenses; this repository's license does not replace those terms.

## Roadmap

- [x] Establish the SakOS organization and SDK repository.
- [x] Record the module boundaries, privacy contract, and release gates.
- [ ] Complete provenance review and extract the independent Android modules.
- [ ] Preserve and validate the existing detection and sampling behavior.
- [ ] Implement and test video cleanup and recovery.
- [ ] Deliver the sample app and developer integration guides.
- [ ] Launch the SakOS website and publish an experimental SDK release.

See the [project plan](docs/PROJECT_PLAN.md) for scope and acceptance criteria.

## Local consumer verification

The four libraries can be published only to a repository-local Maven directory
with provisional coordinates for a separate minified consumer build:

```powershell
.\scripts\verify-local-consumer.ps1
```

This creates `build/local-maven` temporarily and runs
`integration-tests/consumer` without `project(...)` dependencies. The later
clean build removes the directory; rerun the script to recreate it. See
[the consumer report](docs/validation/CONSUMER_REPORT.md) and
[integration guide](docs/INTEGRATION.md). This is not remote publication or a
claim that the model/runtime works.

## Website and project feedback

The planned website is [sakosproject.org](https://sakosproject.org), with a SakOS homepage, camera product page, and developer documentation. The website is not deployed as part of this repository setup.

Repository owners and collaborators can use [issues](https://github.com/sakosproject/sakos-camera-sdk/issues) for planning. Please use text descriptions and synthetic reproductions; do not attach private photos, videos, or credentials. Public contribution and security-reporting instructions will be established before launch.

## Sample app

The sample demonstrates the Photo/Video mode, runtime availability, and approved-media status without fabricating a capture result. Its capture actions remain disabled until the model runtime, private file-store adapter, and CameraX recorder integration pass their recorded gates. It requests no network permission and writes no media by itself.
