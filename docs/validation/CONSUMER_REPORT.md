# Local Maven consumer report

Status: **locally verified on 2026-09-17**.

Phase 11 uses provisional local-only coordinates:

```text
org.sakos.camera:safety-core:0.0.0-local
org.sakos.camera:safety-opennsfw2:0.0.0-local
org.sakos.camera:capture-camerax:0.0.0-local
org.sakos.camera:capture-video:0.0.0-local
```

`scripts/verify-local-consumer.ps1` publishes the four release AARs to
`build/local-maven` and compiles `integration-tests/consumer` as a separate,
minified Android build. The consumer uses Maven coordinates only; it does not
declare `project(...)`, a composite build, or a production checkout path.

Local publication and a minified compile do not prove model availability,
offline runtime behavior, physical-device behavior, source/corpus parity,
remote publication, or release readiness.

The exact verification script published the four AARs and built the separate
consumer's minified release. Its dependency graph resolved the coordinates from
the local Maven repository with no project/composite substitution. The later
SDK clean build removes `build/local-maven`; rerun the script to recreate it.
