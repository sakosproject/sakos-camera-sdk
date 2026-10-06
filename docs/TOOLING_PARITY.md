# Source-derived tooling and standalone boundary

This SDK contains an authorized, code-only adaptation of camera and gallery
workflow behavior from a related Android application. This publication copy omits
private source-project identifiers, package paths, revisions, and source-file
digests. No source media, private test corpus, credentials, signing material,
telemetry payloads, or source build configuration is included.

| Capability area | SDK implementation | Evidence boundary |
| --- | --- | --- |
| Camera discovery and calibration | Advertised capability inventory, verified selectors, calibration profiles, safe defaults, retries, and diagnostics | Generated/synthetic and bounded device mechanics only; no broad OEM/API coverage claim |
| Photo and video capture | In-memory photo review; no-backup video staging, finalization, bounded temporal sampling, cleanup and recovery | Does not inspect every video frame; deletion is not forensic erasure |
| Reviewed media library | Capture/configuration-bound approvals, approved-only inventory and preview, private playback, explicit authorized save | Host integration and real gallery behavior remain outside this SDK |
| Model evaluation | Fixed14 default and optional Adaptive14 policy over caller-owned inputs | Conversion and synthetic policy evidence do not establish classifier accuracy or efficacy |

## Standalone ownership differences

The libraries use injected host interfaces instead of app-specific account,
entitlement, trust, provider-authority, or diagnostics dependencies. Capture and
approval ownership, cancellation, cleanup, and selector safeguards remain explicit.
The sample does not automatically export to a device gallery and requests no
microphone, storage, or network permission.

This document describes design ancestry and the independent SDK boundary. It is
not a claim of exact source parity, classifier accuracy, or broad hardware behavior.