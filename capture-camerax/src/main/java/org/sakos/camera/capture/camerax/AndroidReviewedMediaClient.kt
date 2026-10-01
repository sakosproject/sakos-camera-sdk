package org.sakos.camera.capture.camerax

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.sakos.camera.safety.core.*

/** Reads only named approved library members; private playback copies have explicit leases and restart cleanup. */
class AndroidReviewedMediaClient(context: Context, private val library: PrivateReviewedMediaLibrary) {
    private val playbackRoot = File(context.noBackupFilesDir, "sakos-approved-playback").canonicalFile
    private val ownership = OWNERS.computeIfAbsent(playbackRoot.canonicalPath) { PlaybackOwnership() }
    init { check(playbackRoot.isDirectory || playbackRoot.mkdirs()); retryCleanup() }

    /** Retry true orphan cleanup. Other clients' live leases are retained. */
    fun retryCleanup() = synchronized(ownership) {
        acquireOwnership()
        try { playbackRoot.listFiles().orEmpty().filter { it.name != LOCK_FILE && it !in ownership.active }.forEach {
            check(it.canonicalFile.parentFile == playbackRoot.canonicalFile)
            check(it.delete()) { "Private playback cleanup failed." }
        } } finally { releaseIfIdle() }
    }

    private fun acquireOwnership() {
        if (ownership.channel?.isOpen == true && ownership.lock?.isValid == true) return
        releaseIfIdle()
        val channel = RandomAccessFile(File(playbackRoot, LOCK_FILE), "rw").channel
        try {
            ownership.lock = checkNotNull(channel.tryLock()) { "Private playback is owned by another process; retry later." }
            ownership.channel = channel
        } catch (failure: Exception) { channel.close(); throw failure }
    }
    private fun releaseIfIdle() {
        if (ownership.active.isNotEmpty()) return
        ownership.lock?.release(); ownership.channel?.close()
        ownership.lock = null; ownership.channel = null
    }
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
    fun playback(entry: ReviewedMediaEntry): ApprovedPlaybackLease = synchronized(ownership) {
        require(entry.kind == ReviewedMediaKind.Video)
        acquireOwnership()
        val file = File(playbackRoot, "${UUID.randomUUID()}.mp4")
        try {
            library.open(entry).use { input -> FileOutputStream(file).use { output -> input.copyTo(output); output.fd.sync() } }
            ownership.active += file
            ApprovedPlaybackLease(file) { synchronized(ownership) {
                check(!file.exists() || file.delete()) { "Private playback cleanup failed; close can be retried." }
                ownership.active -= file
                releaseIfIdle()
            } }
        } catch (error: Exception) { check(!file.exists() || file.delete()); throw error }
        finally { releaseIfIdle() }
    }
    companion object {
        private const val LOCK_FILE = ".sakos-playback.lock"
        private class PlaybackOwnership {
            val active = mutableSetOf<File>(); var channel: FileChannel? = null; var lock: FileLock? = null
        }
        private val OWNERS = ConcurrentHashMap<String, PlaybackOwnership>()
    }
}
class ApprovedPlaybackLease internal constructor(internal val file: File, private val cleanup: () -> Unit) : AutoCloseable {
    val uri: Uri get() = Uri.fromFile(file)
    override fun close() = cleanup()
}
