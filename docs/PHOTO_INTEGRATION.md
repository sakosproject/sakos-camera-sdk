# Managed CameraX photo callback

`ManagedPhotoCaptureCallback` is the in-memory CameraX boundary for a single
capture. Construct it with a `ManagedPhotoReviewPipeline`, capture context,
`ImageProxy` converter, and result listener; then provide it to
`ManagedPhotoCaptureLauncher(imageCapture).capture(executor, callback)`.

On a CameraX success callback, the bridge invokes `reviewImageProxy`. That
pipeline owns exactly-once proxy closure and sends input to the approved sink
only when the evaluator returns a capture-bound Allow receipt. CameraX capture
errors are reported as `CaptureFailure`; converter/evaluator/sink failures are
reported as `PipelineFailure` or the corresponding review result.

The bridge does not choose a camera, request permission, enable network access,
serialize a frame, write a file, add EXIF/location data, create a thumbnail, or
provide a real evaluator. It must be wired to a host-owned camera lifecycle and
an injected evaluator/output sink. Model inference, physical orientation/front
camera behavior, cancellation/lifecycle behavior and device validation remain
pending gates.
