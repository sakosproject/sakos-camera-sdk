# Model input attribution and license record

The planned candidate uses `open_nsfw_weights.h5` from OpenNSFW2 release
`v0.1.0`, identified by its release URL and pinned in
[`SOURCE_MANIFEST.json`](SOURCE_MANIFEST.json). OpenNSFW2's release notes say
these are Yahoo Open NSFW pretrained weights migrated through TensorFlow for
its TensorFlow 2 implementation. This describes the new candidate's input
lineage only; it does not establish which exact bytes generated the inherited
SakOS TFLite asset.

The repository retains these complete upstream license texts:

- OpenNSFW2 MIT: [`opennsfw2-MIT.txt`](../../third_party/licenses/opennsfw2-MIT.txt),
  from `bhky/opennsfw2` revision
  `19530b8f08aac12479a901fe18763c0392c8bd8c`.
- Yahoo Open NSFW BSD-2-Clause:
  [`yahoo-open-nsfw-BSD-2-Clause.txt`](../../third_party/licenses/yahoo-open-nsfw-BSD-2-Clause.txt),
  from `yahoo/open_nsfw` revision
  `a4e13931465f4380742545932657eeea0a10aa48`.
- TensorFlow Open NSFW's complete combined license:
  [`tensorflow-open-nsfw-LICENSE.txt`](../../third_party/licenses/tensorflow-open-nsfw-LICENSE.txt),
  from `mdietrichstein/tensorflow-open_nsfw` revision
  `ead9f4d1748e8bc80ab14bf0a36f696a5fe4109d`. It retains Yahoo Inc.'s terms
  and Marc Dietrichstein's 2017 BSD-2-Clause notice.

[`third_party/NOTICE.md`](../../third_party/NOTICE.md) attributes the model
lineage and describes the additional retained notice. The SDK's artifact
inspection requires every library AAR and source JAR to carry the complete
notice inventory. These records are attribution evidence; they do not by
themselves settle every question about redistribution scope for trained model
weights or a converted binary. Keep any unresolved rights question as an
external distribution gate.

The TensorFlow and OpenNSFW2 Python packages are conversion tools and are not
copied into the Android artifacts. Their exact wheel identities are recorded
in `tools/model-conversion/uv.lock`.
