package org.sakos.camera.sample

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import org.sakos.camera.safety.core.ManagedCaptureApproval
import org.sakos.camera.safety.core.SafetyCaptureContext
import org.sakos.camera.safety.opennsfw2.OpenNsfw2ModelPreflight

/** Sample-owned output, separate from staging; pending writes are never viewer items. */
internal class ApprovedMediaStore(context: Context) {
    private val root = File(context.noBackupFilesDir, "sakos-approved-media")
    init {
        check(root.isDirectory || root.mkdirs())
        root.listFiles()?.filter { it.name.endsWith(".pending") }?.forEach { check(it.delete()) }
    }
    fun items(): List<File> = root.listFiles().orEmpty()
        .filter { it.isFile && (it.extension == "jpg" || it.extension == "mp4") }.sortedByDescending { it.lastModified() }

    fun savePhoto(bitmap: Bitmap, capture: SafetyCaptureContext, approval: ManagedCaptureApproval) {
        require(approval.isFor(capture) && approval.configuration == OpenNsfw2ModelPreflight.configuration)
        write("jpg") { stream -> check(bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) }
    }

    fun saveVideo(staged: File) {
        require(staged.isFile && staged.length() > 0)
        write("mp4") { stream -> staged.inputStream().use { it.copyTo(stream) } }
    }

    private fun write(extension: String, action: (FileOutputStream) -> Unit) {
        val target = File(root, "${UUID.randomUUID()}.$extension")
        val pending = File(root, "${target.name}.pending")
        try {
            FileOutputStream(pending).use { action(it); it.fd.sync() }
            check(pending.renameTo(target)) { "Approved output could not be committed." }
        } finally { if (pending.exists()) check(pending.delete()) }
    }
}
