# Private local candidate preparation runbook

Local candidate preparation is authorized; external actions remain unapproved.
The owner expressly superseded the previous requirement to stop local preparation
on missing external release decisions.

1. Confirm the existing worktree/branch and reviewed source revision. Preserve
   unrelated changes. Local narrow commits are authorized.
2. Run `scripts/verify-local-candidate.ps1` using the installed cached toolchain.
   Provide `-Serial` only for an explicitly authorized isolated emulator. The
   script checks emulator identity and targets every adb action to that serial.
   It never creates/wipes an AVD, alters a VM or accesses a physical device.
3. The script runs clean debug assembly, all JVM tests, synthetic instrumented
   APK compilation and lint before local-only Maven packaging. It builds a
   separate minified consumer and an additional test-key-only runtime variant.
   Direct adb instrumentation avoids uncached Gradle UTP runner dependencies.
4. Review `build/private-candidate/manifest.json`, `SHA256SUMS.txt`, sanitized logs,
   source/POM/dependency/notices inventory and exact bundled-model fingerprint.
   APK permission inspection rejects network/audio/public-storage permission.
   Inspect the private text-audit report outside the repository; do not copy
   sensitive historical wording or validation details into public artifacts.
5. Repeat the script to verify it succeeds from clean output. Compare library
   artifact hashes for the same code/toolchain. Do not claim universal byte
   reproducibility of signed APKs or builds on different machines.
6. Record sanitized current synthetic outcomes in BUILD_NOTES/RELEASE_EVIDENCE.
   Candidate manifests explicitly identify a dirty tree if built before final
   commits; regenerate at a clean reviewed commit for the final local candidate.

Keep model rights, independent-validation status, real-world efficacy,
physical-device/API-range coverage, dependency notice clearance, intake and
external destinations unresolved until evidence/owner decisions arrive.

Stop before any push, tag, merge, visibility change, real release signing,
upload/publication, deployment, DNS action or public communication. Each needs
an explicit instruction naming the target. This runbook grants none of them.
