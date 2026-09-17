# Video staging storage contract

`capture-video` owns a state machine for an app-private staging store. Production
callers can use `AndroidVideoPrivateStagingStore(context)`, which roots session
content below `Context.noBackupFilesDir/sakos-camera-video-staging`. Each session
has a private content directory for its clip and sidecars plus durable metadata;
the store rediscovers missing or malformed metadata as a cleanup-blocking session.
It must not use MediaStore, a public export directory, thumbnails, gallery scans,
or backup-visible paths for staging.

The state sequence is `Recording` → `Reviewing` → `Promoting` → `Completed`.
`Completed` means the caller has already promoted an approved output and the
manager has deleted session-owned staging content. The manager deliberately
does not model an output URI because the approved-output owner is connected in
Phase 7B.

For rejection, review, failure, cancellation, abandonment, or an interrupted
promotion, call `discard`. It deletes the staged clip, every sidecar, and its
session metadata. Cleanup is idempotent: a missing session is treated as
removed. A delete or metadata error records `CleanupFailed`, and new recording
attempts return `BlockedByCleanup` until `retryCleanup` succeeds.

On startup, call `recoverAbandonedSessions` before accepting a recording. It
purges every non-completed session and preserves completed records. A malformed
or orphaned private session is surfaced as `CleanupFailed`, which blocks new
recordings until `retryCleanup` removes its content and metadata.

`CameraXPrivateVideoRecordingFactory` creates `FileOutputOptions` only from a
`Recording` session in a `VideoPrivateStagingFileStore` and can start a
caller-supplied CameraX `PendingRecording`. It neither enables audio nor maps
media to a public destination. The managed pipeline still owns an injected
decoder/evaluator and approved-output promoter. A real CameraX finalization
callback, file-descriptor behavior, backup behavior on a device, promotion
atomicity, model review and process-death behavior remain unverified runtime or
device gates. This store does not guarantee forensic erasure or protect against
a modified host.
