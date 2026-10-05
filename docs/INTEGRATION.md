# Integration guide

The four Android libraries require minSdk 26 and use compileSdk 36 / Java 11
bytecode. The local candidate uses CameraX 1.5.3 and LiteRT 1.4.2. There are no
remote Maven coordinates. Run `scripts/verify-local-consumer.ps1` to build local
AARs, sources, POMs and a separate minified Maven consumer.

## Photo

Open `OpenNsfw2BitmapRuntime` with an application context and adapt it through
`OpenNsfw2BitmapEvaluator`. Use `runtime.configuration` for the
request. Fixed14 is the compatible default. Pass `strategy =
IntegratedOpenNsfw2Strategy.Adaptive14` to `open` for the optional staged driver;
its configuration/approval identity differs from the default. Capture in memory with CameraX `OnImageCapturedCallback`, then feed
`ManagedPhotoReviewPipeline.reviewImageProxy` with a capture ID unique to that
input, its dimensions/rotation and a converter. The pipeline closes the proxy
exactly once, including failures. Caller-owned converted Bitmaps also need
recycling after review. Only a matching capture/configuration Allow reaches
`ApprovedPhotoSink`. Keep sinks transactional and check host cancellation before
committing an output. `ManagedPhotoCaptureCallback` also accepts a coroutine
context for host cancellation; its default context has no host lifecycle.

## Video

Construct `AndroidVideoPrivateStagingStore(context)` and
`VideoStagingSessionManager`; call startup recovery before accepting a recording.
Do not recover sessions while a recording/review is live. Use
`CameraXPrivateVideoRecordingFactory` to prepare private output, then pass the
CameraX finalization to `CameraXVideoFinalizationBridge`. A clean event produces a
Reviewing session. Any finalization error discards staging without review.

`AndroidVideoReviewBridge` opens only that session's staged clip, reads the
container duration, decodes planned timestamps with MediaMetadataRetriever and
recycles each frame. `OpenNsfw2VideoFrameEvaluator` uses the source contextual
base sweep and the selected full spatial strategy on temporal escalation. The managed pipeline permits a promoter only after temporal Allow and
an exclusive Reviewing-to-Promoting transition. The promoter must bind its source
to that session and commit only approved output transactionally. Block, Review,
decode/runtime failure and cancellation do not promote. Decoder-open failures
request staging cleanup. Cleanup failures remain visible and block recording.
If approved output commits but staging cleanup fails, the pipeline returns
`PromotedCleanupPending`; retry cleanup without promoting that output again.

## Host responsibilities and sample

The host owns permission, CameraX lifecycle/executor, unique capture identities,
cancellation, approved destination and viewer. Share runtime access through its
synchronized API and close it after outstanding evaluations. Stop/close a live
recording, wait for finalization before cleanup and discard abandoned sessions
on next startup. Do not expose staging, thumbnails, providers or backup paths.

`sample-app` demonstrates camera permission, preview, in-memory rotated photos,
silent video with pause/resume and Back protection, cancellation/backgrounding,
cleanup retry and an approved-only private viewer. Mandatory first-run calibration
distinguishes advertised from verified usable configurations. It binds preview,
closing latest-only analysis and one capture use case at a time; High still has
an independent capture viewport. No account,
host application authorization, microphone, storage or network permission is used.

Verification includes synthetic patterns, simulated scores, an isolated emulator
scene and the bounded phone flow. See BUILD_NOTES for executed evidence. Spatial runtime and
sampled video are probabilistic; neither every-frame coverage nor accuracy,
parity or broad physical-device compatibility is established. One bounded
Samsung live-camera mechanics result is recorded in BUILD_NOTES. A modified host can bypass
an app-level SDK. External redistribution and delivery remain gated.

See [camera discovery/calibration and controls](CAMERA_TOOLING.md),
[approved library and explicit saving](REVIEWED_LIBRARY.md) and
[source inclusion/deferment map](TOOLING_PARITY.md) for standalone APIs.
