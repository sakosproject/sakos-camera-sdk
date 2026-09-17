# Private release checklist

Status: **not release-ready**. This checklist is a private preparation record;
it does not authorize a tag, publication, deployment, repository visibility
change, DNS change, or public announcement.

## Current release decision

No release target, registry namespace, release version, signing identity,
hosting platform, DNS change, or verified private security-reporting channel
has been recorded. Those decisions must be supplied and reviewed before a
launch runbook can name an external action.

| Gate | Required evidence | Current status | Stop condition |
| --- | --- | --- | --- |
| Exact model rights | Reviewable conversion chain, exact asset checksum, redistribution notice and approval | Blocked | Do not add or package a model asset. |
| Model runtime | Real bundled-model inference, failure handling and Android runtime evidence | Blocked | Do not claim runtime or offline inference works. |
| Corpus parity | Authorized corpus, matched pinned reference/SDK records and completed comparator report | Blocked | Do not claim accuracy, parity or performance. |
| Physical-device validation | Completed device matrix with sanitized evidence and fault/recovery outcomes | Blocked | Do not claim capture, cleanup or device behavior. |
| Security reporting | Verified private intake path and reviewed public disclosure wording | Blocked | Do not publish a security contact or disclosure policy. |
| Package destination | Confirmed namespace, immutable version, account and approved publishing method | Unresolved | Do not sign, tag or publish. |
| Website destination | Confirmed platform/account, domain/DNS ownership and rollback owner | Unresolved | Do not deploy the local preview or alter DNS. |
| License and notices | Final artifact inspection against cleared dependencies/model and notices | Pending | Do not represent notices as final artifact coverage. |
| Release authorization | User names the exact external actions after reviewing a completed plan | Pending | Do not make any public change. |

## Verified local evidence available for later review

- Phase 11 published four provisional `org.sakos.camera:*:0.0.0-local` AARs
  only to `build/local-maven` and built a separate minified consumer. It was
  not a remote publication or runtime validation.
- Phase 12 documentation and Phase 13 static pages describe those local limits.
- `docs/PROVENANCE.md` and `docs/MODEL_CARD.md` record the excluded model and
  the evidence still required before it can enter a package.
- `docs/validation/PARITY_REPORT.md` and `docs/validation/DEVICE_MATRIX.md`
  are templates/runbooks with pending evidence, not completed validation.
- `SECURITY.md` explicitly says no verified reporting channel is advertised.

## Private review package, once gates are closed

Prepare the following only after the corresponding gates above pass:

1. A sanitized artifact manifest with source revision, version, SHA-256 values,
   module list, model/policy identity and included license/notice files.
2. Reproducible build, local install and consumer commands with captured exit
   status; identify the execution environment without credentials.
3. A claim-to-evidence table for README, model card, changelog and website.
4. A rollback and incident-contact record held in the approved private system.
   This repository must not invent or expose contact details.
5. An explicit user authorization naming each public action independently.

## Scope guard

This file may be updated with evidence, but it must not be used to turn pending
items into pass results. Local builds, local Maven output, synthetic tests, a
static site preview, or a Git remote URL do not establish external delivery.
