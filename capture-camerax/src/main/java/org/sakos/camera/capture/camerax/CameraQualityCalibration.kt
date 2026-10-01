/* Adapted from allowlisted CameraQualityCalibration.kt, source 843d93f (unchanged at source revision omitted). */
package org.sakos.camera.capture.camerax

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Locale
import java.util.Properties
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Serializable
public enum class CameraQualityCalibrationMode(
    val token: String,
    val displayLabel: String,
) {
    Photo("photo", "Photo"),
    Video("video", "Video"),
}

@Serializable
public enum class CameraCalibratedQualityTier(
    val token: String,
) {
    Low("low"),
    Default("default"),
    High("high");

    fun labelFor(mode: CameraQualityCalibrationMode): String = when (this) {
        Low -> "Low"
        Default -> if (mode == CameraQualityCalibrationMode.Photo) "Normal" else "Medium"
        High -> "High"
    }

    companion object {
        fun fromDebugToken(rawToken: String?): CameraCalibratedQualityTier? =
            when (rawToken?.trim()?.lowercase(Locale.US)) {
                "low", "small", "sd" -> Low
                "normal", "medium", "med", "default", "auto", "automatic" -> Default
                "high", "large", "best", "fhd" -> High
                "uhd", "4k" -> High
                else -> null
            }
    }
}

@Serializable
public enum class CameraQualityGraphPreset(
    val token: String,
    val displayLabel: String,
    val mode: CameraQualityCalibrationMode,
    val tier: CameraCalibratedQualityTier,
    val selectorCandidate: Boolean,
    val includesLiveAnalysis: Boolean,
    val viewportMode: String,
    val useCases: String,
    val requestedCameraXQuality: String,
) {
    PhotoStable(
        token = "photo-stable",
        displayLabel = "PhotoStable",
        mode = CameraQualityCalibrationMode.Photo,
        tier = CameraCalibratedQualityTier.Default,
        selectorCandidate = true,
        includesLiveAnalysis = true,
        viewportMode = "portrait display-matched viewport",
        useCases = "Preview + ImageAnalysis + ImageCapture",
        requestedCameraXQuality = "automatic ImageCapture, minimize latency",
    ),
    PhotoLow(
        token = "photo-low",
        displayLabel = "PhotoLow",
        mode = CameraQualityCalibrationMode.Photo,
        tier = CameraCalibratedQualityTier.Low,
        selectorCandidate = true,
        includesLiveAnalysis = true,
        viewportMode = "portrait display-matched viewport",
        useCases = "Preview + ImageAnalysis + ImageCapture",
        requestedCameraXQuality = "automatic ImageCapture plus debug downscale to <=3MP",
    ),
    PhotoHighProbe(
        token = "photo-high-probe",
        displayLabel = "PhotoHighProbe",
        mode = CameraQualityCalibrationMode.Photo,
        tier = CameraCalibratedQualityTier.High,
        selectorCandidate = false,
        includesLiveAnalysis = false,
        viewportMode = "portrait display-matched viewport",
        useCases = "Preview + ImageCapture",
        requestedCameraXQuality = "legacy diagnostics-only ImageCapture maximize quality, portrait viewport",
    ),
    PhotoHighStillProbeV2(
        token = "photo-high-still-probe-v2",
        displayLabel = "PhotoHighStillProbeV2",
        mode = CameraQualityCalibrationMode.Photo,
        tier = CameraCalibratedQualityTier.High,
        selectorCandidate = true,
        includesLiveAnalysis = false,
        viewportMode = "portrait preview viewport; still ImageCapture bound outside viewport",
        useCases = "Preview in portrait UseCaseGroup + independent ImageCapture",
        requestedCameraXQuality = "ImageCapture maximize quality, higher resolution preferred, no forced portrait still viewport",
    ),
    VideoStable(
        token = "video-stable",
        displayLabel = "VideoStable",
        mode = CameraQualityCalibrationMode.Video,
        tier = CameraCalibratedQualityTier.Default,
        selectorCandidate = true,
        includesLiveAnalysis = true,
        viewportMode = "portrait display-matched viewport",
        useCases = "Preview + ImageAnalysis + VideoCapture",
        requestedCameraXQuality = "automatic CameraX Recorder",
    ),
    VideoLow(
        token = "video-low",
        displayLabel = "VideoLow",
        mode = CameraQualityCalibrationMode.Video,
        tier = CameraCalibratedQualityTier.Low,
        selectorCandidate = true,
        includesLiveAnalysis = false,
        viewportMode = "portrait display-matched viewport",
        useCases = "Preview + VideoCapture",
        requestedCameraXQuality = "CameraX Recorder QualitySelector target SD/480p",
    ),
    VideoHighFhdProbe(
        token = "video-high-fhd-probe",
        displayLabel = "VideoHighFhdProbe",
        mode = CameraQualityCalibrationMode.Video,
        tier = CameraCalibratedQualityTier.High,
        selectorCandidate = true,
        includesLiveAnalysis = false,
        viewportMode = "portrait display-matched viewport",
        useCases = "Preview + VideoCapture",
        requestedCameraXQuality = "CameraX Recorder QualitySelector target FHD/1080p",
    ),
    VideoUhdProbe(
        token = "video-uhd-probe",
        displayLabel = "VideoUhdProbe",
        mode = CameraQualityCalibrationMode.Video,
        tier = CameraCalibratedQualityTier.High,
        selectorCandidate = false,
        includesLiveAnalysis = false,
        viewportMode = "portrait display-matched viewport",
        useCases = "Preview + VideoCapture",
        requestedCameraXQuality = "CameraX Recorder QualitySelector target UHD/4K, diagnostics-only",
    );

    companion object {
        fun fromToken(rawToken: String?): CameraQualityGraphPreset? =
            rawToken
                ?.trim()
                ?.lowercase(Locale.US)
                ?.replace('_', '-')
                ?.let { token ->
                    entries.firstOrNull { preset ->
                        preset.token == token ||
                            preset.displayLabel.lowercase(Locale.US) == token.replace("-", "")
                    }
                }

        fun selectorPresetFor(
            mode: CameraQualityCalibrationMode,
            tier: CameraCalibratedQualityTier,
        ): CameraQualityGraphPreset = when (mode) {
            CameraQualityCalibrationMode.Photo -> when (tier) {
                CameraCalibratedQualityTier.Low -> PhotoLow
                CameraCalibratedQualityTier.Default -> PhotoStable
                CameraCalibratedQualityTier.High -> PhotoHighStillProbeV2
            }

            CameraQualityCalibrationMode.Video -> when (tier) {
                CameraCalibratedQualityTier.Low -> VideoLow
                CameraCalibratedQualityTier.Default -> VideoStable
                CameraCalibratedQualityTier.High -> VideoHighFhdProbe
            }
        }
    }
}

@Serializable
public enum class CameraQualityCalibrationStatus(
    val token: String,
    val displayLabel: String,
) {
    Verified("verified", "Verified"),
    Failed("failed", "Failed");

    companion object {
        fun fromToken(rawToken: String?): CameraQualityCalibrationStatus =
            entries.firstOrNull { status -> status.token == rawToken } ?: Failed
    }
}

@Serializable
public enum class CameraQualityCalibrationProbePurpose(
    val displayLabel: String,
) {
    RequiredDefault("required default"),
    RequiredSelectorCandidate("required selector candidate"),
    DerivedSelectorDecision("derived selector decision"),
    OptionalDiagnostics("optional diagnostics"),
}

@Serializable
public enum class CameraQualityCalibrationGateCoverage(
    val displayLabel: String,
) {
    FastSingleFrame("fast single-frame video gate"),
    FullTemporalFallback("full temporal fallback gate"),
    StillGate("full still safety gate"),
    MetadataOnly("metadata/profile only"),
    DerivedFromDefault("derived from default gate"),
}

@Serializable
public enum class CameraZoomCalibrationStatus(
    val displayLabel: String,
) {
    Verified("Verified"),
    Unsupported("Unsupported"),
    Failed("Failed"),
}

public const val CAMERA_QUALITY_CALIBRATION_SCHEMA_VERSION = 10
public const val CAMERA_QUALITY_CALIBRATION_LOW_PHOTO_MAX_PIXELS = 3_000_000L
public const val CAMERA_PRODUCT_MAX_ZOOM_RATIO = 5f

private const val cameraZoomCalibrationTolerance = 0.05f
private const val cameraQualityCalibrationSecureProfileFileName = "camera-quality-calibration-profile-v10.bin"
private val cameraQualityCalibrationObsoleteSecureProfileFileNames = listOf(
    "camera-quality-calibration-profile-v9.bin",
    "camera-quality-calibration-profile-v8.bin",
    "camera-quality-calibration-profile-v7.bin",
    "camera-quality-calibration-profile-v6.bin",
    "camera-quality-calibration-profile-v5.bin",
    "camera-quality-calibration-profile-v4.bin",
)
private const val cameraQualityCalibrationLegacyProfileFileName = "camera-quality-calibration-table.properties"
private const val cameraQualityCalibrationSecureKeyAlias = "sakos_camera_quality_calibration_v1"
private const val cameraQualityCalibrationGraphAssumption = "sakos-camera-quality-v1-source-v10-camerax-1.5.3"
private const val cameraQualityCalibrationPolicyHash = "sakos-policy-v1-ratio-aware-gate"

private val cameraQualityCalibrationJson = Json {
    encodeDefaults = true
    ignoreUnknownKeys = false
    prettyPrint = false
}

public data class CameraQualityCalibrationEnvironment(
    val schemaVersion: Int,
    val deviceKey: String,
    val packageName: String,
    val appVersionName: String?,
    val appVersionCode: Long?,
    val modelId: String,
    val policyHash: String,
    val cameraXGraphAssumption: String,
    val buildFingerprint: String,
    val requiredStepSetHash: String,
    val cameraInventoryHash: String,
) {
    companion object {
        fun current(context: Context, safetyIdentity: String, inventoryHash: String): CameraQualityCalibrationEnvironment =
            CameraQualityCalibrationEnvironment(
                schemaVersion = CAMERA_QUALITY_CALIBRATION_SCHEMA_VERSION,
                deviceKey = cameraQualityCalibrationDeviceKey(),
                packageName = context.packageName,
                appVersionName = context.packageManager
                    .getPackageInfoCompat(context.packageName)
                    ?.versionName,
                appVersionCode = context.packageManager
                    .getPackageInfoCompat(context.packageName)
                    ?.longVersionCodeCompat(),
                modelId = safetyIdentity,
                policyHash = cameraQualityCalibrationPolicyHash,
                cameraXGraphAssumption = cameraQualityCalibrationGraphAssumption,
                buildFingerprint = Build.FINGERPRINT.ifBlank { "unknown" },
                requiredStepSetHash = cameraQualityCalibrationRequiredStepSetHash(),
                cameraInventoryHash = inventoryHash,
            )

        fun test(
            deviceKey: String = "test-device",
            packageName: String = "org.sakos.camera.synthetic",
        ): CameraQualityCalibrationEnvironment =
            CameraQualityCalibrationEnvironment(
                schemaVersion = CAMERA_QUALITY_CALIBRATION_SCHEMA_VERSION,
                deviceKey = deviceKey,
                packageName = packageName,
                appVersionName = "test",
                appVersionCode = 1L,
                modelId = "opennsfw2_resnet50_v1",
                policyHash = cameraQualityCalibrationPolicyHash,
                cameraXGraphAssumption = cameraQualityCalibrationGraphAssumption,
                buildFingerprint = "test-fingerprint",
                requiredStepSetHash = cameraQualityCalibrationRequiredStepSetHash(),
                cameraInventoryHash = "test-inventory",
            )
    }
}

public data class CameraQualityCalibrationReadiness(
    val ready: Boolean,
    val reason: String,
    val completedStepCount: Int,
    val requiredStepCount: Int,
    val progressPercent: Int,
    val terminalFailure: Boolean,
)

@Serializable
public data class CameraQualitySelectorEligibility(
    val eligible: Boolean,
    val reason: String,
)

@Serializable
public data class CameraQualityCalibrationRecord(
    val mode: CameraQualityCalibrationMode,
    val lensLabel: String,
    val tier: CameraCalibratedQualityTier,
    val status: CameraQualityCalibrationStatus,
    val requestedPolicy: String,
    val advertisedVideoQualities: String?,
    val boundStreamLabel: String?,
    val finalWidth: Int?,
    val finalHeight: Int?,
    val finalRotationDegrees: Int?,
    val durationMillis: Long?,
    val fileSizeBytes: Long?,
    val failureReason: String?,
    val updatedAtMillis: Long,
    val graphPreset: CameraQualityGraphPreset = CameraQualityGraphPreset.selectorPresetFor(mode, tier),
    val useCasesBound: String? = graphPreset.useCases,
    val viewportMode: String? = graphPreset.viewportMode,
    val liveAnalysisIncluded: Boolean? = graphPreset.includesLiveAnalysis,
    val requestedCameraXQuality: String? = graphPreset.requestedCameraXQuality,
    val previewStreamLabel: String? = null,
    val previewStable: Boolean? = true,
    val gateDurationMillis: Long? = null,
    val gateStatusLabel: String? = null,
    val selectorEligible: Boolean? = null,
    val selectorRejectReason: String? = null,
    val bitrateBitsPerSecond: Long? = videoBitrateBitsPerSecond(fileSizeBytes, durationMillis),
    val gateSamplingProfileId: String? = null,
    val gateSamplingProfileLabel: String? = null,
    val gateSamplingRatio: Float? = null,
    val gateSamplingOrientation: String? = null,
    val gateSamplingProfileSelectorEligible: Boolean? = null,
    val gateSampledFrameCount: Int? = null,
    val gateEvaluatedFrameCount: Int? = null,
    val calibrationGateMode: String? = null,
    val calibrationGateRequiredForSelector: Boolean? = null,
    val calibrationArtifactCleanupSucceeded: Boolean? = null,
    val calibrationArtifactCleanupSummary: String? = null,
    val calibrationBatchStartedAtMillis: Long? = null,
    val calibrationStepIndex: Int? = null,
    val calibrationStepCount: Int? = null,
    val calibrationStepStartedAtMillis: Long? = null,
    val calibrationStepCompletedAtMillis: Long? = null,
    val calibrationStepDurationMillis: Long? = null,
    val calibrationBindWaitMillis: Long? = null,
    val calibrationCaptureOrRecordMillis: Long? = null,
    val calibrationRecordingTargetMillis: Long? = null,
    val probePurpose: CameraQualityCalibrationProbePurpose? = null,
    val gateCoverage: CameraQualityCalibrationGateCoverage? = null,
    val fastProbeRetried: Boolean? = null,
    val derivedFromRecordKey: String? = null,
    val mandatoryForReadiness: Boolean? = null,
    val cameraInventorySummary: String? = null,
) {
    val hasActualOutput: Boolean
        get() = finalWidth != null && finalHeight != null && finalWidth > 0 && finalHeight > 0

    fun actualOutputKey(): String? {
        val width = finalWidth ?: return null
        val height = finalHeight ?: return null
        if (width <= 0 || height <= 0) {
            return null
        }
        return "${minOf(width, height)}x${maxOf(width, height)}"
    }

    fun recordKey(): String =
        cameraQualityCalibrationRecordKey(
            mode = mode,
            lensToken = cameraQualityCalibrationLensToken(lensLabel),
            tier = tier,
            graphPreset = graphPreset,
        )
}

@Serializable
public data class CameraZoomCalibrationRecord(
    val mode: CameraQualityCalibrationMode,
    val lensLabel: String,
    val requestedZoomRatio: Float,
    val status: CameraZoomCalibrationStatus,
    val minZoomRatio: Float?,
    val maxZoomRatio: Float?,
    val appliedZoomRatio: Float?,
    val observedZoomRatio: Float?,
    val failureReason: String?,
    val updatedAtMillis: Long,
    val diagnosticsOnly: Boolean = false,
    val productEligible: Boolean? = null,
    val productRejectReason: String? = null,
    val calibrationBatchStartedAtMillis: Long? = null,
    val calibrationStepIndex: Int? = null,
    val calibrationStepCount: Int? = null,
    val calibrationStepStartedAtMillis: Long? = null,
    val calibrationStepCompletedAtMillis: Long? = null,
    val calibrationStepDurationMillis: Long? = null,
    val calibrationBindWaitMillis: Long? = null,
) {
    fun recordKey(): String =
        cameraZoomCalibrationRecordKey(
            mode = mode,
            lensToken = cameraQualityCalibrationLensToken(lensLabel),
            requestedZoomRatio = requestedZoomRatio,
        )
}

@Serializable
public data class CameraQualityCalibrationProfile(
    val deviceKey: String,
    val updatedAtMillis: Long?,
    val records: List<CameraQualityCalibrationRecord>,
    val zoomRecords: List<CameraZoomCalibrationRecord> = emptyList(),
    val schemaVersion: Int = CAMERA_QUALITY_CALIBRATION_SCHEMA_VERSION,
    val createdAtMillis: Long? = null,
    val appVersionName: String? = null,
    val appVersionCode: Long? = null,
    val modelId: String? = null,
    val policyHash: String? = null,
    val cameraXGraphAssumption: String? = null,
    val buildFingerprint: String? = null,
    val requiredStepSetHash: String? = null,
    val cameraInventoryHash: String? = null,
    val secureStoreStatus: String? = null,
    val calibrationStartedAtMillis: Long? = null,
    val calibrationCompletedAtMillis: Long? = null,
    val calibrationTotalDurationMillis: Long? = null,
) {
    fun recordsFor(
        mode: CameraQualityCalibrationMode,
        lensLabel: String,
        tier: CameraCalibratedQualityTier,
    ): List<CameraQualityCalibrationRecord> {
        val normalizedLens = cameraQualityCalibrationLensToken(lensLabel)
        return records.filter { record ->
            record.mode == mode &&
                cameraQualityCalibrationLensToken(record.lensLabel) == normalizedLens &&
                record.tier == tier
        }
    }

    fun recordFor(
        mode: CameraQualityCalibrationMode,
        lensLabel: String,
        tier: CameraCalibratedQualityTier,
        graphPreset: CameraQualityGraphPreset? = null,
    ): CameraQualityCalibrationRecord? {
        val candidates = recordsFor(mode, lensLabel, tier)
        if (graphPreset != null) {
            return candidates.firstOrNull { record -> record.graphPreset == graphPreset }
        }
        return candidates.firstOrNull { record -> record.graphPreset.selectorCandidate } ?: candidates.firstOrNull()
    }

    fun visibleTiersFor(
        mode: CameraQualityCalibrationMode,
        lensLabel: String,
    ): List<CameraCalibratedQualityTier> {
        val defaultRecord = recordFor(mode, lensLabel, CameraCalibratedQualityTier.Default)
        val visible = mutableListOf(CameraCalibratedQualityTier.Default)
        CameraCalibratedQualityTier.entries
            .filter { tier -> tier != CameraCalibratedQualityTier.Default }
            .forEach { tier ->
                val record = recordFor(mode, lensLabel, tier)
                if (record?.selectorEligibility(defaultRecord)?.eligible == true) {
                    visible += tier
                }
            }
        return visible
    }

    fun calibratedTierFor(
        mode: CameraQualityCalibrationMode,
        lensLabel: String,
        requestedTier: CameraCalibratedQualityTier,
    ): CameraCalibratedQualityTier {
        val visibleTiers = visibleTiersFor(mode, lensLabel)
        return if (requestedTier in visibleTiers) {
            requestedTier
        } else {
            CameraCalibratedQualityTier.Default
        }
    }

    fun childFacingVisibleTiersFor(
        mode: CameraQualityCalibrationMode,
        lensLabel: String,
    ): List<CameraCalibratedQualityTier> {
        if (mode == CameraQualityCalibrationMode.Video) {
            return emptyList()
        }
        if (cameraQualityCalibrationLensToken(lensLabel) != "back") {
            return emptyList()
        }
        val visibleTiers = visibleTiersFor(mode, lensLabel).toSet()
        return cameraQualityCalibrationChildFacingTierOrder.filter { tier -> tier in visibleTiers }
    }

    fun zoomRecordsFor(
        mode: CameraQualityCalibrationMode,
        lensLabel: String,
    ): List<CameraZoomCalibrationRecord> {
        val normalizedLens = cameraQualityCalibrationLensToken(lensLabel)
        return zoomRecords.filter { record ->
            record.mode == mode &&
                cameraQualityCalibrationLensToken(record.lensLabel) == normalizedLens
        }
    }

    fun zoomRecordFor(
        mode: CameraQualityCalibrationMode,
        lensLabel: String,
        requestedZoomRatio: Float,
    ): CameraZoomCalibrationRecord? =
        zoomRecordsFor(mode, lensLabel).firstOrNull { record ->
            cameraZoomRatiosEquivalent(record.requestedZoomRatio, requestedZoomRatio)
        }

    fun visibleZoomStopsFor(
        mode: CameraQualityCalibrationMode,
        lensLabel: String,
        currentMinZoomRatio: Float? = null,
        currentMaxZoomRatio: Float? = null,
    ): List<Float> {
        if (cameraQualityCalibrationLensToken(lensLabel) != "back") {
            return emptyList()
        }
        val productStops = cameraZoomCalibrationProductStops()
        val visibleStops = productStops.filter { stop ->
            val record = zoomRecordFor(mode, lensLabel, stop)
            record?.productEligibility()?.eligible == true &&
                cameraZoomRatioInRange(
                    zoomRatio = stop,
                    minZoomRatio = currentMinZoomRatio ?: record.minZoomRatio,
                    maxZoomRatio = currentMaxZoomRatio ?: record.maxZoomRatio,
                )
        }
        return visibleStops.ifEmpty {
            productStops.filter { stop ->
                cameraZoomRatiosEquivalent(stop, 1f) &&
                    cameraZoomRatioInRange(
                        zoomRatio = stop,
                        minZoomRatio = currentMinZoomRatio,
                        maxZoomRatio = currentMaxZoomRatio,
                    )
            }
        }
    }

    fun childFacingVisibleZoomStopsFor(
        mode: CameraQualityCalibrationMode,
        lensLabel: String,
        currentMinZoomRatio: Float? = null,
        currentMaxZoomRatio: Float? = null,
    ): List<Float> =
        visibleZoomStopsFor(
            mode = mode,
            lensLabel = lensLabel,
            currentMinZoomRatio = currentMinZoomRatio,
            currentMaxZoomRatio = currentMaxZoomRatio,
        )

    fun selectorDecisionLine(
        mode: CameraQualityCalibrationMode,
        lensLabel: String,
        requestedTier: CameraCalibratedQualityTier,
    ): String {
        val activeTier = calibratedTierFor(mode, lensLabel, requestedTier)
        val requestedLabel = requestedTier.labelFor(mode)
        val activeLabel = activeTier.labelFor(mode)
        return if (activeTier == requestedTier) {
            "Calibrated selector: requested $requestedLabel; active $activeLabel."
        } else {
            val rejectReason = recordFor(mode, lensLabel, requestedTier)
                ?.selectorEligibility(recordFor(mode, lensLabel, CameraCalibratedQualityTier.Default))
                ?.reason
                ?: "unmeasured"
            "Calibrated selector: requested $requestedLabel; active $activeLabel because the requested tier is $rejectReason."
        }
    }

    fun summaryLines(
        activeMode: CameraQualityCalibrationMode,
        activeLensLabel: String,
        selectedTier: CameraCalibratedQualityTier,
    ): List<String> = buildList {
        add("Scope: local device calibration profile from real bound/captured output.")
        add("Device key: $deviceKey")
        add("Secure profile: ${secureStoreStatus ?: "v$schemaVersion app-data encrypted profile"}")
        add("Required calibration records: ${records.map { record -> record.recordKey() }.toSet().intersect(cameraQualityCalibrationRequiredRecords.map { it.recordKey }.toSet()).size}/${cameraQualityCalibrationRequiredRecords.size}")
        calibrationTotalDurationMillis?.let { totalDuration ->
            add("Calibration timing: total ${cameraQualityCalibrationMillisLabel(totalDuration)}")
        }
        records.maxByOrNull { record -> record.calibrationStepDurationMillis ?: -1L }
            ?.takeIf { record -> (record.calibrationStepDurationMillis ?: 0L) > 0L }
            ?.let { record ->
                add(
                    "Slowest calibration step: ${record.graphPreset.displayLabel} ${record.lensLabel.lowercase(Locale.US)} ${record.mode.displayLabel.lowercase(Locale.US)} in ${cameraQualityCalibrationMillisLabel(record.calibrationStepDurationMillis ?: 0L)}",
                )
            }
        add("Selected debug tier: ${selectedTier.labelFor(activeMode)}")
        add(
            "Visible tiers for current lens: ${
                visibleTiersFor(activeMode, activeLensLabel)
                    .joinToString { tier -> tier.labelFor(activeMode) }
            }",
        )
        add(
            "Visible zoom stops for current lens: ${
                visibleZoomStopsFor(activeMode, activeLensLabel)
                    .joinToString { stop -> cameraZoomCalibrationRatioLabel(stop) }
                    .ifBlank { "none" }
            }",
        )
        if (records.isEmpty() && zoomRecords.isEmpty()) {
            add("No calibration observations recorded yet. Capture each debug tier and zoom stop to populate this table.")
            return@buildList
        }

        CameraQualityCalibrationMode.entries.forEach { mode ->
            val modeRecords = records.filter { record -> record.mode == mode }
            if (modeRecords.isNotEmpty()) {
                add("${mode.displayLabel} graph table:")
                modeRecords
                    .groupBy { record -> cameraQualityCalibrationLensToken(record.lensLabel) }
                    .toSortedMap()
                    .forEach { (_, lensRecords) ->
                        val lensLabel = lensRecords.first().lensLabel
                        val defaultRecord = lensRecords.firstOrNull {
                            it.tier == CameraCalibratedQualityTier.Default && it.graphPreset.selectorCandidate
                        }
                        add("  ${lensLabel.lowercase(Locale.US)} lens:")
                        lensRecords
                            .sortedWith(
                                compareBy<CameraQualityCalibrationRecord> { it.tier.ordinal }
                                    .thenBy { it.graphPreset.ordinal },
                            )
                            .forEach { record ->
                                add("    ${record.graphPreset.displayLabel}: ${record.summaryFor(defaultRecord, mode)}")
                }
            }
        }
        if (zoomRecords.isNotEmpty()) {
            add("Zoom table:")
            zoomRecords
                .groupBy { record ->
                    "${record.mode.token}:${cameraQualityCalibrationLensToken(record.lensLabel)}"
                }
                .toSortedMap()
                .forEach { (_, lensModeRecords) ->
                    val first = lensModeRecords.first()
                    add("  ${first.lensLabel.lowercase(Locale.US)} ${first.mode.displayLabel.lowercase(Locale.US)}:")
                    lensModeRecords
                        .sortedWith(compareBy<CameraZoomCalibrationRecord> { it.requestedZoomRatio })
                        .forEach { record ->
                            add("    ${cameraZoomCalibrationRatioLabel(record.requestedZoomRatio)}: ${record.summaryFor()}")
                        }
                }
        }
    }
    }

    fun withInferredSelectorEligibility(): CameraQualityCalibrationProfile {
        val updatedRecords = records.map { record ->
            val defaultRecord = records.firstOrNull { candidate ->
                candidate.mode == record.mode &&
                    cameraQualityCalibrationLensToken(candidate.lensLabel) == cameraQualityCalibrationLensToken(record.lensLabel) &&
                    candidate.tier == CameraCalibratedQualityTier.Default &&
                    candidate.graphPreset.selectorCandidate
            }
            val eligibility = record.selectorEligibility(defaultRecord)
            record.copy(
                selectorEligible = eligibility.eligible,
                selectorRejectReason = eligibility.reason,
                bitrateBitsPerSecond = record.bitrateBitsPerSecond
                    ?: videoBitrateBitsPerSecond(record.fileSizeBytes, record.durationMillis),
            )
        }
        val updatedZoomRecords = zoomRecords.map { record ->
            val eligibility = record.productEligibility()
            record.copy(
                productEligible = eligibility.eligible,
                productRejectReason = eligibility.reason,
            )
        }
        return copy(
            records = updatedRecords,
            zoomRecords = updatedZoomRecords,
        )
    }

    fun mandatoryReadiness(
        environment: CameraQualityCalibrationEnvironment,
    ): CameraQualityCalibrationReadiness {
        fun notReady(
            reason: String,
            completedStepCount: Int = completedRequiredStepCount(),
            terminalFailure: Boolean = false,
        ): CameraQualityCalibrationReadiness = CameraQualityCalibrationReadiness(
            ready = false,
            reason = reason,
            completedStepCount = completedStepCount,
            requiredStepCount = cameraQualityCalibrationTotalRequiredStepCount(),
            progressPercent = cameraQualityCalibrationProgressPercent(
                completedStepCount = completedStepCount,
                requiredStepCount = cameraQualityCalibrationTotalRequiredStepCount(),
            ),
            terminalFailure = terminalFailure,
        )

        secureStoreStatus?.let { status ->
            if (status.startsWith("unreadable:", ignoreCase = true)) {
                return notReady(
                    reason = "Secure calibration profile could not be read: ${status.removePrefix("unreadable:").trim()}",
                    completedStepCount = 0,
                    terminalFailure = true,
                )
            }
        }

        if (schemaVersion != environment.schemaVersion) {
            return notReady(
                reason = "Calibration schema changed; this device needs a fresh camera calibration.",
                completedStepCount = 0,
            )
        }
        if (deviceKey != environment.deviceKey) {
            return notReady(
                reason = "Calibration belongs to a different device.",
                completedStepCount = 0,
                terminalFailure = true,
            )
        }
        if (modelId != environment.modelId) {
            return notReady(
                reason = "Safety model changed; calibration must be rerun.",
                completedStepCount = 0,
            )
        }
        if (policyHash != environment.policyHash) {
            return notReady(
                reason = "Safety policy changed; calibration must be rerun.",
                completedStepCount = 0,
            )
        }
        if (cameraXGraphAssumption != environment.cameraXGraphAssumption) {
            return notReady(
                reason = "CameraX graph assumptions changed; calibration must be rerun.",
                completedStepCount = 0,
            )
        }
        if (requiredStepSetHash != environment.requiredStepSetHash) {
            return notReady(
                reason = "Required graph-probe set changed; calibration must be rerun.",
                completedStepCount = 0,
            )
        }
        if (cameraInventoryHash != environment.cameraInventoryHash) {
            return notReady(
                reason = "Camera inventory changed; calibration must be rerun.",
                completedStepCount = 0,
            )
        }

        if (buildFingerprint != environment.buildFingerprint || appVersionName != environment.appVersionName ||
            appVersionCode != environment.appVersionCode) {
            return notReady("Build or application version changed; recalibration required.", completedStepCount = 0)
        }

        val recordsByKey = records.associateBy { record -> record.recordKey() }
        val missingSteps = cameraQualityCalibrationRequiredRecords.filter { required ->
            recordsByKey[required.recordKey] == null
        }
        if (missingSteps.isNotEmpty()) {
            val completed = cameraQualityCalibrationRequiredRecords.size - missingSteps.size
            return notReady(
                reason = "Calibration incomplete: $completed of ${cameraQualityCalibrationRequiredRecords.size} required graph probes finished.",
                completedStepCount = completed + completedRequiredZoomStepCount(),
            )
        }

        cameraQualityCalibrationRequiredRecords.forEach { required ->
            val record = recordsByKey.getValue(required.recordKey)
            if (record.probePurpose == CameraQualityCalibrationProbePurpose.OptionalDiagnostics ||
                record.mandatoryForReadiness == false
            ) {
                return notReady(
                    reason = "Required calibration record was marked optional for ${required.displayLabel}.",
                    terminalFailure = true,
                )
            }
            if (required.probePurpose == CameraQualityCalibrationProbePurpose.DerivedSelectorDecision &&
                record.derivedFromRecordKey.isNullOrBlank()
            ) {
                return notReady(
                    reason = "Derived calibration decision is missing its source record for ${required.displayLabel}.",
                    terminalFailure = true,
                )
            }
            if (record.calibrationArtifactCleanupSucceeded != true) {
                return notReady(
                    reason = "Calibration media cleanup is not proven for ${required.displayLabel}.",
                    terminalFailure = true,
                )
            }
            if (record.status == CameraQualityCalibrationStatus.Failed && record.failureReason.isNullOrBlank()) {
                return notReady(
                    reason = "Calibration failure reason is missing for ${required.displayLabel}.",
                    terminalFailure = true,
                )
            }
            if (required.defaultRequired) {
                if (record.status != CameraQualityCalibrationStatus.Verified) {
                    return notReady(
                        reason = "Required default graph failed for ${required.displayLabel}: ${record.failureReason ?: "unknown failure"}.",
                        terminalFailure = true,
                    )
                }
                if (!record.hasActualOutput) {
                    return notReady(
                        reason = "Required default graph has no measured output for ${required.displayLabel}.",
                        terminalFailure = true,
                    )
                }
                if (record.previewStable == false) {
                    return notReady(
                        reason = "Required default graph has unstable preview for ${required.displayLabel}.",
                        terminalFailure = true,
                    )
                }
                if (record.gateStatusLabel?.contains("runtime failure", ignoreCase = true) == true) {
                    return notReady(
                        reason = "Required default graph had a gate runtime failure for ${required.displayLabel}.",
                        terminalFailure = true,
                    )
                }
            }
        }

        val zoomRecordsByKey = zoomRecords.associateBy { record -> record.recordKey() }
        val missingZoomSteps = cameraZoomCalibrationRequiredRecords.filter { required ->
            zoomRecordsByKey[required.recordKey] == null
        }
        if (missingZoomSteps.isNotEmpty()) {
            val completed = cameraZoomCalibrationRequiredRecords.size - missingZoomSteps.size
            return notReady(
                reason = "Zoom calibration incomplete: $completed of ${cameraZoomCalibrationRequiredRecords.size} required zoom probes finished.",
                completedStepCount = cameraQualityCalibrationRequiredRecords.size + completed,
            )
        }
        cameraZoomCalibrationRequiredRecords.forEach { required ->
            val record = zoomRecordsByKey.getValue(required.recordKey)
            if (cameraZoomRatiosEquivalent(required.requestedZoomRatio, 1f) &&
                record.productEligibility().eligible != true
            ) {
                return notReady(
                    reason = "Required 1x zoom probe failed for ${required.displayLabel}: ${record.productEligibility().reason}.",
                    terminalFailure = true,
                )
            }
        }

        return CameraQualityCalibrationReadiness(
            ready = true,
            reason = "Calibration complete.",
            completedStepCount = cameraQualityCalibrationTotalRequiredStepCount(),
            requiredStepCount = cameraQualityCalibrationTotalRequiredStepCount(),
            progressPercent = 100,
            terminalFailure = false,
        )
    }

    private fun completedRequiredStepCount(): Int {
        val recordKeys = records.mapTo(mutableSetOf()) { record -> record.recordKey() }
        return cameraQualityCalibrationRequiredRecords.count { required -> required.recordKey in recordKeys } +
            completedRequiredZoomStepCount()
    }

    private fun completedRequiredZoomStepCount(): Int {
        val zoomRecordKeys = zoomRecords.mapTo(mutableSetOf()) { record -> record.recordKey() }
        return cameraZoomCalibrationRequiredRecords.count { required -> required.recordKey in zoomRecordKeys }
    }
}

private val cameraQualityCalibrationChildFacingTierOrder = listOf(
    CameraCalibratedQualityTier.Default,
    CameraCalibratedQualityTier.High,
    CameraCalibratedQualityTier.Low,
)

private data class CameraQualityCalibrationRequiredRecord(
    val mode: CameraQualityCalibrationMode,
    val lensToken: String,
    val tier: CameraCalibratedQualityTier,
    val graphPreset: CameraQualityGraphPreset,
    val defaultRequired: Boolean,
    val probePurpose: CameraQualityCalibrationProbePurpose,
    val activeCaptureRequired: Boolean,
) {
    val recordKey: String = cameraQualityCalibrationRecordKey(
        mode = mode,
        lensToken = lensToken,
        tier = tier,
        graphPreset = graphPreset,
    )

    val displayLabel: String = listOf(
        lensToken,
        mode.displayLabel,
        tier.labelFor(mode),
        graphPreset.displayLabel,
    ).joinToString(separator = " ")
}

private val cameraQualityCalibrationRequiredRecords: List<CameraQualityCalibrationRequiredRecord> = listOf(
    requiredCalibrationRecord(CameraQualityCalibrationMode.Photo, "back", CameraCalibratedQualityTier.Low, CameraQualityGraphPreset.PhotoLow, CameraQualityCalibrationProbePurpose.DerivedSelectorDecision, activeCaptureRequired = false),
    requiredCalibrationRecord(CameraQualityCalibrationMode.Photo, "back", CameraCalibratedQualityTier.Default, CameraQualityGraphPreset.PhotoStable, CameraQualityCalibrationProbePurpose.RequiredDefault),
    requiredCalibrationRecord(CameraQualityCalibrationMode.Photo, "back", CameraCalibratedQualityTier.High, CameraQualityGraphPreset.PhotoHighStillProbeV2, CameraQualityCalibrationProbePurpose.RequiredSelectorCandidate),
    requiredCalibrationRecord(CameraQualityCalibrationMode.Photo, "front", CameraCalibratedQualityTier.Default, CameraQualityGraphPreset.PhotoStable, CameraQualityCalibrationProbePurpose.RequiredDefault),
    requiredCalibrationRecord(CameraQualityCalibrationMode.Video, "back", CameraCalibratedQualityTier.Default, CameraQualityGraphPreset.VideoStable, CameraQualityCalibrationProbePurpose.RequiredDefault),
    requiredCalibrationRecord(CameraQualityCalibrationMode.Video, "front", CameraCalibratedQualityTier.Default, CameraQualityGraphPreset.VideoStable, CameraQualityCalibrationProbePurpose.RequiredDefault),
)

private data class CameraZoomCalibrationRequiredRecord(
    val mode: CameraQualityCalibrationMode,
    val lensToken: String,
    val requestedZoomRatio: Float,
) {
    val recordKey: String = cameraZoomCalibrationRecordKey(
        mode = mode,
        lensToken = lensToken,
        requestedZoomRatio = requestedZoomRatio,
    )

    val displayLabel: String = listOf(
        lensToken,
        mode.displayLabel,
        cameraZoomCalibrationRatioLabel(requestedZoomRatio),
    ).joinToString(separator = " ")
}

private val cameraZoomCalibrationRequiredRecords: List<CameraZoomCalibrationRequiredRecord> =
    CameraQualityCalibrationMode.entries.flatMap { mode ->
        cameraZoomCalibrationProductStops().map { zoomRatio ->
            CameraZoomCalibrationRequiredRecord(
                mode = mode,
                lensToken = "back",
                requestedZoomRatio = zoomRatio,
            )
        }
    }

private fun requiredCalibrationRecord(
    mode: CameraQualityCalibrationMode,
    lensToken: String,
    tier: CameraCalibratedQualityTier,
    graphPreset: CameraQualityGraphPreset,
    probePurpose: CameraQualityCalibrationProbePurpose,
    activeCaptureRequired: Boolean = true,
): CameraQualityCalibrationRequiredRecord = CameraQualityCalibrationRequiredRecord(
    mode = mode,
    lensToken = lensToken,
    tier = tier,
    graphPreset = graphPreset,
    defaultRequired = tier == CameraCalibratedQualityTier.Default && graphPreset.selectorCandidate,
    probePurpose = probePurpose,
    activeCaptureRequired = activeCaptureRequired,
)

public fun cameraQualityCalibrationRequiredStepSetHash(): String =
    sha256Hex(
        (
            cameraQualityCalibrationRequiredRecords.map { required -> required.recordKey } +
                cameraZoomCalibrationRequiredRecords.map { required -> required.recordKey }
            ).joinToString(separator = "|"),
    ).take(16)

public fun cameraQualityCalibrationRequiredStepCount(): Int =
    cameraQualityCalibrationRequiredRecords.size

public fun cameraZoomCalibrationRequiredStepCount(): Int =
    cameraZoomCalibrationRequiredRecords.size

public fun cameraQualityCalibrationTotalRequiredStepCount(): Int =
    cameraQualityCalibrationRequiredStepCount() + cameraZoomCalibrationRequiredStepCount()

public fun cameraQualityCalibrationRequiredActiveCaptureCount(): Int =
    cameraQualityCalibrationRequiredRecords.count { required -> required.activeCaptureRequired }

public fun cameraQualityCalibrationRequiredDerivedDecisionCount(): Int =
    cameraQualityCalibrationRequiredRecords.count { required ->
        required.probePurpose == CameraQualityCalibrationProbePurpose.DerivedSelectorDecision
    }

private fun List<CameraQualityCalibrationRecord>.withDerivedPhotoLowDecision(
    updatedAtMillis: Long,
): List<CameraQualityCalibrationRecord> {
    val source = firstOrNull { record ->
        record.mode == CameraQualityCalibrationMode.Photo &&
            cameraQualityCalibrationLensToken(record.lensLabel) == "back" &&
            record.tier == CameraCalibratedQualityTier.Default &&
            record.graphPreset == CameraQualityGraphPreset.PhotoStable
    } ?: return this
    return filterNot { record ->
        record.mode == CameraQualityCalibrationMode.Photo &&
            cameraQualityCalibrationLensToken(record.lensLabel) == "back" &&
            record.tier == CameraCalibratedQualityTier.Low &&
            record.graphPreset == CameraQualityGraphPreset.PhotoLow &&
            record.probePurpose == CameraQualityCalibrationProbePurpose.DerivedSelectorDecision
    } + cameraQualityCalibrationDerivedPhotoLowDecision(source, updatedAtMillis)
}

public fun cameraQualityCalibrationDerivedPhotoLowDecision(
    source: CameraQualityCalibrationRecord,
    updatedAtMillis: Long,
): CameraQualityCalibrationRecord {
    val failureReason = source.derivedPhotoLowFailureReason()
    val derivedDimensions = if (failureReason == null) {
        derivedLowPhotoDimensions(
            width = source.finalWidth ?: 0,
            height = source.finalHeight ?: 0,
        )
    } else {
        null
    }
    return source.copy(
        tier = CameraCalibratedQualityTier.Low,
        status = if (failureReason == null) {
            CameraQualityCalibrationStatus.Verified
        } else {
            CameraQualityCalibrationStatus.Failed
        },
        requestedPolicy = "PhotoLow: derived from PhotoStable default; post-capture downscale to <=3MP",
        finalWidth = derivedDimensions?.first,
        finalHeight = derivedDimensions?.second,
        failureReason = failureReason,
        updatedAtMillis = updatedAtMillis,
        graphPreset = CameraQualityGraphPreset.PhotoLow,
        useCasesBound = CameraQualityGraphPreset.PhotoLow.useCases,
        viewportMode = CameraQualityGraphPreset.PhotoLow.viewportMode,
        liveAnalysisIncluded = CameraQualityGraphPreset.PhotoLow.includesLiveAnalysis,
        requestedCameraXQuality = CameraQualityGraphPreset.PhotoLow.requestedCameraXQuality,
        selectorEligible = null,
        selectorRejectReason = null,
        gateDurationMillis = 0L,
        calibrationGateMode = CameraQualityCalibrationGateCoverage.DerivedFromDefault.displayLabel,
        calibrationGateRequiredForSelector = failureReason == null,
        calibrationArtifactCleanupSucceeded = true,
        calibrationArtifactCleanupSummary = "derived from default still probe; no extra capture artifact created",
        calibrationStepIndex = null,
        calibrationStepStartedAtMillis = null,
        calibrationStepCompletedAtMillis = null,
        calibrationStepDurationMillis = null,
        calibrationBindWaitMillis = null,
        calibrationCaptureOrRecordMillis = null,
        calibrationRecordingTargetMillis = null,
        probePurpose = CameraQualityCalibrationProbePurpose.DerivedSelectorDecision,
        gateCoverage = CameraQualityCalibrationGateCoverage.DerivedFromDefault,
        fastProbeRetried = null,
        derivedFromRecordKey = source.recordKey(),
        mandatoryForReadiness = true,
    )
}

private fun CameraQualityCalibrationRecord.derivedPhotoLowFailureReason(): String? {
    if (status != CameraQualityCalibrationStatus.Verified) {
        return "default still probe failed"
    }
    val width = finalWidth ?: return "default still output unmeasured"
    val height = finalHeight ?: return "default still output unmeasured"
    if (width <= 0 || height <= 0) {
        return "default still output invalid"
    }
    val pixelCount = width.toLong() * height.toLong()
    if (pixelCount <= CAMERA_QUALITY_CALIBRATION_LOW_PHOTO_MAX_PIXELS) {
        return "default still output is not larger than the Low photo target"
    }
    if (gateSamplingProfileId.isNullOrBlank()) {
        return "default still gate sampling profile unmeasured"
    }
    if (gateSamplingProfileSelectorEligible != true) {
        return "default still gate sampling profile unsupported"
    }
    if (gateStatusLabel?.contains("runtime failure", ignoreCase = true) == true) {
        return "default still gate runtime failure"
    }
    return null
}

private fun derivedLowPhotoDimensions(
    width: Int,
    height: Int,
): Pair<Int, Int> {
    val pixels = width.toLong() * height.toLong()
    if (pixels <= CAMERA_QUALITY_CALIBRATION_LOW_PHOTO_MAX_PIXELS) {
        return width to height
    }
    val scale = sqrt(CAMERA_QUALITY_CALIBRATION_LOW_PHOTO_MAX_PIXELS.toDouble() / pixels.toDouble())
    var derivedWidth = (width * scale).toInt().coerceAtLeast(1)
    var derivedHeight = (height * scale).toInt().coerceAtLeast(1)
    while (derivedWidth.toLong() * derivedHeight.toLong() > CAMERA_QUALITY_CALIBRATION_LOW_PHOTO_MAX_PIXELS) {
        if (derivedWidth >= derivedHeight && derivedWidth > 1) {
            derivedWidth -= 1
        } else if (derivedHeight > 1) {
            derivedHeight -= 1
        } else {
            break
        }
    }
    return derivedWidth to derivedHeight
}

private fun cameraQualityCalibrationRecordKey(
    mode: CameraQualityCalibrationMode,
    lensToken: String,
    tier: CameraCalibratedQualityTier,
    graphPreset: CameraQualityGraphPreset,
): String = listOf(
    mode.token,
    lensToken,
    tier.token,
    graphPreset.token,
).joinToString(separator = ".")

private fun cameraZoomCalibrationRecordKey(
    mode: CameraQualityCalibrationMode,
    lensToken: String,
    requestedZoomRatio: Float,
): String = listOf(
    mode.token,
    lensToken,
    "zoom",
    cameraZoomCalibrationStopToken(requestedZoomRatio),
).joinToString(separator = ".")

public fun cameraZoomCalibrationProductStops(): List<Float> = listOf(0.6f, 1f, 2f, CAMERA_PRODUCT_MAX_ZOOM_RATIO)

public fun cameraZoomCalibrationDiagnosticStops(): List<Float> = listOf(10f)

public fun cameraZoomCalibrationStopsForRange(
    minZoomRatio: Float,
    maxZoomRatio: Float,
): List<Float> =
    cameraZoomCalibrationProductStops()
        .filter { stop ->
            cameraZoomRatioInRange(
                zoomRatio = stop,
                minZoomRatio = minZoomRatio,
                maxZoomRatio = maxZoomRatio,
            )
        }
        .ifEmpty {
            cameraZoomCalibrationProductStops().filter { stop ->
                cameraZoomRatiosEquivalent(stop, 1f) &&
                    cameraZoomRatioInRange(
                        zoomRatio = stop,
                        minZoomRatio = minZoomRatio,
                        maxZoomRatio = maxZoomRatio,
                    )
            }
        }

public fun cameraZoomRatioInRange(
    zoomRatio: Float,
    minZoomRatio: Float?,
    maxZoomRatio: Float?,
): Boolean {
    val min = minZoomRatio ?: 1f
    val max = maxZoomRatio ?: 1f
    return zoomRatio >= min - cameraZoomCalibrationTolerance &&
        zoomRatio <= max + cameraZoomCalibrationTolerance
}

public fun cameraZoomRatiosEquivalent(
    first: Float,
    second: Float,
): Boolean = abs(first - second) <= cameraZoomCalibrationTolerance

public fun cameraZoomCalibrationRatioLabel(zoomRatio: Float): String {
    val rounded = ((zoomRatio * 10f).roundToInt()) / 10f
    return if (abs(rounded - rounded.toInt()) < 0.05f) {
        "${rounded.toInt()}x"
    } else {
        String.format(Locale.US, "%.1fx", rounded)
    }
}

private fun cameraZoomCalibrationStopToken(zoomRatio: Float): String =
    String.format(Locale.US, "%.1f", ((zoomRatio * 10f).roundToInt()) / 10f)

private fun cameraQualityCalibrationProgressPercent(
    completedStepCount: Int,
    requiredStepCount: Int,
): Int {
    if (requiredStepCount <= 0) {
        return 0
    }
    return ((completedStepCount.coerceIn(0, requiredStepCount) * 100f) / requiredStepCount)
        .roundToInt()
        .coerceIn(0, 100)
}

public class CameraQualityCalibrationStore(private val environment: CameraQualityCalibrationEnvironment) {
    private val directoryName = "sakos-camera-calibration"

    fun directory(context: Context): File = File(context.noBackupFilesDir, directoryName).apply {
        mkdirs()
    }

    fun profileFile(context: Context): File = File(directory(context), cameraQualityCalibrationSecureProfileFileName)

    fun obsoleteSecureProfileFiles(context: Context): List<File> =
        cameraQualityCalibrationObsoleteSecureProfileFileNames.map { fileName ->
            File(directory(context), fileName)
        }

    fun legacyProfileFile(context: Context): File = File(directory(context), cameraQualityCalibrationLegacyProfileFileName)

    @Synchronized
    fun clear(context: Context) {
        val file = profileFile(context)
        if (file.exists()) {
            check(file.delete()) { "Calibration reset failed." }
        }
        obsoleteSecureProfileFiles(context).forEach { obsoleteFile ->
            if (obsoleteFile.exists()) {
                check(obsoleteFile.delete()) { "Obsolete calibration cleanup failed." }
            }
        }
        val legacyFile = legacyProfileFile(context)
        if (legacyFile.exists()) {
            check(legacyFile.delete()) { "Legacy calibration cleanup failed." }
        }
    }

    @Synchronized
    fun load(context: Context): CameraQualityCalibrationProfile {
        val pending = File(directory(context), "${cameraQualityCalibrationSecureProfileFileName}.pending")
        check(!pending.exists() || pending.delete()) { "Interrupted calibration cleanup failed." }
        val file = profileFile(context)
        if (file.exists()) {
            return runCatching {
                CameraQualityCalibrationSecureFileStore.load(
                    file = file,
                    cipher = AndroidKeystoreCameraQualityCalibrationCipher(),
                    associatedData = cameraQualityCalibrationAssociatedData(environment),
                ).also { profile ->
                    check(profile.schemaVersion == environment.schemaVersion && profile.deviceKey == environment.deviceKey &&
                        profile.modelId == environment.modelId && profile.policyHash == environment.policyHash &&
                        profile.cameraXGraphAssumption == environment.cameraXGraphAssumption && profile.buildFingerprint == environment.buildFingerprint &&
                        profile.appVersionName == environment.appVersionName && profile.appVersionCode == environment.appVersionCode &&
                        profile.requiredStepSetHash == environment.requiredStepSetHash && profile.cameraInventoryHash == environment.cameraInventoryHash) {
                        "Calibration identity changed; retry required."
                    }
                }
            }.getOrElse { error ->
                emptyProfile(
                    environment = environment,
                    secureStoreStatus = "unreadable:${error.message ?: error.javaClass.simpleName}",
                )
            }
        }

        val obsoleteRemoved = deleteObsoleteProfiles(context)

        return emptyProfile(
            environment = environment,
            secureStoreStatus = if (obsoleteRemoved) {
                "obsolete v4/v5/v6/v7/v8/v9/properties profile removed; recalibration required"
            } else {
                null
            },
        )
    }

    @Synchronized
    fun recordObservation(
        context: Context,
        record: CameraQualityCalibrationRecord,
    ): CameraQualityCalibrationProfile {
        val current = load(context)
        val nextRecords = current.records
            .filterNot { existing ->
                existing.mode == record.mode &&
                    cameraQualityCalibrationLensToken(existing.lensLabel) == cameraQualityCalibrationLensToken(record.lensLabel) &&
                    existing.tier == record.tier &&
                    existing.graphPreset == record.graphPreset
            }
            .plus(record)
            .withDerivedPhotoLowDecision(updatedAtMillis = record.updatedAtMillis)
        val startedAtMillis = current.calibrationStartedAtMillis
            ?: record.calibrationBatchStartedAtMillis
            ?: record.calibrationStepStartedAtMillis
            ?: current.createdAtMillis
            ?: record.updatedAtMillis
        val requiredRecordKeys = cameraQualityCalibrationRequiredRecords.mapTo(mutableSetOf()) { required ->
            required.recordKey
        }
        val requiredZoomRecordKeys = cameraZoomCalibrationRequiredRecords.mapTo(mutableSetOf()) { required ->
            required.recordKey
        }
        val completedRequired = requiredRecordKeys.all { requiredKey ->
            nextRecords.any { candidate -> candidate.recordKey() == requiredKey }
        } && requiredZoomRecordKeys.all { requiredKey ->
            current.zoomRecords.any { candidate -> candidate.recordKey() == requiredKey }
        }
        val completedAtMillis = if (completedRequired) {
            record.updatedAtMillis
        } else {
            null
        }
        val nextProfile = CameraQualityCalibrationProfile(
            schemaVersion = environment.schemaVersion,
            deviceKey = environment.deviceKey,
            createdAtMillis = current.createdAtMillis ?: record.updatedAtMillis,
            updatedAtMillis = record.updatedAtMillis,
            appVersionName = environment.appVersionName,
            appVersionCode = environment.appVersionCode,
            modelId = environment.modelId,
            policyHash = environment.policyHash,
            cameraXGraphAssumption = environment.cameraXGraphAssumption,
            buildFingerprint = environment.buildFingerprint,
            requiredStepSetHash = environment.requiredStepSetHash,
            cameraInventoryHash = environment.cameraInventoryHash,
            calibrationStartedAtMillis = startedAtMillis,
            calibrationCompletedAtMillis = completedAtMillis,
            calibrationTotalDurationMillis = completedAtMillis?.let { completed ->
                (completed - startedAtMillis).coerceAtLeast(0L)
            },
            records = nextRecords.sortedCalibrationRecords(),
            zoomRecords = current.zoomRecords.sortedZoomCalibrationRecords(),
        ).withInferredSelectorEligibility()
        save(context, nextProfile, environment)
        return nextProfile
    }

    @Synchronized
    fun recordZoomObservation(
        context: Context,
        record: CameraZoomCalibrationRecord,
    ): CameraQualityCalibrationProfile {
        val current = load(context)
        val nextZoomRecords = current.zoomRecords
            .filterNot { existing ->
                existing.mode == record.mode &&
                    cameraQualityCalibrationLensToken(existing.lensLabel) == cameraQualityCalibrationLensToken(record.lensLabel) &&
                    cameraZoomRatiosEquivalent(existing.requestedZoomRatio, record.requestedZoomRatio)
            }
            .plus(record)
        val startedAtMillis = current.calibrationStartedAtMillis
            ?: record.calibrationBatchStartedAtMillis
            ?: record.calibrationStepStartedAtMillis
            ?: current.createdAtMillis
            ?: record.updatedAtMillis
        val requiredRecordKeys = cameraQualityCalibrationRequiredRecords.mapTo(mutableSetOf()) { required ->
            required.recordKey
        }
        val requiredZoomRecordKeys = cameraZoomCalibrationRequiredRecords.mapTo(mutableSetOf()) { required ->
            required.recordKey
        }
        val completedRequired = requiredRecordKeys.all { requiredKey ->
            current.records.any { candidate -> candidate.recordKey() == requiredKey }
        } && requiredZoomRecordKeys.all { requiredKey ->
            nextZoomRecords.any { candidate -> candidate.recordKey() == requiredKey }
        }
        val completedAtMillis = if (completedRequired) {
            record.updatedAtMillis
        } else {
            null
        }
        val nextProfile = CameraQualityCalibrationProfile(
            schemaVersion = environment.schemaVersion,
            deviceKey = environment.deviceKey,
            createdAtMillis = current.createdAtMillis ?: record.updatedAtMillis,
            updatedAtMillis = record.updatedAtMillis,
            appVersionName = environment.appVersionName,
            appVersionCode = environment.appVersionCode,
            modelId = environment.modelId,
            policyHash = environment.policyHash,
            cameraXGraphAssumption = environment.cameraXGraphAssumption,
            buildFingerprint = environment.buildFingerprint,
            requiredStepSetHash = environment.requiredStepSetHash,
            cameraInventoryHash = environment.cameraInventoryHash,
            calibrationStartedAtMillis = startedAtMillis,
            calibrationCompletedAtMillis = completedAtMillis,
            calibrationTotalDurationMillis = completedAtMillis?.let { completed ->
                (completed - startedAtMillis).coerceAtLeast(0L)
            },
            records = current.records.sortedCalibrationRecords(),
            zoomRecords = nextZoomRecords.sortedZoomCalibrationRecords(),
        ).withInferredSelectorEligibility()
        save(context, nextProfile, environment)
        return nextProfile
    }

    fun saveForTest(
        file: File,
        profile: CameraQualityCalibrationProfile,
        cipher: CameraQualityCalibrationCipher,
        environment: CameraQualityCalibrationEnvironment,
    ) {
        CameraQualityCalibrationSecureFileStore.save(
            file = file,
            profile = profile.withInferredSelectorEligibility(),
            cipher = cipher,
            associatedData = cameraQualityCalibrationAssociatedData(environment),
        )
    }

    fun loadForTest(
        file: File,
        cipher: CameraQualityCalibrationCipher,
        environment: CameraQualityCalibrationEnvironment,
    ): CameraQualityCalibrationProfile = CameraQualityCalibrationSecureFileStore.load(
        file = file,
        cipher = cipher,
        associatedData = cameraQualityCalibrationAssociatedData(environment),
    )

    private fun save(
        context: Context,
        profile: CameraQualityCalibrationProfile,
        environment: CameraQualityCalibrationEnvironment,
    ) {
        saveForTest(
            file = profileFile(context),
            profile = profile,
            cipher = AndroidKeystoreCameraQualityCalibrationCipher(),
            environment = environment,
        )
    }

    private fun emptyProfile(
        environment: CameraQualityCalibrationEnvironment,
        secureStoreStatus: String? = null,
    ): CameraQualityCalibrationProfile = CameraQualityCalibrationProfile(
        schemaVersion = environment.schemaVersion,
        deviceKey = environment.deviceKey,
        createdAtMillis = null,
        updatedAtMillis = null,
        appVersionName = environment.appVersionName,
        appVersionCode = environment.appVersionCode,
        modelId = environment.modelId,
        policyHash = environment.policyHash,
        cameraXGraphAssumption = environment.cameraXGraphAssumption,
        buildFingerprint = environment.buildFingerprint,
        requiredStepSetHash = environment.requiredStepSetHash,
        cameraInventoryHash = environment.cameraInventoryHash,
        secureStoreStatus = secureStoreStatus,
        calibrationStartedAtMillis = null,
        calibrationCompletedAtMillis = null,
        calibrationTotalDurationMillis = null,
        records = emptyList(),
        zoomRecords = emptyList(),
    )

    private fun deleteObsoleteProfiles(context: Context): Boolean {
        var foundAny = false
        (obsoleteSecureProfileFiles(context) + legacyProfileFile(context)).forEach { obsoleteFile ->
            if (obsoleteFile.exists()) {
                foundAny = true
                check(obsoleteFile.delete()) { "Obsolete calibration cleanup failed." }
            }
        }
        return foundAny
    }
}

public data class CameraQualityEncryptedPayload(
    val iv: ByteArray,
    val cipherText: ByteArray,
)

public interface CameraQualityCalibrationCipher {
    fun encrypt(
        plainText: ByteArray,
        associatedData: ByteArray,
    ): CameraQualityEncryptedPayload

    fun decrypt(
        encryptedPayload: CameraQualityEncryptedPayload,
        associatedData: ByteArray,
    ): ByteArray
}

private object CameraQualityCalibrationSecureFileStore {
    fun save(
        file: File,
        profile: CameraQualityCalibrationProfile,
        cipher: CameraQualityCalibrationCipher,
        associatedData: ByteArray,
    ) {
        file.parentFile?.mkdirs()
        val encryptedPayload = cipher.encrypt(
            plainText = profile.toJsonPayloadBytes(),
            associatedData = associatedData,
        )
        val envelope = Properties().apply {
            setProperty("version", CAMERA_QUALITY_CALIBRATION_SCHEMA_VERSION.toString())
            setProperty("debugOnly", "false")
            setProperty("scope", "device-camera-quality-calibration")
            setProperty("cipher", "AES/GCM/NoPadding")
            setProperty("payload", "json")
            setProperty("iv", encryptedPayload.iv.base64Encode())
            setProperty("cipherText", encryptedPayload.cipherText.base64Encode())
        }
        val pendingFile = File(file.parentFile, "${file.name}.pending")
        java.io.FileOutputStream(pendingFile).use { output -> output.write(envelope.toEnvelopeBytes()); output.fd.sync() }
        check(pendingFile.renameTo(file)) { "Calibration profile commit failed." }
    }

    fun load(
        file: File,
        cipher: CameraQualityCalibrationCipher,
        associatedData: ByteArray,
    ): CameraQualityCalibrationProfile {
        val envelope = Properties().apply {
            ByteArrayInputStream(file.readBytes()).use(::load)
        }
        check(envelope.getProperty("version")?.toIntOrNull() == CAMERA_QUALITY_CALIBRATION_SCHEMA_VERSION) {
            "Unsupported calibration envelope version ${envelope.getProperty("version")}."
        }
        check(envelope.getProperty("payload") == "json") {
            "Unsupported calibration payload ${envelope.getProperty("payload")}."
        }
        val encryptedPayload = CameraQualityEncryptedPayload(
            iv = envelope.getProperty("iv").orEmpty().base64Decode(),
            cipherText = envelope.getProperty("cipherText").orEmpty().base64Decode(),
        )
        val plainText = cipher.decrypt(
            encryptedPayload = encryptedPayload,
            associatedData = associatedData,
        )
        return cameraQualityCalibrationJson
            .decodeFromString<CameraQualityCalibrationProfile>(plainText.toString(Charsets.UTF_8))
            .withInferredSelectorEligibility()
    }
}

private class AndroidKeystoreCameraQualityCalibrationCipher : CameraQualityCalibrationCipher {
    override fun encrypt(
        plainText: ByteArray,
        associatedData: ByteArray,
    ): CameraQualityEncryptedPayload {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        cipher.updateAAD(associatedData)
        return CameraQualityEncryptedPayload(
            iv = cipher.iv,
            cipherText = cipher.doFinal(plainText),
        )
    }

    override fun decrypt(
        encryptedPayload: CameraQualityEncryptedPayload,
        associatedData: ByteArray,
    ): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(128, encryptedPayload.iv),
        )
        cipher.updateAAD(associatedData)
        return cipher.doFinal(encryptedPayload.cipherText)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
        val existingKey = (keyStore.getEntry(cameraQualityCalibrationSecureKeyAlias, null) as? KeyStore.SecretKeyEntry)
            ?.secretKey
        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore",
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                cameraQualityCalibrationSecureKeyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return keyGenerator.generateKey()
    }
}

private fun cameraQualityCalibrationAssociatedData(
    environment: CameraQualityCalibrationEnvironment,
): ByteArray = listOf(
    environment.packageName,
    "camera-quality-calibration",
    "schema-${CAMERA_QUALITY_CALIBRATION_SCHEMA_VERSION}",
).joinToString(separator = "|").toByteArray(Charsets.UTF_8)

public fun debugPhotoQualityPolicyLabel(tier: CameraCalibratedQualityTier): String =
    debugGraphPresetPolicyLabel(
        CameraQualityGraphPreset.selectorPresetFor(
            mode = CameraQualityCalibrationMode.Photo,
            tier = tier,
        ),
    )

public fun debugVideoQualityPolicyLabel(tier: CameraCalibratedQualityTier): String =
    debugGraphPresetPolicyLabel(
        CameraQualityGraphPreset.selectorPresetFor(
            mode = CameraQualityCalibrationMode.Video,
            tier = tier,
        ),
    )

public fun cameraProductEffectiveQualityTier(
    mode: CameraQualityCalibrationMode,
    requestedTier: CameraCalibratedQualityTier,
    debugQualityOverrideActive: Boolean,
): CameraCalibratedQualityTier =
    if (!debugQualityOverrideActive && mode == CameraQualityCalibrationMode.Video) {
        CameraCalibratedQualityTier.Default
    } else {
        requestedTier
    }

public fun debugGraphPresetPolicyLabel(graphPreset: CameraQualityGraphPreset): String =
    "${graphPreset.displayLabel}: ${graphPreset.requestedCameraXQuality}; ${graphPreset.useCases}"

public fun cameraQualityGateDependsOnLiveAnalysis(graphPreset: CameraQualityGraphPreset): Boolean = false

public fun cameraQualityCalibrationDeviceKey(): String =
    listOf(
        Build.MANUFACTURER,
        Build.MODEL,
        Build.DEVICE,
        Build.VERSION.SDK_INT.toString(),
    )
        .joinToString(separator = "/") { part -> part.ifBlank { "unknown" } }

public fun cameraQualityCalibrationLensToken(lensLabel: String): String =
    lensLabel
        .trim()
        .lowercase(Locale.US)
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { "unknown" }

public fun CameraQualityCalibrationRecord.selectorEligibility(
    defaultRecord: CameraQualityCalibrationRecord?,
): CameraQualitySelectorEligibility {
    if (status == CameraQualityCalibrationStatus.Failed) {
        return CameraQualitySelectorEligibility(
            eligible = false,
            reason = failureReason ?: "probe failed",
        )
    }
    if (!graphPreset.selectorCandidate) {
        return CameraQualitySelectorEligibility(
            eligible = false,
            reason = "diagnostics-only probe",
        )
    }
    if (tier != CameraCalibratedQualityTier.Default &&
        cameraQualityCalibrationLensToken(lensLabel) == "front"
    ) {
        return CameraQualitySelectorEligibility(
            eligible = false,
            reason = "front quality selector not calibrated in v1",
        )
    }
    if (probePurpose == CameraQualityCalibrationProbePurpose.OptionalDiagnostics ||
        mandatoryForReadiness == false
    ) {
        return CameraQualitySelectorEligibility(
            eligible = false,
            reason = "optional diagnostics only",
        )
    }
    if (!hasActualOutput) {
        return CameraQualitySelectorEligibility(
            eligible = false,
            reason = "no measured output",
        )
    }
    if (previewStable == false) {
        return CameraQualitySelectorEligibility(
            eligible = false,
            reason = "preview crop unstable",
        )
    }
    if (mandatoryForReadiness == true && calibrationArtifactCleanupSucceeded != true) {
        return CameraQualitySelectorEligibility(
            eligible = false,
            reason = if (calibrationArtifactCleanupSucceeded == false) {
                "cleanup failed"
            } else {
                "cleanup unproven"
            },
        )
    }
    if (gateStatusLabel?.contains("runtime failure", ignoreCase = true) == true) {
        return CameraQualitySelectorEligibility(
            eligible = false,
            reason = "gate runtime failure",
        )
    }
    if (graphPreset.selectorCandidate && calibrationGateRequiredForSelector == false) {
        return CameraQualitySelectorEligibility(
            eligible = false,
            reason = "gate not evaluated for selector",
        )
    }
    if (graphPreset.selectorCandidate &&
        gateCoverage == CameraQualityCalibrationGateCoverage.MetadataOnly
    ) {
        return CameraQualitySelectorEligibility(
            eligible = false,
            reason = "metadata-only gate evidence",
        )
    }
    if (gateSamplingProfileId.equals("mixed", ignoreCase = true)) {
        return CameraQualitySelectorEligibility(
            eligible = false,
            reason = "mixed gate sampling profiles",
        )
    }
    if (gateSamplingProfileSelectorEligible == false) {
        return CameraQualitySelectorEligibility(
            eligible = false,
            reason = "unsupported gate sampling profile",
        )
    }
    if (tier != CameraCalibratedQualityTier.Default && gateSamplingProfileId.isNullOrBlank()) {
        return CameraQualitySelectorEligibility(
            eligible = false,
            reason = "gate sampling profile unmeasured",
        )
    }
    if (tier != CameraCalibratedQualityTier.Default && !isDistinctFrom(defaultRecord)) {
        return CameraQualitySelectorEligibility(
            eligible = false,
            reason = "duplicate default",
        )
    }
    if (tier == CameraCalibratedQualityTier.High && !isMateriallyLargerThan(defaultRecord)) {
        return CameraQualitySelectorEligibility(
            eligible = false,
            reason = "not materially larger than default",
        )
    }
    return CameraQualitySelectorEligibility(
        eligible = true,
        reason = "distinct and stable",
    )
}

public fun CameraZoomCalibrationRecord.productEligibility(): CameraQualitySelectorEligibility {
    if (diagnosticsOnly || requestedZoomRatio > CAMERA_PRODUCT_MAX_ZOOM_RATIO + cameraZoomCalibrationTolerance) {
        return CameraQualitySelectorEligibility(
            eligible = false,
            reason = "diagnostics-only zoom stop",
        )
    }
    if (cameraZoomCalibrationProductStops().none { stop -> cameraZoomRatiosEquivalent(stop, requestedZoomRatio) }) {
        return CameraQualitySelectorEligibility(
            eligible = false,
            reason = "not a product zoom stop",
        )
    }
    if (status != CameraZoomCalibrationStatus.Verified) {
        return CameraQualitySelectorEligibility(
            eligible = false,
            reason = failureReason ?: status.displayLabel.lowercase(Locale.US),
        )
    }
    if (!cameraZoomRatioInRange(requestedZoomRatio, minZoomRatio, maxZoomRatio)) {
        return CameraQualitySelectorEligibility(
            eligible = false,
            reason = "outside CameraX zoom range",
        )
    }
    if (appliedZoomRatio == null || !cameraZoomRatiosEquivalent(appliedZoomRatio, requestedZoomRatio)) {
        return CameraQualitySelectorEligibility(
            eligible = false,
            reason = "CameraX applied a different zoom",
        )
    }
    if (observedZoomRatio == null || !cameraZoomRatiosEquivalent(observedZoomRatio, requestedZoomRatio)) {
        return CameraQualitySelectorEligibility(
            eligible = false,
            reason = "observed zoom did not settle",
        )
    }
    return CameraQualitySelectorEligibility(
        eligible = true,
        reason = "CameraX-applied zoom verified",
    )
}

public fun cameraQualityCalibrationNeedsFullTemporalRetry(
    record: CameraQualityCalibrationRecord,
): Boolean {
    if (record.mode != CameraQualityCalibrationMode.Video ||
        record.gateCoverage != CameraQualityCalibrationGateCoverage.FastSingleFrame ||
        record.fastProbeRetried == true
    ) {
        return false
    }
    if (record.status == CameraQualityCalibrationStatus.Failed) {
        return true
    }
    if (!record.hasActualOutput || (record.durationMillis ?: 0L) <= 0L) {
        return true
    }
    if (record.gateStatusLabel?.contains("runtime failure", ignoreCase = true) == true) {
        return true
    }
    if ((record.gateSampledFrameCount ?: 0) < 1 || (record.gateEvaluatedFrameCount ?: 0) < 1) {
        return true
    }
    if (record.gateSamplingProfileSelectorEligible != true) {
        return true
    }
    if (record.calibrationArtifactCleanupSucceeded != true) {
        return true
    }
    return false
}

private fun CameraQualityCalibrationProfile.toJsonPayloadBytes(): ByteArray =
    cameraQualityCalibrationJson
        .encodeToString(
            copy(
                records = records.sortedCalibrationRecords(),
                zoomRecords = zoomRecords.sortedZoomCalibrationRecords(),
            ),
        )
        .toByteArray(Charsets.UTF_8)

private fun List<CameraQualityCalibrationRecord>.sortedCalibrationRecords(): List<CameraQualityCalibrationRecord> =
    sortedWith(
        compareBy<CameraQualityCalibrationRecord> { it.mode.token }
            .thenBy { cameraQualityCalibrationLensToken(it.lensLabel) }
            .thenBy { it.tier.ordinal }
            .thenBy { it.graphPreset.ordinal },
    )

private fun List<CameraZoomCalibrationRecord>.sortedZoomCalibrationRecords(): List<CameraZoomCalibrationRecord> =
    sortedWith(
        compareBy<CameraZoomCalibrationRecord> { it.mode.token }
            .thenBy { cameraQualityCalibrationLensToken(it.lensLabel) }
            .thenBy { it.requestedZoomRatio },
    )

private fun CameraQualityCalibrationRecord.isDistinctFrom(
    defaultRecord: CameraQualityCalibrationRecord?,
): Boolean {
    val actual = actualOutputKey() ?: return false
    val defaultActual = defaultRecord?.actualOutputKey() ?: return true
    return actual != defaultActual
}

private fun CameraQualityCalibrationRecord.isMateriallyLargerThan(
    defaultRecord: CameraQualityCalibrationRecord?,
): Boolean {
    val width = finalWidth ?: return false
    val height = finalHeight ?: return false
    val defaultWidth = defaultRecord?.finalWidth ?: return true
    val defaultHeight = defaultRecord.finalHeight ?: return true
    val actualPixels = width.toLong() * height.toLong()
    val defaultPixels = defaultWidth.toLong() * defaultHeight.toLong()
    if (actualPixels <= 0L || defaultPixels <= 0L) {
        return false
    }
    return actualPixels * 100L >= defaultPixels * 110L
}

private fun CameraQualityCalibrationRecord.summaryFor(
    defaultRecord: CameraQualityCalibrationRecord?,
    mode: CameraQualityCalibrationMode,
): String {
    if (status == CameraQualityCalibrationStatus.Failed) {
        return "failed${failureReason?.let { reason -> " ($reason)" } ?: ""}; ${graphPreset.displayLabel}; policy $requestedPolicy"
    }
    val actual = if (finalWidth != null && finalHeight != null) {
        buildString {
            append(cameraDiagnosticResolutionLabel(finalWidth, finalHeight))
            finalRotationDegrees?.let { rotation -> append(", rotation $rotation") }
            durationMillis?.let { duration -> append(", duration ${cameraDiagnosticDurationLabel(duration)}") }
            fileSizeBytes?.let { size -> append(", size ${cameraDiagnosticByteCountLabel(size)}") }
            bitrateBitsPerSecond?.let { bitrate -> append(", bitrate ${cameraDiagnosticBitrateLabel(bitrate)}") }
        }
    } else {
        "actual output unavailable"
    }
    val bound = boundStreamLabel?.let { label -> ", bound $label" }.orEmpty()
    val preview = previewStreamLabel?.let { label -> ", preview $label" }.orEmpty()
    val gate = gateStatusLabel?.let { label ->
        ", gate $label${gateDurationMillis?.let { duration -> " in ${cameraDiagnosticDurationLabel(duration)}" } ?: ""}"
    }.orEmpty()
    val samplingProfile = gateSamplingProfileId?.let { profileId ->
        val ratio = gateSamplingRatio?.let { value -> " ratio ${String.format(Locale.US, "%.3f", value)}" }.orEmpty()
        val label = gateSamplingProfileLabel?.let { value -> " $value" }.orEmpty()
        val orientation = gateSamplingOrientation?.let { value -> " $value" }.orEmpty()
        ", gate profile $profileId$label$orientation$ratio"
    }.orEmpty()
    val gateSamples = if (gateSampledFrameCount != null || gateEvaluatedFrameCount != null) {
        ", gate samples ${gateSampledFrameCount ?: 0}/${gateEvaluatedFrameCount ?: 0}"
    } else {
        ""
    }
    val gateMode = calibrationGateMode?.let { mode ->
        ", gate mode $mode"
    }.orEmpty()
    val coverage = gateCoverage?.let { coverage ->
        ", gate coverage ${coverage.displayLabel}"
    }.orEmpty()
    val purpose = probePurpose?.let { purpose ->
        ", purpose ${purpose.displayLabel}"
    }.orEmpty()
    val retry = fastProbeRetried?.takeIf { retried -> retried }?.let {
        ", fast probe retried"
    }.orEmpty()
    val derivedFrom = derivedFromRecordKey?.let { key ->
        ", derived from $key"
    }.orEmpty()
    val cleanup = calibrationArtifactCleanupSucceeded?.let { cleanupSucceeded ->
        ", calibration cleanup ${if (cleanupSucceeded) "confirmed" else "failed"}${calibrationArtifactCleanupSummary?.let { summary -> ": $summary" } ?: ""}"
    }.orEmpty()
    val timing = calibrationStepDurationMillis?.let { duration ->
        buildString {
            append(", timing step")
            calibrationStepIndex?.let { index ->
                append(" $index")
                calibrationStepCount?.let { count -> append("/$count") }
            }
            append(" ${cameraQualityCalibrationMillisLabel(duration)}")
            calibrationBindWaitMillis?.let { bind -> append(", bind ${cameraQualityCalibrationMillisLabel(bind)}") }
            calibrationCaptureOrRecordMillis?.let { capture -> append(", capture/record ${cameraQualityCalibrationMillisLabel(capture)}") }
            calibrationRecordingTargetMillis?.let { target -> append(", target clip ${cameraQualityCalibrationMillisLabel(target)}") }
        }
    }.orEmpty()
    val inventory = cameraInventorySummary?.let { summary -> ", camera inventory $summary" }.orEmpty()
    val eligibility = selectorEligibility(defaultRecord)
    val selector = if (eligibility.eligible) {
        ", selector eligible"
    } else {
        ", selector hidden: ${eligibility.reason}"
    }
    val analysis = liveAnalysisIncluded?.let { included ->
        ", live analysis ${if (included) "included" else "omitted"}"
    }.orEmpty()
    val requested = requestedCameraXQuality?.let { quality -> ", requested $quality" }.orEmpty()
    return "${status.displayLabel}: $actual$bound$preview$gate$samplingProfile$gateSamples$gateMode$coverage$purpose$retry$derivedFrom$cleanup$timing$inventory$selector$analysis$requested; use cases ${useCasesBound ?: graphPreset.useCases}; policy $requestedPolicy"
}

private fun CameraZoomCalibrationRecord.summaryFor(): String {
    val minZoom = minZoomRatio
    val maxZoom = maxZoomRatio
    val range = if (minZoom != null && maxZoom != null) {
        ", CameraX range ${cameraZoomCalibrationRatioLabel(minZoom)}..${cameraZoomCalibrationRatioLabel(maxZoom)}"
    } else {
        ", CameraX range unavailable"
    }
    val applied = appliedZoomRatio?.let { value ->
        ", applied ${cameraZoomCalibrationRatioLabel(value)}"
    }.orEmpty()
    val observed = observedZoomRatio?.let { value ->
        ", observed ${cameraZoomCalibrationRatioLabel(value)}"
    }.orEmpty()
    val failure = failureReason?.let { reason ->
        ", reason $reason"
    }.orEmpty()
    val timing = calibrationStepDurationMillis?.let { duration ->
        buildString {
            append(", timing step")
            calibrationStepIndex?.let { index ->
                append(" $index")
                calibrationStepCount?.let { count -> append("/$count") }
            }
            append(" ${cameraQualityCalibrationMillisLabel(duration)}")
            calibrationBindWaitMillis?.let { bind -> append(", bind ${cameraQualityCalibrationMillisLabel(bind)}") }
        }
    }.orEmpty()
    val eligibility = productEligibility()
    val selector = if (eligibility.eligible) {
        ", product zoom eligible"
    } else {
        ", product zoom hidden: ${eligibility.reason}"
    }
    val scope = if (diagnosticsOnly) ", diagnostics-only" else ""
    return "${status.displayLabel}: requested ${cameraZoomCalibrationRatioLabel(requestedZoomRatio)}$range$applied$observed$failure$timing$scope$selector"
}

private fun videoBitrateBitsPerSecond(
    fileSizeBytes: Long?,
    durationMillis: Long?,
): Long? {
    val size = fileSizeBytes ?: return null
    val duration = durationMillis ?: return null
    if (size <= 0L || duration <= 0L) {
        return null
    }
    return (size * 8_000L) / duration
}

private fun cameraDiagnosticBitrateLabel(bitsPerSecond: Long): String {
    val megabits = bitsPerSecond / 1_000_000.0
    return if (megabits >= 1.0) {
        "${String.format(Locale.US, "%.1f", megabits)} Mbps"
    } else {
        "${bitsPerSecond / 1_000} Kbps"
    }
}

public fun cameraQualityCalibrationMillisLabel(durationMillis: Long): String =
    if (durationMillis < 1_000L) {
        "${durationMillis.coerceAtLeast(0L)} ms"
    } else {
        "${String.format(Locale.US, "%.1f", durationMillis.coerceAtLeast(0L) / 1_000.0)}s"
    }

private fun Properties.toEnvelopeBytes(): ByteArray =
    ByteArrayOutputStream().use { output ->
        store(output, "CameraX quality calibration profile")
        output.toByteArray()
    }

private fun ByteArray.base64Encode(): String = Base64.getEncoder().encodeToString(this)

private fun String.base64Decode(): ByteArray = Base64.getDecoder().decode(this)

private fun sha256Hex(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
    return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
}

private fun cameraQualityCalibrationDeviceInventoryHash(): String =
    sha256Hex(
        listOf(
            cameraQualityCalibrationDeviceKey(),
            Build.BOARD,
            Build.BRAND,
            Build.HARDWARE,
            Build.FINGERPRINT,
            "front-back-debug-camera-inventory-v1",
        ).joinToString(separator = "|") { part -> part.ifBlank { "unknown" } },
    ).take(16)

@Suppress("DEPRECATION")
private fun PackageManager.getPackageInfoCompat(packageName: String): android.content.pm.PackageInfo? =
    runCatching {
        if (Build.VERSION.SDK_INT >= 33) {
            getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            getPackageInfo(packageName, 0)
        }
    }.getOrNull()

@Suppress("DEPRECATION")
private fun android.content.pm.PackageInfo.longVersionCodeCompat(): Long =
    if (Build.VERSION.SDK_INT >= 28) {
        longVersionCode
    } else {
        versionCode.toLong()
    }
