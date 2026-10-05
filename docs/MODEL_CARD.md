# OpenNSFW2 runtime model card

Status: exact owner-authorized asset bundled for private local verification.
External redistribution rights remain unresolved; real-world efficacy is unverified.

| Property | Recorded contract |
| --- | --- |
| Model ID | opennsfw2_resnet50_v1 |
| Asset | model/sakos_nudity_model.tflite |
| Bytes | 6,128,536 |
| SHA-256 | 051A21BF697858C1E2537354A99BE09A48D26BBFBA0C35216B340F16DE7528D7 |
| Input | One float32 tensor [1,224,224,3], BGR subtraction [104,117,123] |
| Output | One float32 tensor [1,2], SFW/NSFW probabilities |
| Runtime | LiteRT 1.4.2, Fixed14/default and Adaptive14/optional evaluation over caller-owned Bitmap |

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
No accuracy, latency distribution, source parity, efficacy or broad hardware
claim follows. BUILD_NOTES separates the synthetic and live-device evidence.

Video review samples a bounded timeline (at most 35 decoded samples), not every
frame. The retained policy permits an isolated, uncorroborated, non-extreme
final block only in the documented absence of review, high-risk and unresolved
crop evidence. That behavior has synthetic characterization only.

[Provenance](PROVENANCE.md) records the exact imported bytes and retained
OpenNSFW2 MIT / Yahoo BSD-2-Clause notices. Owner authorization is not complete
legal clearance of upstream weights or conversion/redistribution. Minimum
distribution requirements and the separately disclosed validation backlog
are in RELEASE_CHECKLIST. Model/preprocessing/policy changes need new identities
and evidence; do not reuse this candidate's results for a replacement.
