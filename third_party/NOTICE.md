# Third-party notices

## Bundled model attribution

`safety-opennsfw2/src/main/assets/model/sakos_nudity_model.tflite`
(model ID `opennsfw2_resnet50_v1`) has the following recorded upstream lineage:

- **OpenNSFW2**, the Keras implementation of Yahoo Open NSFW by **Bosco Yung**.
  Copyright (c) 2021 Bosco Yung. Licensed under MIT.
  Upstream: https://github.com/bhky/opennsfw2
  Complete retained license: `licenses/opennsfw2-MIT.txt`.
- **Yahoo Open NSFW**, the original model and Caffe implementation.
  Copyright 2016, Yahoo Inc. Its repository carries the BSD-2-Clause license.
  Upstream: https://github.com/yahoo/open_nsfw
  Complete retained license: `licenses/yahoo-open-nsfw-BSD-2-Clause.txt`.
  Yahoo credits model training to Jay Mahadeokar, in collaboration with Sachin
  Farfade, Amar Ramesh Kamat, Armin Kappeler, and others.

The model was inherited unchanged from `private camera/gallery source project`, which records its import
from `historical model source path omitted`. Its SHA-256 is
`051A21BF697858C1E2537354A99BE09A48D26BBFBA0C35216B340F16DE7528D7`.
The SakOS filename identifies the integrated asset; the original model lineage
is credited above. This SDK did not perform the upstream TFLite conversion.

## Separately generated conversion candidate

A local, unselected candidate was generated from the official OpenNSFW2
`v0.1.0` HDF5 weight release using the pinned OpenNSFW2 `0.15.2` source and
TensorFlow `2.20.0` conversion environment. The candidate is not the currently
bundled SDK model. That input lineage also retains the complete combined
Yahoo Inc. and Marc Dietrichstein BSD-2-Clause notices in
`licenses/tensorflow-open-nsfw-LICENSE.txt`; see
`docs/model-conversion/LICENSE_REVIEW.md` and `docs/PROVENANCE.md` for scope,
source revisions, and the candidate's validation result.

## Retention and scope

The complete license texts retain their copyright notices, permission/conditions,
and disclaimers. Library builds include this notice and all listed license files in
the AAR's `classes.jar` and source JAR under `META-INF/sakos/<module>/`.
Consumers distributing model-bearing binaries must retain the applicable notices
and license texts in documentation or other materials accompanying distribution.

Project-owned material is covered by the repository Apache-2.0 license, which
does not replace these upstream licenses. The exact owner-authorized model is
bundled for private local verification. These attributions do not establish the
unrecorded conversion details or full redistribution rights for the converted
asset; see the repository's `docs/PROVENANCE.md` for the evidence and open gates.
