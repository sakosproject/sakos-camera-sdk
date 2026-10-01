/* Source-derived code only; no diagnostics export or input intake. */
package org.sakos.camera.capture.camerax

import android.media.MediaMetadataRetriever
import androidx.camera.core.ResolutionInfo
import androidx.camera.video.Quality
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

public data class CameraDiagnosticVideoFileFacts(
    val width: Int?,
    val height: Int?,
    val rotationDegrees: Int?,
    val metadataDurationMillis: Long?,
)

public fun cameraDiagnosticUseCaseLine(
    label: String,
    resolutionInfo: ResolutionInfo?,
): String {
    val resolution = resolutionInfo?.resolution
        ?: return "$label: not bound"
    val crop = resolutionInfo.cropRect
    val cropLabel = if (crop == null || crop.width() == resolution.width && crop.height() == resolution.height) {
        "full"
    } else {
        cameraDiagnosticDimensionsLabel(crop.width(), crop.height())
    }
    return "$label: ${cameraDiagnosticResolutionLabel(resolution.width, resolution.height)}, crop $cropLabel, rotation ${resolutionInfo.rotationDegrees}"
}

public fun cameraDiagnosticDisplayLine(
    label: String,
    width: Int,
    height: Int,
): String = "$label: ${cameraDiagnosticResolutionLabel(width, height)}"

public fun cameraDiagnosticResolutionLabel(
    width: Int?,
    height: Int?,
): String {
    if (width == null || height == null || width <= 0 || height <= 0) {
        return "unavailable"
    }
    return "${cameraDiagnosticDimensionsLabel(width, height)} (${cameraDiagnosticMegapixelLabel(width, height)}, ${cameraDiagnosticAspectRatioLabel(width, height)})"
}

public fun cameraDiagnosticDimensionsLabel(
    width: Int,
    height: Int,
): String = "${width}x$height"

public fun cameraDiagnosticMegapixelLabel(
    width: Int,
    height: Int,
): String {
    if (width <= 0 || height <= 0) {
        return "unknown MP"
    }
    val megapixels = width.toDouble() * height.toDouble() / 1_000_000.0
    return if (megapixels >= 10.0) {
        "${megapixels.roundToInt()} MP"
    } else {
        "${String.format(Locale.US, "%.1f", megapixels)} MP"
    }
}

public fun cameraDiagnosticAspectRatioLabel(
    width: Int,
    height: Int,
): String {
    if (width <= 0 || height <= 0) {
        return "unknown ratio"
    }
    val divisor = greatestCommonDivisor(abs(width), abs(height))
    return "${width / divisor}:${height / divisor}"
}

public fun cameraDiagnosticVideoQualityLabel(quality: Quality): String = when (quality) {
    Quality.UHD -> "UHD/4K"
    Quality.FHD -> "FHD/1080p"
    Quality.HD -> "HD/720p"
    Quality.SD -> "SD/480p"
    Quality.HIGHEST -> "Highest"
    Quality.LOWEST -> "Lowest"
    else -> quality.toString()
}

public fun cameraDiagnosticVideoTierLabel(
    width: Int?,
    height: Int?,
): String {
    if (width == null || height == null || width <= 0 || height <= 0) {
        return "unknown video quality"
    }
    val longEdge = maxOf(width, height)
    val shortEdge = minOf(width, height)
    return when {
        longEdge >= 3840 || shortEdge >= 2160 -> "UHD/4K actual"
        longEdge >= 1920 || shortEdge >= 1080 -> "FHD/1080p actual"
        longEdge >= 1280 || shortEdge >= 720 -> "HD/720p actual"
        longEdge >= 640 || shortEdge >= 480 -> "SD/480p actual"
        else -> "low-res actual"
    }
}

public fun readCameraDiagnosticVideoFileFacts(videoFile: File): CameraDiagnosticVideoFileFacts? = runCatching {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(videoFile.absolutePath)
        CameraDiagnosticVideoFileFacts(
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull(),
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull(),
            rotationDegrees = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull(),
            metadataDurationMillis = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
        )
    } finally {
        retriever.release()
    }
}.getOrNull()

public fun cameraDiagnosticVideoFileSummary(
    label: String,
    durationMillis: Long,
    fileSizeBytes: Long,
    facts: CameraDiagnosticVideoFileFacts?,
): String {
    val duration = facts?.metadataDurationMillis?.takeIf { it > 0L } ?: durationMillis
    val dimensions = if (facts?.width != null && facts.height != null) {
        "${cameraDiagnosticResolutionLabel(facts.width, facts.height)}, ${cameraDiagnosticVideoTierLabel(facts.width, facts.height)}"
    } else {
        "actual dimensions unavailable"
    }
    val rotation = facts?.rotationDegrees?.let { ", rotation $it" } ?: ""
    return "$label: $dimensions$rotation, duration ${cameraDiagnosticDurationLabel(duration)}, size ${cameraDiagnosticByteCountLabel(fileSizeBytes)}"
}

public fun cameraDiagnosticDurationLabel(durationMillis: Long): String {
    val totalSeconds = (durationMillis.coerceAtLeast(0L) + 500L) / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

public fun cameraDiagnosticByteCountLabel(byteCount: Long): String {
    val safeByteCount = byteCount.coerceAtLeast(0L)
    val units = listOf("B", "KB", "MB", "GB")
    var unitIndex = 0
    var value = safeByteCount.toDouble()
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex += 1
    }
    return if (unitIndex == 0) {
        "${safeByteCount} ${units[unitIndex]}"
    } else {
        "${String.format(Locale.US, "%.1f", value)} ${units[unitIndex]}"
    }
}

public fun cameraDiagnosticSafeExportToken(rawToken: String?): String {
    val normalized = rawToken
        ?.trim()
        ?.lowercase(Locale.US)
        ?.replace(Regex("[^a-z0-9._-]+"), "-")
        ?.trim('-', '_', '.')
        ?.take(48)
        ?.trim('-', '_', '.')
    return normalized?.takeIf { it.isNotEmpty() } ?: "snapshot"
}

private fun greatestCommonDivisor(
    first: Int,
    second: Int,
): Int {
    var a = first
    var b = second
    while (b != 0) {
        val next = a % b
        a = b
        b = next
    }
    return a.coerceAtLeast(1)
}
