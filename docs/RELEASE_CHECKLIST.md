# Minimum publication requirements

The owner authorized repository integration/push and helper-site publication on
2026-10-05. Site publication and experimental SDK distribution are independent.
The SDK remains `0.0.0-local`; no SDK package release or visibility change is part
of the helper-site launch.

## Helper site

Required before publishing the static status pages:

- Accurate status and limitations; working navigation and HTTPS routes/assets.
- Publish only the site's HTML/CSS, with no credentials, private media, SDK/model
  downloads or unverified reporting endpoint.
- Confirm the intended Cloudflare account/Pages project and hostname. Publication
  is authorized; attach the owner-controlled apex through Pages Custom domains,
  or provide a working Pages hostname and manual attachment steps.

Model redistribution clearance, efficacy studies, broad device testing and a
formal security intake are not requirements for this status-only helper site.
Deployment/domain status is recorded in [website/README](../website/README.md).

## Experimental SDK distribution

| Minimum requirement | Current status |
| --- | --- |
| Confirm permission to redistribute the included code, exact model weights/conversion and dependencies; retain required licenses/notices | Owner import authorized and upstream notices retained; exact external distribution permission and final notice inventory still need confirmation |
| Approved experimental version/delivery target, immutable artifact hashes, relevant build/tests and minified-consumer verification | Local `0.0.0-local` candidate passed; 24 artifacts verified, four local publications and consumer checks retained; final release version/target not selected |
| Accurate documentation of tested scope, privacy behavior and known limitations; exclude secrets/private media | Current docs disclose sampled video, probabilistic detection and one-handset evidence; private text audit/evidence retained |

Reuse valid source-bound evidence for unchanged artifacts. Rebuild/rerun affected
checks when implementation, model, toolchain or packaging changes; a documentation
edit alone does not require another full Android/device suite. An experimental
release does not require production signing of library AARs or a sample APK release.

## Disclosed validation backlog

Broader API/OEM/camera coverage, long-run performance, real-world classifier
false-accept/false-reject evaluation, source parity and independent validation
remain unverified. They are follow-up work, not blanket blockers for an accurately
labelled experimental release. Stronger accuracy, certification or broad
compatibility claims need supporting evidence before they are made.

No verified private reporting endpoint is advertised. Verify a reporting route
before inviting private security reports; a formal intake program is not a
helper-site publication prerequisite. The October 2 Samsung SM-G781W/API 33 flow
supports only the camera/control/storage mechanics exercised on that handset.
