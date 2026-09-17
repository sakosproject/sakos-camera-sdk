# Candidate release manifest template

Status: **template only — no candidate exists**. Replace `UNSET` fields only
after every prerequisite in [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) passes
and a private review has approved the completed manifest. This file is not a
publication record.

## Candidate identity

| Field | Value | Required evidence |
| --- | --- | --- |
| Source commit | `UNSET` | Clean reviewed commit and scoped diff. |
| Release version | `UNSET` | Approved immutable version decision. |
| Package registry/namespace | `UNSET` | Approved account and publishing method. |
| Website host/domain | `UNSET` | Approved host, DNS ownership and rollback owner. |
| Security-reporting wording | `UNSET` | Verified private intake and approved public text. |
| Release decision | `UNSET` | Explicit user authorization naming the external action. |

## Artifact inventory

Record each generated deliverable only after it exists. Do not list local Maven
output, build tooling, a model candidate, or a local static preview as a release
artifact.

| Artifact | SHA-256 | Included modules/assets | License/notice review | Local install evidence |
| --- | --- | --- | --- | --- |
| `UNSET` | `UNSET` | `UNSET` | `UNSET` | `UNSET` |

## Model and validation identity

| Field | Value | Evidence |
| --- | --- | --- |
| Model asset/version | `UNSET` | Cleared conversion and redistribution record. |
| Policy/preprocessing identity | `UNSET` | Model/runtime implementation and test evidence. |
| Corpus parity report | `UNSET` | Completed authorized comparison report. |
| Physical-device matrix | `UNSET` | Completed sanitized device evidence. |
| Local consumer validation | `UNSET` | Re-run against this candidate, not a prior local build. |

## Website review artifact

The current website is a local static preview. Review it locally with:

```powershell
python -m http.server 4173 --directory website
```

Opening `http://localhost:4173/` demonstrates the static content only. It does
not validate hosting, CDN behavior, TLS, DNS, analytics, accessibility on a
deployed origin, or a public route.

## Sign-off

| Review | Name/date | Evidence link | Result |
| --- | --- | --- | --- |
| Model/provenance | `UNSET` | `UNSET` | `UNSET` |
| Runtime/parity/device | `UNSET` | `UNSET` | `UNSET` |
| License/notice/security | `UNSET` | `UNSET` | `UNSET` |
| Package/website rollback | `UNSET` | `UNSET` | `UNSET` |
| User external-action approval | `UNSET` | `UNSET` | `UNSET` |
