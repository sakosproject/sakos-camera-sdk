package org.sakos.camera.capture.camerax

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import org.sakos.camera.safety.core.*

/** Reads only named approved library members; private playback copies have explicit leases and restart cleanup. */
class AndroidReviewedMediaClient(context: Context, private val library: PrivateReviewedMediaLibrary) {
    private val playbackRoot = File(context.noBackupFilesDir, "sakos-approved-playback")
    init { check(playbackRoot.isDirectory || playbackRoot.mkdirs()); playbackRoot.listFiles().orEmpty().forEach { check(it.delete()) } }
    fun decodePreview(entry: ReviewedMediaEntry, maxDimension: Int = 1600): Bitmap? {
        require(maxDimension in 1..4096)
        if (entry.kind == ReviewedMediaKind.Video) return playback(entry).use { lease ->
            val retriever = MediaMetadataRetriever()
            try { retriever.setDataSource(lease.file.absolutePath); if (android.os.Build.VERSION.SDK_INT >= 27) retriever.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, maxDimension, maxDimension)
                else retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { frame ->
                    val factor = minOf(1.0, maxDimension.toDouble() / maxOf(frame.width, frame.height))
                    if (factor == 1.0) frame else Bitmap.createScaledBitmap(frame, (frame.width * factor).toInt().coerceAtLeast(1), (frame.height * factor).toInt().coerceAtLeast(1), true).also { frame.recycle() }
                } }
            finally { retriever.release() }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        library.open(entry).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxDimension) sample *= 2
        return library.open(entry).use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
    }
    fun playback(entry: ReviewedMediaEntry): ApprovedPlaybackLease {
        require(entry.kind == ReviewedMediaKind.Video)
        val file = File(playbackRoot, "${UUID.randomUUID()}.mp4")
        try {
            library.open(entry).use { input -> FileOutputStream(file).use { output -> input.copyTo(output); output.fd.sync() } }
            return ApprovedPlaybackLease(file)
        } catch (error: Exception) { check(!file.exists() || file.delete()); throw error }
    }
}
class ApprovedPlaybackLease internal constructor(internal val file: File) : AutoCloseable {
    val uri: Uri get() = Uri.fromFile(file)
    override fun close() { check(!file.exists() || file.delete()) { "Private playback cleanup failed." } }
}
