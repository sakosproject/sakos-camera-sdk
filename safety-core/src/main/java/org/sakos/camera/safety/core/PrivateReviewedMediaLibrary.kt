package org.sakos.camera.safety.core

import java.io.*
import java.security.MessageDigest
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class ReviewedMediaKind(val extension: String, val mimeType: String) { Photo("jpg", "image/jpeg"), Video("mp4", "video/mp4") }
data class ReviewedMediaEntry(val id: String, val kind: ReviewedMediaKind, val bytes: Long, val savedAtMillis: Long,
    val width: Int, val height: Int, val contentSha256: String, val configurationIdentity: String)

fun SafetyConfigurationVersion.identity(): String = "${model.id}@${model.version}|${preprocessing.id}@${preprocessing.version}|${policy.id}@${policy.version}"

/** Pending writes are invisible. Root must be host-owned app-private no-backup storage. No device gallery intake. */
class PrivateReviewedMediaLibrary(private val root: File, private val configuration: SafetyConfigurationVersion) {
    private val ownership = OWNERS.computeIfAbsent(root.canonicalPath) { RootOwnership() }
    init {
        check(root.isDirectory || root.mkdirs())
        synchronized(ownership) {
            // Reentrant adapter construction during a write must not recover it.
            // Another thread waits for the shared owner; another process is
            // rejected by the file lock instead of deleting live output.
            if (!ownership.writing) withRootLock {
                root.listFiles().orEmpty().filter { it.name.endsWith(".pending") }.forEach { pending ->
                    check(pending.canonicalFile.parentFile == root.canonicalFile)
                    check(pending.deleteRecursively()) { "Private pending output cleanup failed." }
                }
            }
        }
    }
    suspend fun save(kind: ReviewedMediaKind, capture: SafetyCaptureContext, approval: ManagedCaptureApproval,
        stillActive: () -> Boolean = { true }, write: (OutputStream) -> Unit): ReviewedMediaEntry {
        val operationContext = currentCoroutineContext()
        return ownership.mutex.withLock {
            synchronized(ownership) {
                withRootLock {
                    ownership.writing = true
                    try {
                        require(approval.isFor(capture) && approval.configuration == configuration)
                        operationContext.ensureActive(); check(stillActive())
                        val id = sha256(capture.captureId.value.toByteArray())
                        val target = File(root, id); require(!target.exists()) { "This capture has already been committed." }
                        val pending = File(root, "$id.pending"); check(pending.mkdir())
                        try {
                            val media = File(pending, "media.${kind.extension}")
                            FileOutputStream(media).use { output -> write(output); output.fd.sync() }
                            require(media.length() > 0)
                            val entry = ReviewedMediaEntry(id, kind, media.length(), System.currentTimeMillis(), capture.width, capture.height,
                                media.inputStream().use(::digest), configuration.identity())
                            val facts = Properties().apply {
                                setProperty("kind", kind.name); setProperty("bytes", entry.bytes.toString()); setProperty("savedAt", entry.savedAtMillis.toString())
                                setProperty("width", entry.width.toString()); setProperty("height", entry.height.toString())
                                setProperty("sha256", entry.contentSha256); setProperty("configuration", entry.configurationIdentity)
                            }
                            FileOutputStream(File(pending, "approval.properties")).use { facts.store(it, "SDK approved private output"); it.fd.sync() }
                            operationContext.ensureActive(); check(stillActive())
                            check(pending.renameTo(target)) { "Private output could not be committed." }
                            entry
                        } finally { if (pending.exists()) check(pending.deleteRecursively()) { "Private pending cleanup failed." } }
                    } finally { ownership.writing = false }
                }
            }
        }
    }

    private fun <T> withRootLock(block: () -> T): T = RandomAccessFile(File(root, LOCK_FILE), "rw").use { lockFile ->
        lockFile.channel.use { channel ->
            val lock = channel.tryLock() ?: error("Private library is owned by another process; retry after its operation completes.")
            lock.use { block() }
        }
    }

    fun items(): List<ReviewedMediaEntry> = root.listFiles().orEmpty().filter { it.isDirectory && ID.matches(it.name) }
        .mapNotNull { directory -> runCatching {
            require(directory.canonicalFile.parentFile == root.canonicalFile)
            val properties = Properties().apply { File(directory, "approval.properties").inputStream().use(::load) }
            val kind = ReviewedMediaKind.valueOf(properties.getProperty("kind"))
            val media = File(directory, "media.${kind.extension}")
            val entry = ReviewedMediaEntry(directory.name, kind, properties.getProperty("bytes").toLong(), properties.getProperty("savedAt").toLong(),
                properties.getProperty("width").toInt(), properties.getProperty("height").toInt(), properties.getProperty("sha256"), properties.getProperty("configuration"))
            require(entry.configurationIdentity == configuration.identity() && entry.bytes > 0 && entry.width > 0 && entry.height > 0)
            require(media.canonicalFile.parentFile == directory.canonicalFile && media.length() == entry.bytes)
            require(media.inputStream().use(::digest) == entry.contentSha256)
            entry
        }.getOrNull() }.sortedByDescending { it.savedAtMillis }

    /** Named approved item only; stale/tampered/uncommitted or path-traversal requests fail closed. */
    fun open(entry: ReviewedMediaEntry): InputStream {
        require(entry in items()) { "Item is not an approved member of this private library." }
        return File(File(root, entry.id), "media.${entry.kind.extension}").inputStream()
    }
    fun delete(entry: ReviewedMediaEntry): Boolean { require(entry in items()); return File(root, entry.id).deleteRecursively() }
    companion object {
        private const val LOCK_FILE = ".sakos-library.lock"
        private class RootOwnership { val mutex = Mutex(); var writing = false }
        private val OWNERS = ConcurrentHashMap<String, RootOwnership>()
        private val ID = Regex("[a-f0-9]{64}")
        private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        private fun digest(stream: InputStream): String {
            val digest = MessageDigest.getInstance("SHA-256"); val buffer = ByteArray(8192)
            while (true) { val count = stream.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}

/** Host authorization replaces source app-specific trust. Never implied by a classifier Allow. */
fun interface ReviewedExportAuthorization { suspend fun authorize(entry: ReviewedMediaEntry): Boolean }
interface ReviewedExportTransaction : AutoCloseable {
    fun output(): OutputStream
    fun commit()
    /** Removes pending destination on any failure or cancellation. close after commit retains committed output. */
    override fun close()
}
fun interface ReviewedExportDestination { fun begin(entry: ReviewedMediaEntry): ReviewedExportTransaction }
data class ReviewedExportResult(val requested: Int, val saved: Int, val denied: Int, val failed: Int,
    /** Subset of saved; committed output must not be exported again as a failed save. */
    val committedCleanupFailures: Int = 0, val committedEntryIds: List<String> = emptyList())

/** Cancellation preserves the exact committed IDs; callers must not retry those outputs. */
class ReviewedExportCancelledException(val partialResult: ReviewedExportResult,
    cause: CancellationException) : CancellationException("Authorized save cancelled; committed outputs remain saved.") {
    val committedEntryIds: List<String> get() = partialResult.committedEntryIds
    init { initCause(cause) }
}

class ReviewedMediaExporter(private val library: PrivateReviewedMediaLibrary) {
    suspend fun export(entries: List<ReviewedMediaEntry>, authorization: ReviewedExportAuthorization, destination: ReviewedExportDestination): ReviewedExportResult {
        var saved = 0; var denied = 0; var failed = 0; var committedCleanupFailures = 0
        val committedIds = mutableListOf<String>()
        try {
        for (entry in entries) {
            currentCoroutineContext().ensureActive()
            if (!authorization.authorize(entry)) { denied++; continue }
            var committed = false
            try {
                // Validate before creating any destination.
                library.open(entry).use { input -> destination.begin(entry).use { transaction ->
                    transaction.output().use { output ->
                        val buffer = ByteArray(8192)
                        while (true) { currentCoroutineContext().ensureActive(); val count = input.read(buffer); if (count < 0) break; output.write(buffer, 0, count) }
                    }
                    currentCoroutineContext().ensureActive(); transaction.commit(); committed = true; saved++; committedIds += entry.id
                } }
            } catch (cancelled: CancellationException) {
                if (committed) committedCleanupFailures++
                throw cancelled
            } catch (_: Exception) { if (committed) committedCleanupFailures++ else failed++ }
        }
        } catch (cancelled: CancellationException) {
            throw ReviewedExportCancelledException(ReviewedExportResult(entries.size, saved, denied, failed, committedCleanupFailures, committedIds.toList()), cancelled)
        }
        return ReviewedExportResult(entries.size, saved, denied, failed, committedCleanupFailures, committedIds.toList())
    }
}
