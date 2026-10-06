# Draft candidate notes — private, unreleased

The local `0.0.0-local` candidate includes four independent Android libraries,
the exact owner-authorized bundled model, Fixed14/default and Adaptive14/optional
Bitmap review, first-run camera discovery/calibration and controls, standalone
approval-bound private library and explicit authorized-save adapters, in-memory
photo delivery and private staged/sampled video review. The sample provides
camera permission/preview, photo/video capture and a private approved-only viewer.

Local AAR/source/POM/notices inspection, JVM tests, lint, synthetic Android tests
and a separate minified consumer are described in BUILD_NOTES/RELEASE_EVIDENCE.
Current fixtures use benign generated patterns, mocks, simulated scores and an
isolated emulator scene. Ten focused synthetic phone checks and one authorized
live-camera flow passed on Samsung SM-G781W/API 33 on October 2, 2026. Runtime
and camera execution demonstrate exercised mechanics, not classifier accuracy.

Real-world efficacy, exact source parity and broader physical-device coverage
remain unverified. The selected replacement model's published license conditions
and required notices are documented as satisfied; release still requires an approved
version, delivery target and verified versioned artifacts. Packages remain
unreleased; helper-site hosting is tracked separately in website/README.md.
Video sampling is bounded; it does not inspect
every frame. Rejected video can touch private temporary disk; deletion is not
forensic erasure. A host can bypass library controls.

Use the minimum experimental distribution requirements in RELEASE_CHECKLIST.
Broad device coverage and independent validation are disclosed follow-up work.
No independent validation artifacts are included.
