package org.sakos.camera.capture.camerax

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.OutputStream
import org.sakos.camera.safety.core.*

/** Standalone approved library: no provider authority, host application identity or public-storage permission. */
class AndroidReviewedMediaLibrary(context: Context, configuration: SafetyConfigurationVersion) {
    val library = PrivateReviewedMediaLibrary(File(context.noBackupFilesDir, "sakos-reviewed-library"), configuration)
    suspend fun savePhoto(bitmap: Bitmap, capture: SafetyCaptureContext, approval: ManagedCaptureApproval,
        stillActive: () -> Boolean = { true }): ReviewedMediaEntry {
        require(!bitmap.isRecycled && bitmap.width == capture.width && bitmap.height == capture.height)
        return library.save(ReviewedMediaKind.Photo, capture, approval, stillActive) { output ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)) { "JPEG compression failed." }
        }
    }
}

/** Optional explicit authorized-save target. No automatic export, no external inventory reading. API 29+. */
class AndroidMediaStoreReviewedDestination(context: Context, private val album: String) : ReviewedExportDestination {
    private val resolver = context.applicationContext.contentResolver
    init { require(Regex("[A-Za-z0-9 _-]{1,64}").matches(album)) }
    override fun begin(entry: ReviewedMediaEntry): ReviewedExportTransaction {
        check(Build.VERSION.SDK_INT >= 29) { "Scoped MediaStore export requires API 29+." }
        val photo = entry.kind == ReviewedMediaKind.Photo
        val collection = if (photo) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "${entry.id}.${entry.kind.extension}")
            put(MediaStore.MediaColumns.MIME_TYPE, entry.kind.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${if (photo) Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_MOVIES}/$album")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = checkNotNull(resolver.insert(collection, values)) { "Destination unavailable." }
        return object : ReviewedExportTransaction {
            private var committed = false
            override fun output(): OutputStream = checkNotNull(resolver.openOutputStream(uri, "w"))
            override fun commit() {
                check(resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null) == 1)
                committed = true
            }
            override fun close() { if (!committed) check(resolver.delete(uri, null, null) == 1) { "Pending export cleanup failed." } }
        }
    }
}
