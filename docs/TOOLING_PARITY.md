# Source tooling and standalone boundary

Code-only inventory: source checkout HEAD `source revision omitted`.
Calibration, diagnostics, foundation and runtime match their pinned
`historical source revision omitted` versions after CRLF normalization.
Current CameraActivity is additionally referenced at HEAD. No source media is
part of extraction or tests. This is a behavior map, not an accuracy/parity claim.

All source paths below are relative to that checkout.

| Source code | Reusable behavior | SDK treatment |
| --- | --- | --- |
| `private host-app source file omitted` | v10 required defaults: back/front still and video, back High still, derived <=3MP Low, back photo/video zoom 0.6/1/2/5; readiness, selector evidence, fast retry, secure cache | Adapt source records/policy and AES-GCM Android Keystore profile; no-backup SDK directory; identity includes actual advertised inventory, model/strategy, build/app/graph/step versions; invalidated profile cannot be relabeled |
| `private host-app source file omitted` | Actual dimensions/crop/rotation/duration versus requested quality | Reusable diagnostics; no export or input intake |
| `private host-app source file omitted` and `CameraActivity.kt` | CameraX Preview/closing latest-only analysis/still or video; portrait viewport; independent High still; higher resolution preference; binding wait/timeout; gate/cleanup probes | Standalone graph binding and first-run runner; probe artifacts never approved library items; default failure blocks readiness and offers retry |
| `CameraActivity.kt`, `CameraStillFlashPreference.kt` (same camera directory) | Back/front, verified zoom stops (0.05 tolerance, max product 5), selfie 1x, tap AF/AE auto-cancel 3s, Off/Auto/On persisted choice, display/output orientation | SDK controls and minimal sample; preserve choices while unsupported modes use safe effective settings |
| `CameraQualityCalibration.kt` | PhotoHighProbe and UHD diagnostics; Low/FHD graph candidates do not automatically become product selectors; High must be >=110% default pixels and distinct; no non-default front selector | Preserve distinctions and source default sequence; optional diagnostics remain hidden from product selectors |
| `private source path omitted` | Fixed block short circuit and adaptive context/sentinel/portrait/refinement/targeted/fallback stages; exact exported thresholds | Pure live score driver and Bitmap runtime; Fixed14 retains default configuration; Adaptive14 has distinct policy identity; synthetic mechanical coverage only |
| `private host-app source file omitted`, `ReviewedVideoRepository.kt`, `TemporaryVideoCaptureRepository.kt` | Allowed-only private save, pending file/metadata commit, non-Allow disk cleanup, private approved inventory | Standalone approval-bound photo/video library and managed no-backup staging; atomic entry commit and restart recovery; no temporary or unapproved item becomes viewer/export input |
| `private host-app source file omitted`, `private host-app source file omitted` | Read-only approved inventory/preview/playback, bounded decode, private playback caches, named-item access | Same-process repository interfaces replace fixed cross-app authority/signature permission; caller chooses any provider integration; no exported provider installed by SDK |
| `private host-app source file omitted`, `GalleryViewModel.kt` | Cancellable inventory load, previous state on recoverable failure, generation guard against stale responses | Standalone library loader/controller with caller coroutine scope; no Application/host application/diagnostics dependency |
| `private host-app source file omitted` | Explicit user save, Android Q+ pending MediaStore insert/copy/publish; delete partial output on failure | Explicit caller authorization and transactional destination interface; optional configurable MediaStore adapter; sample never automatically exports |

App-specific integrations excluded: entitlement/trust/parent authorization, fixed
host application package names and provider authority, production credentials/build/signing,
telemetry and exported diagnostics, private input intake, source gallery fixtures,
location tracking. Host authorization replaces app trust through explicit injected
interfaces. Full gallery layout, editing and sharing UI, cross-app migration and
recycle-bin UI are deferred because this SDK supplies independent capture/review/
private save primitives, not those app screens. Capture and approval ownership,
cancellation, cleanup and default selector safeguards remain required.

Missing front/back or failed required default/1x probes do not establish readiness.
The SDK does not claim to make every advertised camera usable. Owners must test
physical devices, API ranges and real-world efficacy separately.
