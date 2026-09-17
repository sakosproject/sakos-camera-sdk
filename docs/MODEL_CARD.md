# OpenNSFW2 runtime model card

Status: no model asset is bundled as of Phase 4B preflight.

The planned runtime contract is OpenNSFW2/Yahoo Open NSFW lineage with model ID
`opennsfw2_resnet50_v1`, float32 input `[1, 224, 224, 3]`, BGR mean subtraction
`[104, 117, 123]`, and float32 output `[1, 2]` representing SFW and NSFW
probabilities. The preflight expects a 6,128,536-byte asset at
`model/sakos_nudity_model.tflite` with SHA-256
`051A21BF697858C1E2537354A99BE09A48D26BBFBA0C35216B340F16DE7528D7`.

The SDK presently provides only a streaming size/digest preflight and a
fail-closed evaluator. It cannot perform inference until the exact conversion
provenance and redistribution notices for the asset are verified. It makes no
accuracy, latency, coverage, child-safety certification, or every-frame video
claims.

The public upstream notices are retained under `third_party/licenses/`. The
exact converted asset remains excluded pending the provenance record.

Alternative models, preprocessing changes, or policy changes require their own
version identity and corpus/device evaluation. The existing temporal engine
samples a bounded set of frames; it does not inspect every frame. Its preserved
source-policy behavior can allow an isolated, uncorroborated, non-extreme final
block only under the documented absence of review, high-risk, and unresolved
crop evidence. This behavior has synthetic characterization only, not model or
corpus validation.
