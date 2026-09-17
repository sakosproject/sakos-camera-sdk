# Provenance and redistribution record

Status: **source/model import clearance is incomplete** as of 2026-09-17.

This record distinguishes evidence observed in the local source repository
from clearance to redistribute material in SakOS Camera SDK. It does not grant
rights, replace contributor approval, or relicense third-party assets.

## Observed evidence

The local source document `docs/UPSTREAM_PROVENANCE.md` states that:

- the exact model asset was imported through private SakOS repositories;
- the declared model lineage is OpenNSFW2 (MIT wrapper) and Yahoo Open NSFW
  (BSD-2-Clause model lineage);
- the SakOS wrapper and gate repositories declare Apache-2.0 for their own
  wrapper code.

The local source model contract independently names the same lineage and
declares its tensor/preprocessing contract. The local source Git history has
an import checkpoint for the model asset and source-history entries for the
runtime and temporal gate. These observations establish a lead for further
review, not the chain of title or redistribution terms for the exact converted
`.tflite` file.

No tracked `LICENSE`, `NOTICE`, or attribution text for the exact model asset
was found in the inspected source module. No private SakOS repository contents,
conversion script, model-card source, or contributor authorization was imported
into this repository during Phase 1.

## Required clearance before import

The following evidence must be placed in a reviewable internal record before
the corresponding material can enter this repository:

1. The exact upstream license/notice text that governs the source model and
   permits distribution of the converted Android asset.
2. A reproducible conversion provenance record tying the exact source/checksum
   to the candidate `.tflite` checksum and documenting modifications.
3. Confirmation that the private SakOS repository owner and every relevant
   contributor authorized Apache-2.0 redistribution of any copied/adapted
   wrapper and gate code, including contribution-history obligations.
4. A review of any artwork, sample media, benchmark fixtures, names, and
   screenshots proposed for inclusion.
5. A final attribution/notice inventory checked against the packaged SDK and
   local Maven artifact, not only source files.

Until then, the model and all verbatim/adapted code from the source repository
are **blocked**. The SDK may use independently authored scaffolding and
clean-room implementations of documented behavior, subject to later policy
and parity validation. This distinction must remain visible in future commits.

## Third-party material handling

No third-party license text is included yet because no third-party code or
asset has been imported. When an item is cleared, add the unmodified verified
license/notice text under `third_party/licenses/`, link it from this document,
and identify the exact packaged files it governs. The project-wide Apache-2.0
license applies only to SakOS-owned material.

## Public-release restriction

Do not publish a model-bearing artifact or represent the model as redistributable
until all required clearance items are evidenced and reviewed. A matching hash,
a declared lineage, or an Apache declaration in another private repository is
not sufficient by itself.
