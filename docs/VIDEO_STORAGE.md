# Video staging storage contract

`capture-video` owns a state machine for an application-provided private staging
store. The store must contain each clip and all session sidecars in app-private
storage, excluded from backups. It must not use MediaStore, a public export
directory, thumbnails, gallery scans, or backup-visible paths for staging.

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
purges every non-completed session and preserves completed records. The current
abstraction cannot guarantee forensic erasure, protect against a modified host,
or prove behavior of a future Android file-store adapter. The current managed
pipeline supplies an injected promotion seam and a narrow CameraX `Recording`
stop/close adapter; it does not start a recorder, assign a file path, or prove
file descriptors, backup configuration, promotion atomicity, or process-death
behavior. Those remain device/runtime validation gates.
