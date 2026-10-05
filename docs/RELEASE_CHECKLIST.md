# Private local candidate and external release gates

Local SDK implementation/testing and candidate packaging remain local. SDK/model
release, tagging, upload, Git push, signing and repository-visibility changes
are outside this status update. The status-only site targets Cloudflare Pages
at `sakosproject.org`; its project/account binding could not be verified in the
available environment, so no deployment or DNS mutation was made. The candidate
remains provisional `0.0.0-local`.

| Gate | Current evidence/status | Remaining requirement |
| --- | --- | --- |
| Exact asset identity | Owner-authorized bundled bytes; size/digest/tensor checks | Complete upstream conversion/weight provenance and owner/legal redistribution clearance |
| Local runtime | Fixed14/default and Adaptive14/optional Bitmap inference, source-derived camera calibration and Android temporal decoder implemented; current synthetic checks in BUILD_NOTES | Real-world efficacy and source parity are not established |
| Managed sample | Permission/lifecycle photos, silent private video and approved-only viewer implemented | Broader API-range and physical-camera/lifecycle/storage coverage |
| Private artifacts | Four local AARs, sources, POMs, notices and minified consumer | Final dependency/license inventory and release version approval |
| Independent validation | Private/outside repository; no completion claim here | Owner-approved status outside repository |
| Physical devices | One authorized live flow on Samsung SM-G781W / Android 13 passed on 2026-10-02 | Broader physical-camera, API-range, lifecycle and OEM coverage; no efficacy claim |
| Secret/private-data audit | Private heuristic text-only scan, redacted findings outside repository | Owner review; a heuristic pass is not provenance or historical clearance |
| Security intake | No verified public/private reporting endpoint advertised | Owner selects and verifies intake before launch |
| External package/website | The status-only helper site targets Cloudflare Pages at `sakosproject.org`; no account/project binding, deployment or DNS mutation was verified or performed | Verify the existing Pages project and same-account apex-zone binding, then complete the authorized site publication without distributing SDK/model artifacts or changing repository visibility |

Use `scripts/verify-local-candidate.ps1` for local preparation. Generated manifests,
checksums and sanitized logs live under ignored `build/private-candidate`;
prospective-public documentation records only sanitized current synthetic evidence.
No local build, emulator test or checksum closes the legal, physical or efficacy
gates. Website preview and provisional local Maven coordinates do not establish
remote availability. Retain all notices and model identity unchanged.
