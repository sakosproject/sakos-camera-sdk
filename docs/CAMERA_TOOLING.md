# Standalone camera tooling

`capture-camerax` supplies source-derived discovery, graph binding, calibration
records/selection and secure profile storage. `capture-video` supplies actual
photo/video/zoom probes using the managed no-backup staging and bundled runtime.
No host application account, trust snapshot, provider authority or build value is needed.
See TOOLING_PARITY for source-derived behavior, included behavior and deliberate differences.

After CAMERA permission, obtain a ProcessCameraProvider and a mounted PreviewView.
Run calibration on a lifecycle coroutine on the main thread; provide a worker
executor for CameraX callbacks. Disable capture/graph controls during calibration.

```kotlin
val inventory = CameraCapabilityDiscovery.discover(provider)
val environment = CameraQualityCalibrationEnvironment.current(
    context, runtime.configuration.identity(),
    CameraCapabilityDiscovery.inventoryHash(inventory),
)
val storage = AndroidCameraCalibrationProfileStorage(context, environment)
val probe = AndroidCameraCalibrationProbe(
    context, lifecycleOwner, provider, previewView, workerExecutor, runtime,
) { outputRotation }
val runner = CameraCalibrationRunner(environment, storage, probe)
val profile = runner.run() // Reuses a matching ready profile; otherwise first-run probes.
check(profile.mandatoryReadiness(environment).ready)
val preset = requireNotNull(CameraGraphTooling.selectedPreset(
    profile, environment, CameraQualityCalibrationMode.Photo, "back",
    CameraCalibratedQualityTier.Default,
))
val graph = CameraGraphTooling.bind(
    provider, lifecycleOwner, previewView, "back", preset,
    outputRotation, workerExecutor,
)
```

Use `runner.run(retry = true)` after a recoverable failure. A matching ready cache
skips probes. The runner validates the returned lens/preset/mode/tier/purpose,
mandatory flag and gate coverage on both initial and full-temporal retry records,
and exact zoom stop identity/count. Malformed contracts cannot establish
readiness. Cleanup exceptions invalidate readiness and always release runner
ownership so explicit retry remains possible.

Schema/device/build/app/model/strategy/graph/step/inventory changes,
unreadable ciphertext or interrupted writes cannot create readiness. The SDK uses
AES-GCM with an app-local Android Keystore key, a no-backup directory and a synced
pending-file commit. Profiles contain measurements, never image/video inputs.
Calibration identity is stricter about app/build changes than the source app.

The source mandatory sequence probes back Default and High still, front Default
still, back/front Default video and back photo/video zoom stops 0.6/1/2/5. Low
photo is derived from verified Default and downscales to <=3MP. Failed required
Default/1x or unproven cleanup blocks readiness; failed non-default/unsupported
non-1x steps remain measured failures. Fast video probing retries once with full
temporal review when source conditions require it. A Block from a completed gate
is valid mechanical evidence; a runtime failure is not. Probes never save to the
approved library. Cancellation releases inputs and waits for recording terminal
events before cleanup; unresolved staging blocks recording and needs recovery.

Advertised flash, zoom and SDR video quality facts do not prove usability. High
must be distinct and >=110% Default pixels, have stable preview/gate evidence and
proven cleanup. Non-default front quality remains hidden. PhotoHighProbe and UHD
are diagnostics-only. `optionalDiagnostics = true` measures Low/FHD/UHD video
graphs but keeps those optional records out of product selectors. Developers may
use the probe/storage interfaces to explicitly measure a selector candidate;
they must preserve the record purpose, gate and cleanup evidence. Do not turn a
successful diagnostics bind into a selectable setting.

`CameraGraphTooling` preserves closing latest-only analysis, source FillCrop and
FitFourThree framing, independent High still viewport and exact CameraX quality
fallbacks. `CameraOutputRotation` maps source sensor quadrants; unknown input
leaves the caller's rotation intact. Flash preferences cycle Off/Auto/On; keep
the chosen preference while applying Off on unsupported lenses. Tap focus uses
AF/AE with a three-second auto-cancel. Verified zoom stops must also fit the
current CameraX range; selfie stays at 1x. `CameraVideoBackProtection` exposes
the source recording/finishing signal, and the CameraX handle supports pause,
resume and stop without replacing its staging session.

The sample demonstrates these controls, a ten-minute recording cap, lifecycle
cancellation, recovery and an approved-only viewer. Missing cameras or failed
defaults remain visible retry states. Synthetic emulator coverage establishes
mechanics for that emulated setup, not universal hardware compatibility.
