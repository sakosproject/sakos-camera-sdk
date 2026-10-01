# Standalone reviewed library and authorized saving

Source camera repositories and gallery contracts are mapped in TOOLING_PARITY.
The SDK replaces their fixed cross-app authority and host application integration with
same-process interfaces. It installs no exported media provider, scans no device
gallery and performs no automatic external saving.

`PrivateReviewedMediaLibrary` accepts only a capture/configuration-matching
`ManagedCaptureApproval`. `AndroidReviewedMediaLibrary` places it under app-private
no-backup storage and supplies a JPEG photo adapter. Pending media and approval
metadata commit as one directory rename after sync and final cancellation/host
guard checks. Interrupted pending entries are removed on reopen; missing,
uncommitted, stale-configuration or digest-mismatched entries are unavailable.
Capture IDs prevent replay. The library's named-item open/delete APIs validate
membership and do not expose unapproved staging.

```kotlin
val approved = AndroidReviewedMediaLibrary(context, runtime.configuration)
val photoSink = object : ApprovedPhotoSink<Bitmap> {
    override suspend fun save(input: Bitmap, capture: SafetyCaptureContext,
        approval: ManagedCaptureApproval) {
        approved.savePhoto(input, capture, approval) { captureStillActive }
    }
}
// Use this sink with ManagedPhotoReviewPipeline and the same runtime.configuration.

val result = AndroidVideoReviewBridge(stagingStore, videoPipeline).reviewInto(
    reviewingSession, runtime, approved.library, frontFacing,
) { captureStillActive }
```

`reviewInto` binds the temporal Allow to the durable session and runtime strategy,
checks staged byte identity during copy and delivers only after the exclusive
promotion transition. Rejection/cancellation/failure requests cleanup. A committed
output with failed staging cleanup returns `PromotedCleanupPending`; retry only
cleanup. Legacy caller-owned promoter APIs remain available; those callers must
enforce equivalent source/destination binding themselves.

`AndroidReviewedMediaClient` decodes bounded previews from named approved items.
Video playback uses a private no-backup copy with an `ApprovedPlaybackLease` that
must close on viewer dismissal/backgrounding. Startup removes abandoned playback
copies. `ReviewedLibraryController` accepts a host coroutine scope/repository,
retains prior inventory on recoverable load failure and rejects late responses
after a newer refresh or close. Gallery UI, cross-app migration and recycle-bin
UI are host integrations; explicit permanent private deletion is provided.

For an explicit save action, inject `ReviewedExportAuthorization` and
`ReviewedExportDestination` into `ReviewedMediaExporter`. Authorization is required
for each approved item and is separate from model Allow. Denial creates no
destination. Copy checks cancellation; failure/cancellation closes the transaction
to roll back pending output. Successful copy commits once. The optional
`AndroidMediaStoreReviewedDestination(context, album)` requires API 29+, uses
IS_PENDING and checks publish/delete results. Album is caller configuration,
without source package names or production values. API 26–28 callers can provide
their own authorized destination; no legacy public-storage permission is added.

Current tests exercise fake destinations, simulated Allow receipts, generated
JPEG/AVC patterns and private emulator playback. They do not export to a real
device gallery or establish classifier efficacy. A modified host may bypass SDK
contracts; these APIs are an ownership boundary, not an OS security sandbox.
Deletion is not a forensic-erasure guarantee.
