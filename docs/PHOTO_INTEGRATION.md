# Managed CameraX photo integration

`ManagedPhotoCaptureCallback` connects CameraX in-memory callbacks to
`ManagedPhotoReviewPipeline`. Supply a unique capture context, converter,
evaluator, approved sink and optional host coroutine context. The default
callback context does not own a host lifecycle.

The pipeline closes every proxy exactly once, including converter/evaluator
failures. It matches the capture ID and full model/preprocessing/policy
configuration before delivering an Allow to the sink. Block, Review, Failure,
mismatched results and cancellation do not deliver. It suppresses duplicate
capture IDs within the pipeline instance. Caller-owned converted Bitmaps must
also be recycled when review ends; output sinks must commit transactionally.

The sample demonstrates this path with the bundled Bitmap evaluator, rotated
CameraX input, cancellation tokens and private no-backup approved output. Camera
permission, executor, preview and lifecycle remain host responsibilities. No
public media provider, EXIF/location intake, network or rejected-image file is
created by the bridge. Synthetic tests exercise closure, failure, approval and
cancellation; physical orientation/front-camera verification remains pending.
