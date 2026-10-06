# OpenNSFW2 runtime model card

Status: reproducible OpenNSFW2 float32 conversion is bundled. Attribution and
the published license conditions are satisfied in the repository artifacts;
real-world efficacy remains unverified.

The bundled model was generated from the pinned OpenNSFW2 `v0.1.0` HDF5 release
using the locked recipe in this repository. Its lineage is [Yahoo Open NSFW](https://github.com/yahoo/open_nsfw)
through the [TensorFlow Open NSFW port](https://github.com/mdietrichstein/tensorflow-open_nsfw)
and [OpenNSFW2](https://github.com/bhky/opennsfw2). The repository retains the
OpenNSFW2 MIT, Yahoo BSD-2-Clause, and combined TensorFlow Open NSFW BSD-2-Clause
licenses. See [PROVENANCE](PROVENANCE.md) and [third-party notices](../third_party/NOTICE.md).

| Property | Recorded contract |
| --- | --- |
| Model ID | opennsfw2_resnet50_v1 |
| Asset | model/sakos_nudity_model.tflite |
| Bytes | 23,608,404 |
| SHA-256 | bea35dc93c86f074ae9a047638773aff9eb84c05e6ead8d785af5c8ddde05518 |
| Input | One float32 tensor [1,224,224,3], BGR subtraction [104,117,123] |
| Output | One float32 tensor [1,2], SFW/NSFW probabilities |
| Embedded conversion metadata | TensorFlow 2.20.0, converter API 2, Keras input, float32 conversion; custom and Select TF ops disabled |
| Stored constants | 108 FLOAT32 and 2 INT32; float32 external input/output |
| Minimum runtime metadata | 1.6.0 |
| Runtime | LiteRT 1.4.2, Fixed14/default and Adaptive14/optional evaluation over caller-owned Bitmap |

The selected float32 conversion was reproduced byte-for-byte in two clean output
directories and passed the predeclared `1e-4` maximum absolute error limit against
the source Keras model on eight generated tensor patterns. The dynamic-range
alternative exceeded its predeclared `1e-2` limit and was not selected. No device
test was run for this replacement. This synthetic result validates the conversion
against its source implementation; it does not establish real-world accuracy or
broad classifier efficacy. The historical import is recorded only by hash in provenance; its bytes are absent from this candidate history and the current SDK asset. See the
[conversion recipe](MODEL_CONVERSION_PLAN.md) and
[recorded conversion results](model-conversion/CONVERSION_MANIFEST.json).

The runtime checks size/digest, tensor count, shape and dtype before use. It
rejects recycled input, closed runtime and invalid/non-finite output. The safety
adapter matches request configuration and returns failures without approval.
Owned scratch/native resources close deterministically; the input stays caller-owned.
Fixed14 uses the documented early Block exit. Optional Adaptive14 executes the
source staged context, portrait sentinel/ambiguity/refinement, targeted escalation
and fallback policy. Its policy identity is `opennsfw2-still-policy-adaptive14@1`;
the default retains `opennsfw2-still-policy@1`. No threshold is retuned.
The bundled configuration accepts only its recorded version-1 policy values;
a custom policy must have a separate versioned configuration/runtime contract.

Current Android runtime checks use generated benign shapes or solid-color
patterns. JVM policy checks use simulated scores. Temporal decoding checks encode
solid YUV patterns locally. Emulator camera flows and the October 2 authorized
Samsung SM-G781W/API 33 live flow establish exercised managed-path mechanics.
The live phone test used Default-Fixed14; Adaptive14 remains synthetic-tested.
No accuracy, latency distribution, broad image-level source parity, efficacy or
broad hardware claim follows. BUILD_NOTES separates the synthetic and live-device evidence.

Video review samples a bounded timeline (at most 35 decoded samples), not every
frame. The retained policy permits an isolated, uncorroborated, non-extreme
final block only in the documented absence of review, high-risk and unresolved
crop evidence. That behavior has synthetic characterization only.

[Provenance](PROVENANCE.md) records the historical imported bytes and the selected
model's source and digest. The license review documents the published MIT and
BSD-2-Clause terms and confirms that the required notices accompany the model
artifacts. The release version and delivery target remain pending. Model,
preprocessing, or policy changes need new identities and evidence; do not reuse
this model's results for a later replacement.
