package org.sakos.camera.capture.video

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Base64
import java.util.Properties

/**
 * Durable app-private staging implementation. Production callers construct it from [Context],
 * which roots all session data below [Context.noBackupFilesDir]. The internal file constructor is
 * retained for deterministic JVM filesystem tests only.
 */
class AndroidVideoPrivateStagingStore private constructor(
    private val root: File,
) : VideoPrivateStagingFileStore {
    constructor(context: Context) : this(File(context.noBackupFilesDir, ROOT_DIRECTORY))

    internal constructor(testRoot: File, testOnly: Unit = Unit) : this(testRoot)

    override fun sessions(): List<VideoStagingSession> {
        ensureDirectories()
        val metadataIds = metadataDirectory.listFiles()
            ?.asSequence()
            ?.filter { it.isFile && (it.name.endsWith(METADATA_SUFFIX) || it.name.endsWith("$METADATA_SUFFIX.bak")) }
            ?.mapNotNull { file ->
                val name = file.name.removeSuffix(".bak").removeSuffix(METADATA_SUFFIX)
                parseSessionId(name)
            }
            ?.toSet()
            .orEmpty()
        val contentIds = contentDirectory.listFiles()
            ?.asSequence()
            ?.filter { it.isDirectory }
            ?.mapNotNull { parseSessionId(it.name) }
            ?.toSet()
            .orEmpty()

        return (metadataIds + contentIds)
            .sortedBy { it.value }
            .map { id -> readSessionOrFailure(id) }
    }

    override fun writeSession(session: VideoStagingSession) {
        ensureDirectories()
        val properties = Properties().apply {
            setProperty("id", session.id.value)
            setProperty("state", session.state.name)
            setProperty("createdAtEpochMillis", session.createdAtEpochMillis.toString())
            session.cleanupFailureDetail?.let { detail ->
                setProperty("cleanupFailureDetailBase64", Base64.getEncoder().encodeToString(detail.toByteArray(Charsets.UTF_8)))
            }
        }
        writePropertiesAtomically(metadataFile(session.id), properties)
    }

    override fun recordingOutputFile(id: VideoStagingSessionId): File {
        ensureDirectories()
        val directory = sessionDirectory(id)
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Could not create private staging directory for ${id.value}.")
        }
        return File(directory, RECORDING_FILE)
    }

    override fun deleteStagedContent(id: VideoStagingSessionId) {
        val directory = sessionDirectory(id)
        if (directory.exists() && !directory.deleteRecursively()) {
            throw IOException("Could not delete private staging content for ${id.value}.")
        }
    }

    override fun removeSession(id: VideoStagingSessionId) {
        deleteIfPresent(metadataFile(id))
        deleteIfPresent(backupMetadataFile(id))
        deleteIfPresent(temporaryMetadataFile(id))
    }

    private fun readSessionOrFailure(id: VideoStagingSessionId): VideoStagingSession {
        val source = when {
            metadataFile(id).isFile -> metadataFile(id)
            backupMetadataFile(id).isFile -> backupMetadataFile(id)
            else -> return cleanupFailure(id, "Missing staging metadata")
        }
        return try {
            val properties = Properties()
            FileInputStream(source).use(properties::load)
            require(properties.getProperty("id") == id.value)
            val state = VideoStagingState.valueOf(requireNotNull(properties.getProperty("state")))
            val createdAt = requireNotNull(properties.getProperty("createdAtEpochMillis")).toLong()
            val cleanupDetail = properties.getProperty("cleanupFailureDetailBase64")?.let { encoded ->
                String(Base64.getDecoder().decode(encoded), Charsets.UTF_8)
            }
            VideoStagingSession(id, state, createdAt, cleanupDetail)
        } catch (_: Exception) {
            cleanupFailure(id, "Unreadable staging metadata")
        }
    }

    private fun cleanupFailure(id: VideoStagingSessionId, detail: String) = VideoStagingSession(
        id = id,
        state = VideoStagingState.CleanupFailed,
        createdAtEpochMillis = 0L,
        cleanupFailureDetail = detail,
    )

    private fun writePropertiesAtomically(target: File, properties: Properties) {
        val temporary = temporaryMetadataFileFor(target)
        val backup = File(target.parentFile, "${target.name}.bak")
        FileOutputStream(temporary).use { output -> properties.store(output, null) }
        if (target.exists() && !target.renameTo(backup)) {
            temporary.delete()
            throw IOException("Could not preserve existing staging metadata for ${target.name}.")
        }
        if (!temporary.renameTo(target)) {
            if (backup.exists()) backup.renameTo(target)
            throw IOException("Could not write staging metadata for ${target.name}.")
        }
        backup.delete()
    }

    private fun ensureDirectories() {
        if (!root.exists() && !root.mkdirs()) throw IOException("Could not create private staging root.")
        if (!metadataDirectory.exists() && !metadataDirectory.mkdirs()) throw IOException("Could not create private metadata directory.")
        if (!contentDirectory.exists() && !contentDirectory.mkdirs()) throw IOException("Could not create private content directory.")
    }

    private fun sessionDirectory(id: VideoStagingSessionId): File = File(contentDirectory, id.value)
    private fun metadataFile(id: VideoStagingSessionId): File = File(metadataDirectory, "${id.value}$METADATA_SUFFIX")
    private fun backupMetadataFile(id: VideoStagingSessionId): File = File(metadataDirectory, "${id.value}$METADATA_SUFFIX.bak")
    private fun temporaryMetadataFile(id: VideoStagingSessionId): File = File(metadataDirectory, "${id.value}$METADATA_SUFFIX.tmp")
    private fun temporaryMetadataFileFor(target: File): File = File(target.parentFile, "${target.name}.tmp")
    private fun parseSessionId(value: String): VideoStagingSessionId? = runCatching { VideoStagingSessionId(value) }.getOrNull()
    private fun deleteIfPresent(file: File) {
        if (file.exists() && !file.delete()) throw IOException("Could not delete staging metadata ${file.name}.")
    }

    private val metadataDirectory: File get() = File(root, METADATA_DIRECTORY)
    private val contentDirectory: File get() = File(root, CONTENT_DIRECTORY)

    private companion object {
        const val ROOT_DIRECTORY = "sakos-camera-video-staging"
        const val METADATA_DIRECTORY = "metadata"
        const val CONTENT_DIRECTORY = "content"
        const val METADATA_SUFFIX = ".properties"
        const val RECORDING_FILE = "recording.mp4"
    }
}
