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

## Verified upstream license evidence

On 2026-09-17, the upstream OpenNSFW2 MIT license and Yahoo Open NSFW
BSD-2-Clause license were read from their public canonical repositories and
copied verbatim into `third_party/licenses/opennsfw2-MIT.txt` and
`third_party/licenses/yahoo-open-nsfw-BSD-2-Clause.txt`. The MIT terms permit
distribution when its notice is retained. The Yahoo terms permit source and
binary redistribution when its copyright notice, conditions and disclaimer are
retained. The associated Yahoo documentation describes the model as preliminary
pornography filtering and does not guarantee accuracy.

This establishes the applicable upstream notices if the local model is proven
to be a permitted derivative of that lineage. It does not itself establish the
conversion provenance of the exact local `.tflite` asset.

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

Until then, the model asset and any code whose provenance is not covered below
are **blocked**. The SDK may use independently authored scaffolding and
clean-room implementations of documented behavior, subject to later policy
and parity validation. This distinction must remain visible in future commits.

## User-authorized camera/gallery application code export

The user explicitly directed this project to extract the camera/gallery application
work into an independent SDK and to preserve the detection helpers and
sampling strategies developed there. The inspected source remote is under the
user's `mendipan` account. At the pinned source commit, Git history for the
candidate spatial, runtime, photo and video files listed in the extraction
manifest shows only `mendypan` or `Mendi Yuda` as authors.

That evidence and authorization clear a **selective export of those
project-owned code files** into this repository, with their source revision
and hashes retained in the manifest. It does not clear the model asset,
private SakOS upstream material, a copied third-party contribution that was
not visible in the inspected history, or new artwork/media. Keep attribution
notes and the source commit in the extracted code/documentation, and do not
copy excluded application, signing, diagnostics or corpus content.

This clearance allows Phases 4A and 5–7 to adapt the listed project-owned
algorithms. Phase 4B remains blocked until the exact model asset rights and
conversion provenance are resolved.

## Phase 4B provenance preflight — 2026-09-17

- Public upstream license evidence is now retained in this repository as above.
- The authorized private SakOS provenance host could not be reached during this
  preflight, so the private model card, conversion script and repository notices
  could not be re-read. This is an observation of reachability, not evidence
  that the source repository or its records do not exist.
- Do not import the local `.tflite` or run a real-model phase until the host is
  reachable or equivalent reviewable conversion evidence is supplied.

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
