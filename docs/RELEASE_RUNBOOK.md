# Publication and local candidate runbook

The owner authorized repository integration/push and helper-site deployment on
2026-10-05. Use [RELEASE_CHECKLIST](RELEASE_CHECKLIST.md) for the minimum requirements
for each deliverable. SDK package publication, tags, release signing and repository
visibility changes need a concrete approved version/target before execution.

## Helper site

1. Correct status copy and run `python scripts/check-local-links.py` plus
   `git diff --check`. Review the small static upload payload.
2. Commit the reviewed changes, fast-forward `main`, and push the existing origin.
3. Verify Wrangler authentication and the Pages project/account, then deploy the
   HTML/CSS through the repeatable command in [website/README](../website/README.md).
4. Check the hosted home, camera, status and CSS routes over HTTPS and confirm their
   content matches the committed payload. Attach the apex through Custom domains,
   or deliver the working Pages URL and the documented manual steps.
5. Record the deployment identity, source revision, route checks and actual apex
   status; commit and push the execution record.

## Local SDK candidate and later experimental distribution

1. Confirm the reviewed source revision and preserve unrelated changes. Use
   `scripts/verify-local-candidate.ps1` when implementation/model/toolchain or
   packaging changes require candidate verification. For unchanged artifacts,
   retain the existing source-bound passing evidence and verify hashes instead
   of repeating whole suites for documentation edits.
2. The verifier runs clean builds, JVM tests, lint and local Maven packaging, plus
   a separate minified consumer. Supply `-Serial` only for an authorized isolated
   emulator; it never creates/wipes an AVD or changes a VM. Live-phone checks are
   separately authorized and are not enabled by the normal synthetic runner.
3. Review the ignored candidate manifest/checksums/logs, model fingerprint,
   notice/dependency inventory, permissions, strategy identities and test scope.
   Record relevant changes/results in BUILD_NOTES and RELEASE_EVIDENCE.
4. For experimental distribution, confirm exact redistribution rights/notices,
   select an immutable release version and registry/delivery target, bind artifact
   hashes to verification evidence, and publish accurate known limitations.
   Broad API/OEM, efficacy and independent-validation work stays in the disclosed
   backlog unless the proposed claims require it.
5. Publish only the approved deliverable/version to the approved destination.
   Helper-site authorization does not itself publish SDK/model artifacts.

Historical candidate manifests retain the gates used when generated. New manifests
separate minimum release requirements from the validation backlog. No historical
build or physical test is represented as a new run.
