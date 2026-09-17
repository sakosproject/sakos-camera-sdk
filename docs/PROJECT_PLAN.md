# SakOS Camera SDK project plan

Status: repository foundation complete; SDK extraction and website implementation are planned.

## Purpose

Build an independent Android SDK and sample app under SakOS (Safe Kids OS), preserving the existing capture-review helpers, adaptive image sampling, and temporal video review developed for camera/gallery application. Consumers must not need host application.

## Agreed direction

- Repository: `sakosproject/sakos-camera-sdk`, private during preparation, intended for a reviewed public launch.
- Website: `https://sakosproject.org`, with a SakOS homepage, `/camera/` product page, and `/docs/camera/` developer documentation.
- Deliver an SDK plus sample app with both photo and video support.
- Use Apache-2.0 for project-owned material and retain separate upstream licenses and notices.
- Bundle offline inference; require no account, runtime download, Firebase, or host application installation.
- Keep the existing host application product separate during extraction.

## Planned modules

| Module | Responsibility |
| --- | --- |
| `safety-core` | Evaluation contracts, versioned policy, decisions, and failure reasons |
| `safety-opennsfw2` | Model adapter, preprocessing, spatial sampling, evidence aggregation, and LiteRT runtime |
| `capture-camerax` | In-memory photo capture and approved-only output |
| `capture-video` | Private recording, temporal review, promotion, and cleanup |
| `sample-app` | Camera UI and minimal approved-media viewer |

Keep UI optional for library consumers. Offer an evaluation-only integration and a managed capture integration; only the managed pipeline can make SDK-controlled storage guarantees.

## Preserve and verify the existing work

- Preserve fixed and adaptive crop strategies, portrait-specific checks, corroborated evidence, early exits, runtime buffer reuse, and video sampling/escalation logic.
- Move host authorization outside content classification; do not replace host application checks with fake trusted host application state.
- Compare decisions with the source implementation on the same authorized corpus before changing thresholds or strategies.
- Keep model-specific preprocessing and thresholds versioned together. Alternative models require separate validation.
- Retain portable benchmark tooling and regression tests without copying private media or machine-specific configuration.

## Storage contract

- Photos: evaluate in memory before saving; no SDK-written image file for rejected captures.
- Video: use private temporary disk storage, expose only approved clips, and delete rejected, failed, or cancelled clips and sidecars.
- Purge abandoned video sessions before accepting new recordings. Report cleanup failures and block new recording until resolved.
- Exclude staging from backups, galleries, thumbnails, and export paths.
- Do not claim disk-free video, guaranteed forensic erasure, perfect detection, or enforcement over other apps or a modified host.
- Review unresolved/parent-review outcomes without retaining rejected media by default; a parent review product flow is outside the initial scope.

## Delivery stages

1. Foundation: organization profile, private repository, license, README, ignore rules, local checkout, and this tracked plan.
2. Provenance and extraction: verify rights and attribution; selectively extract the modules while retaining the current Android/toolchain baseline.
3. Validation: sample integration, fault handling, behavioral comparison, and physical-device checks.
4. Public presentation: coherent identity, polished README, real screenshots/demo, landing page, quick start, API guide, model card, limitations, contribution guide, and security reporting instructions.
5. Release: verify a minified independent consumer from a local Maven repository, then prepare a reviewed experimental `0.x` publication and public repository launch.

## Acceptance gates

- Verify redistribution rights for the exact bundled model and SakOS-derived material; preserve full third-party license texts and attribution.
- Audit the allowlisted export for credentials, production configuration, internal logs, private test media, and branding rights.
- Test missing/corrupt models, invalid scores, inference failure, cancellation, process death, low storage, deletion failure, and interrupted promotion.
- Confirm first-run airplane-mode operation without host application; cover front/back cameras, rotation, photos, video, and minified release integration.
- Report measured false accepts, false rejects, latency, and video sampling limitations with model/policy versions and evaluation scope.
- Run focused tests and a clean debug build for Android changes, retain build logs, and record command/results. Documentation-only setup does not require Gradle.

## Setup execution record — 2026-09-17

- Updated the SakOS GitHub organization display name, description, and website link.
- Created the private SDK repository with an initial README, Android ignore template, and Apache-2.0 license.
- Cloned it into the existing empty project directory.
- Added this plan and an introductory README. No SDK source, model weights, private corpus, or production configuration has been imported.
- Website deployment and DNS changes have not been performed. This setup does not assert SDK or model release readiness.
