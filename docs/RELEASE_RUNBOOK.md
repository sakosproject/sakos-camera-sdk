# Private release-preparation runbook

Status: **stopped before external actions**. Follow this only after every gate
in [the release checklist](RELEASE_CHECKLIST.md) is marked passed with linked,
sanitized evidence. Completing local steps below does not authorize public
delivery.

## Inputs required before a candidate exists

Record these in an approved private review location, not in this repository:

- the immutable source revision and intended release version;
- approved package registry, namespace, account and publishing procedure;
- approved website host, domain/DNS account and rollback owner;
- signing/material handling procedure, without keys or tokens in source;
- verified private security-reporting path and approved public wording;
- model authorization, independent validation status, and completed
  physical-device matrix.

If any input is absent, stop and update `docs/RELEASE_CHECKLIST.md` with the
missing decision or evidence.

## Private preparation sequence

1. Recheck repository status and pin the reviewed commit. Confirm all intended
   changes are included and unrelated work is excluded.
2. Re-run only the focused checks invalidated by the candidate changes. Keep
   raw logs private; record sanitized exit status and conclusions.
3. Inspect the candidate artifact contents against a finalized artifact
   manifest: modules, model/policy identity, licenses/notices and hashes.
4. Compare every public statement in README, model card, changelog and website
   with the evidence table. Remove or block unsupported claims.
5. Have the named reviewers confirm model rights, security intake, device
   evidence, independent-validation status, package destination and website/DNS destination.
6. Prepare a rollback record that identifies the exact artifact and website
   revision to restore. Keep contact details in the approved private system.

## Mandatory stop before each external action

Stop after private review. Obtain a fresh, specific user instruction before
performing any one of these distinct actions:

- creating a commit/tag or pushing a branch;
- changing repository visibility or creating a hosted release;
- signing, uploading or publishing a package;
- deploying the website or changing DNS;
- posting release notes, security details or other public communication.

The instruction must name the intended action and target. Approval for one does
not imply approval for the others.

## If a later authorized action fails

Stop dependent actions, preserve the observed error and current external state,
and update the private review record. Do not retry with changed credentials,
targets, DNS, release metadata or visibility settings unless separately
authorized. Report which artifacts or routes, if any, actually became public.
