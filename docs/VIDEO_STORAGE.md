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
manager has deleted session-owned staging content. The manager does not model
an output URI; the host owns the approved destination.

For rejection, review, failure, cancellation, abandonment, or an interrupted
promotion, call `discard`. It deletes the staged clip, every sidecar, and its
session metadata. Cleanup is idempotent: a missing session is treated as
removed. A delete or metadata error records `CleanupFailed`, and new recording
attempts return `BlockedByCleanup` until `retryCleanup` succeeds.
An existing non-completed session also blocks a second recording. Use one
manager/coordinator per store and do not recover sessions while live work owns them.

On startup, call `recoverAbandonedSessions` before accepting a recording. It
purges every non-completed session and preserves completed records. A malformed
or orphaned private session is surfaced as `CleanupFailed`, which blocks new
recordings until `retryCleanup` removes its content and metadata.

`CameraXPrivateVideoRecordingFactory` creates `FileOutputOptions` only from a
`Recording` session in a `VideoPrivateStagingFileStore` and can start a
caller-supplied CameraX `PendingRecording`. It neither enables audio nor maps
media to a public destination. The managed pipeline still owns an injected
decoder/evaluator and approved-output promoter. The Android review bridge now
decodes planned timestamps and applies the bundled Bitmap runtime. A CameraX finalization
bridge now maps a clean `VideoRecordEvent.Finalize` into a durable `Reviewing`
session and its recorded duration. Every CameraX finalization error discards
private staging before review or promotion; duplicate/out-of-order finalizations
are rejected. The host starts review with `reviewPrepared` using its injected
decoder/evaluator/promoter.

Synthetic JVM and isolated-emulator tests cover decoding, model mechanics,
recording/finalization, cancellation and staging recovery; see BUILD_NOTES.
Physical-camera/API-range behavior, low-storage fault coverage and complete
process-death promotion behavior remain gates. This store does not guarantee
forensic erasure or protect against a modified host.
