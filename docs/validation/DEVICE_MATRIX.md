# Device and lifecycle validation matrix

Status: **physical verification pending**. The bundled runtime and functional
CameraX/private-storage sample exist. Current synthetic emulator checks are
recorded in BUILD_NOTES; they never close the physical-camera gate.

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
   source checkout as read-only.
3. Inspect app-private staging and public provider/gallery/backup visibility
   after every terminal outcome; record only aggregate/sanitized observations.
4. For a failure, stop dependent validation, keep the row failed or pending,
   add the minimum scoped regression test/fix, and rerun the affected rows.

This matrix is a runbook, not evidence that any scenario has passed.
