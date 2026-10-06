# Device and lifecycle validation matrix

Status: **bounded Samsung live-camera and synthetic verification passed; broader physical/API coverage pending**.
The bundled runtime and functional CameraX/private-storage sample exist.
A Samsung SM-G781W on Android 13/API 33 passed the focused synthetic checks
below. Emulator and generated-media checks never close the live-camera gate.

Record only sanitized evidence. Each executed row needs device model, Android
API level, SDK commit, model/preprocessing/policy identity, test build hash,
operator date, observed result, relevant log reference, and aggregate cleanup
state. Do not copy media, identifiers, raw paths, or
credentials into this file.

| Area | Scenario | Required observation | Status |
| --- | --- | --- | --- |
| Install | Fresh install in airplane mode, no host application | App works without account/runtime download/network permission | Pending |
| API range | API 26 device and current supported Android device | Startup and supported managed paths work | Pending |
| Camera | Front/back camera and rotation changes | Correct orientation/geometry and no wrong-capture promotion | Pending |
| Camera | Repeated photo/video captures | No resource leak, stale result, duplicate promotion, or unexpected memory growth | Pending |
| Model | Missing/corrupt model and invalid/non-finite result | Fail closed; no approved output | Pending |
| Model | Inference failure/cancellation/backgrounding | No late Allow; owned inputs/handles release | Pending |
| Permission | Camera denial and revocation (sample has no audio) | Useful recovery state; no capture begins without permission | Pending |
| Storage | Low storage and deletion/metadata failure | Cleanup failure is visible and blocks new recording | Pending |
| Video | Kill during recording/review/promotion, then relaunch | Recovery occurs before new recording; no unreviewed exposure | Pending |
| Exposure | Staging, sidecars, thumbnails, gallery/provider and backup inspection | Staging stays private and excluded from backups/public surfaces | Pending |
| Promotion | Approved output and interrupted promotion | Only Allow reaches approved output; no duplicate delivery | Pending |
| Performance | Photo/video latency and thermal/repeated-use behavior | Record distribution, device/runtime identity, and limitations | Pending |

## Reproducible execution outline

1. Build the exact SDK commit and install the sample/host app on an authorized
   physical device. Record the build and configuration identities.
2. Exercise one row at a time with only authorized safe inputs. Preserve the
   historical source material as read-only.
3. Inspect app-private staging and public provider/gallery/backup visibility
   after every terminal outcome; record only aggregate/sanitized observations.
4. For a failure, stop dependent validation, keep the row failed or pending,
   add the minimum scoped regression test/fix, and rerun the affected rows.

This matrix is a runbook, not evidence that any scenario has passed.

## Samsung SM-G781W focused synthetic checks - 2026-10-02

The existing private candidate was tested on one authorized ARM64 physical
device using only generated geometric/solid bitmaps, locally encoded solid
YUV patterns and simulated approval/fake export destinations. No live camera
was opened and no camera permission was granted. The six test/sample/consumer
packages were absent before the run, installed normally, and removed after
testing. Personal media and preexisting app data were not accessed.

| Focused check | Result | Scope |
| --- | --- | --- |
| Normal fresh APK installation | Passed after packaging correction | Six owned packages; no Play Protect bypass/settings change |
| Bundled runtime | 2 passed | ARM64 Fixed14/Adaptive14 execution, tensor/asset contract, configuration-bound receipts and closed-evaluator rejection |
| Private store and calibration profile | 2 passed | Synthetic approval mismatch, pending-write recovery, Android Keystore encrypted profile identity/interruption handling |
| Video and reviewed-library mechanics | 5 passed | Generated AVC decoding/review/promotion, missing/malformed-input rejection, private preview/playback leases, shared ownership and cleanup retries |
| Exact minified Maven consumer | 1 passed | Both strategies, calibration serialization, synthetic private save/preview and host-authorized fake export after R8 |

These are **10 distinct tests**. The first video attempt failed three tests
before SDK review because the device AVC encoder rejects the old 64x64 fixture.
The selected Qualcomm encoder advertises width and height ranges starting at
128 and explicitly reports 64x64 unsupported. A capability-selected 320x240
synthetic fixture passed the complete five-test suite after the test-only fix.

Camera service metadata advertises four normal/API1-public cameras and eleven
HAL entries including vendor aliases. Front/rear orientation, focal lengths,
flash availability and zoom ranges were inventoried without opening a camera.
Advertised characteristics do not establish SDK-selected IDs, usable graphs,
0.6x behavior or calibration readiness.

The matrix above stays pending for live calibration, controls, photo/video
capture, orientation/lifecycle, actual public export, repeated-use/thermal,
exposure/backup, low storage and broad API/OEM coverage. First-run live probes
require explicit confirmation that both lenses face benign blank surfaces and
authorization for temporary preview/photo/video capture. No phone-wide
compatibility, live-media parity or classifier efficacy claim is made.

The APK/build hashes and sanitized logs are retained privately in the device
handoff; source/build details are recorded in BUILD_NOTES.

## Authorized office-view hardware pass - 2026-10-02

Explicit operator approval authorized the same Samsung's temporary live preview,
photo and video capture of office floor/ceiling views. One dedicated opt-in flow
passed in 51.774 seconds; it did not rerun the earlier ten synthetic tests.

| Function | Observed result |
| --- | --- |
| Initial camera permission absent, then authorized grant | Capture disabled before permission; ready after grant |
| First-run front/rear photo/video calibration | Ready; mandatory probes gated and discarded; no approved output from probes |
| Rear Normal/Low | 3024x4032 / 1500x2000; approved private photo save and viewer |
| Rear High eligibility | Probe verified but equal to Normal resolution; hidden correctly |
| Default front | 2448x3264; private photo/video review/save; selfie zoom fixed 1x; no hardware flash |
| Rear zoom | Photo/video 1/2/5x verified; 0.6x unsupported on selected graph |
| Rear flash and tap | Auto/On/Off and On-flash capture; AF/AE tap submitted (sharpness not asserted) |
| Live video | Front/rear review and private save; back pause/resume and back guard |
| Recreation/cache/landscape activity | Ready encrypted cache reused without new probes; front selection retained; capture worked |
| Explicit and background cancellation | Returned ready; no late promotion or unresolved staging |
| Cleanup | Five owned photos/two videos deleted; two owned packages removed; used camera clients closed |

Phone orientation was not physically changed; activity-orientation/recreation
coverage does not close the physical rotation row. Public export was outside
this private-capture authorization and was not attempted. Additional camera
IDs were inventoried, not individually routed/captured. Long-run resource,
thermal/performance, actual backup/public exposure, low-storage, forced-kill,
API-range/OEM and efficacy rows remain pending. No personal gallery was browsed
and no capture bytes were transferred off-device. This scoped success does not
claim phone-wide compatibility or content-detection accuracy.
