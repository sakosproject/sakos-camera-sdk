# Third-party notices

## Bundled OpenNSFW2 model

`safety-opennsfw2/src/main/assets/model/sakos_nudity_model.tflite` is the
reproducibly converted float32 form of the OpenNSFW2 v0.1.0 pretrained weights.
It is generated from the pinned HDF5 input listed in
`docs/model-conversion/SOURCE_MANIFEST.json` and is identified by SHA-256
`bea35dc93c86f074ae9a047638773aff9eb84c05e6ead8d785af5c8ddde05518`.

- **OpenNSFW2**, the Keras implementation by **Bosco Yung**. Copyright (c) 2021
  Bosco Yung. MIT license. Upstream:
  https://github.com/bhky/opennsfw2
  Complete license: `licenses/opennsfw2-MIT.txt`.
- **Yahoo Open NSFW**, the original pretrained model. Copyright 2016, Yahoo Inc.
  BSD-2-Clause license. Upstream:
  https://github.com/yahoo/open_nsfw
  Complete license: `licenses/yahoo-open-nsfw-BSD-2-Clause.txt`.
- **TensorFlow Open NSFW**, the intermediate conversion implementation by
  **Marc Dietrichstein**. Copyright (c) 2017 Marc Dietrichstein; the combined
  license also retains Yahoo Inc.'s BSD-2-Clause notice. Upstream:
  https://github.com/mdietrichstein/tensorflow-open_nsfw
  Complete combined license: `licenses/tensorflow-open-nsfw-LICENSE.txt`.

The OpenNSFW2 v0.1.0 release identifies its HDF5 asset as the Yahoo pretrained
model migrated through TensorFlow. Yahoo's repository distributes the original
Caffe model beside its BSD-2-Clause terms, which permit source and binary
redistribution when the listed notices and disclaimer are reproduced. The
intermediate TensorFlow port and OpenNSFW2 implementation contribute the
additional BSD and MIT terms listed above. The repository retains every complete
license text and includes them with the model-bearing AAR and source JAR.

## Previously imported SakOS model

The original camera/gallery application import was
`051A21BF697858C1E2537354A99BE09A48D26BBFBA0C35216B340F16DE7528D7`.
Its historical source chain and import record remain in
`docs/PROVENANCE.md`; that file is no longer bundled.

## Retention

Consumers redistributing the model-bearing library or binaries must reproduce
the relevant copyright notices, license conditions, and disclaimers in the
accompanying materials. The safety-opennsfw2 Maven POM links to Apache-2.0 for
SDK code and the three upstream license sources for the model. Gradle packages
the complete local license texts and this notice under
`META-INF/sakos/safety-opennsfw2/`; the artifact inspector checks these bytes.
Other SDK modules contain no model, though their archives also retain the root
notice inventory.
