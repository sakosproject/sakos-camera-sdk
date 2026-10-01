package org.sakos.camera.capture.camerax

import android.content.Context
import android.graphics.Bitmap
import android.util.Rational
import androidx.camera.core.*
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import java.security.MessageDigest
import java.util.concurrent.Executor

/** Advertised facts only. Successful binding/capture/gate/cleanup is a separate calibration fact. */
data class CameraAdvertisedCapabilities(
    val lens: String, val cameraId: String, val flash: Boolean,
    val minZoom: Float?, val maxZoom: Float?, val videoQualities: List<String>,
)

object CameraCapabilityDiscovery {
    @androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
    fun discover(provider: ProcessCameraProvider): List<CameraAdvertisedCapabilities> = provider.availableCameraInfos.map { info ->
        val zoom = info.zoomState.value
        CameraAdvertisedCapabilities(when (info.lensFacing) {
            CameraSelector.LENS_FACING_BACK -> "back"
            CameraSelector.LENS_FACING_FRONT -> "front"
            else -> "external"
        }, androidx.camera.camera2.interop.Camera2CameraInfo.from(info).cameraId, info.hasFlashUnit(), zoom?.minZoomRatio, zoom?.maxZoomRatio,
            Recorder.getVideoCapabilities(info).getSupportedQualities(DynamicRange.SDR).map(::cameraDiagnosticVideoQualityLabel))
    }
    fun inventoryHash(facts: List<CameraAdvertisedCapabilities>): String = MessageDigest.getInstance("SHA-256")
        .digest(facts.sortedWith(compareBy<CameraAdvertisedCapabilities> { it.lens }.thenBy { it.cameraId })
            .joinToString("|") { "${it.lens}:${it.cameraId}:${it.flash}:${it.minZoom}:${it.maxZoom}:${it.videoQualities.sorted()}" }.toByteArray())
        .joinToString("") { "%02x".format(it) }
}

data class CameraGraphBinding(
    val camera: Camera, val preview: Preview, val analysis: ImageAnalysis?,
    val photo: ImageCapture?, val video: VideoCapture<Recorder>?, val preset: CameraQualityGraphPreset,
) {
    fun updateOutputRotation(rotation: Int) { photo?.targetRotation = rotation; video?.targetRotation = rotation; analysis?.targetRotation = rotation }
    fun closeAnalysis() { analysis?.clearAnalyzer() }
    fun unbind(provider: ProcessCameraProvider) { closeAnalysis(); provider.unbind(*listOfNotNull<UseCase>(preview, analysis, photo, video).toTypedArray()) }
}

/** Source CameraActivity graph behavior, including independent high still viewport and closing analyzer. Main thread only. */
object CameraGraphTooling {
    fun selectedPreset(profile: CameraQualityCalibrationProfile, environment: CameraQualityCalibrationEnvironment,
        mode: CameraQualityCalibrationMode, lens: String, requested: CameraCalibratedQualityTier): CameraQualityGraphPreset? {
        if (!profile.mandatoryReadiness(environment).ready) return null
        val effective = profile.calibratedTierFor(mode, lens, requested)
        val record = profile.recordFor(mode, lens, effective) ?: return null
        val default = profile.recordFor(mode, lens, CameraCalibratedQualityTier.Default)
        return if (record.selectorEligibility(default).eligible) record.graphPreset else null
    }
    fun bind(provider: ProcessCameraProvider, owner: LifecycleOwner, previewView: PreviewView, lens: String,
        preset: CameraQualityGraphPreset, outputRotation: Int, executor: Executor, flashMode: Int = ImageCapture.FLASH_MODE_OFF, framing: CameraPreviewFraming = CameraPreviewFraming.FillCrop): CameraGraphBinding {
        val selector = CameraSelector.Builder().requireLensFacing(if (lens == "front") CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK).build()
        require(lens == "front" || lens == "back")
        val preview = Preview.Builder().setTargetRotation(previewView.display?.rotation ?: android.view.Surface.ROTATION_0).build().also { it.surfaceProvider = previewView.surfaceProvider }
        previewView.scaleType = framing.previewViewScaleType
        val metrics = previewView.context.resources.displayMetrics
        val viewport = ViewPort.Builder(cameraPreviewViewPortAspectRatio(framing, metrics.widthPixels, metrics.heightPixels), previewView.display?.rotation ?: android.view.Surface.ROTATION_0)
            .setScaleType(framing.viewPortScaleType).build()
        val analysis = if (preset.includesLiveAnalysis) ImageAnalysis.Builder().setTargetRotation(outputRotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also { it.setAnalyzer(executor) { frame -> frame.close() } } else null
        val group = UseCaseGroup.Builder().setViewPort(viewport).addUseCase(preview)
        analysis?.let { group.addUseCase(it) }
        provider.unbindAll()
        try {
            if (preset.mode == CameraQualityCalibrationMode.Photo) {
                val high = preset == CameraQualityGraphPreset.PhotoHighProbe || preset == CameraQualityGraphPreset.PhotoHighStillProbeV2
                val builder = ImageCapture.Builder().setTargetRotation(outputRotation).setFlashMode(flashMode)
                    .setCaptureMode(if (high) ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY else ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                if (high) builder.setResolutionSelector(ResolutionSelector.Builder()
                    .setAllowedResolutionMode(ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE).build())
                val photo = builder.build()
                val camera = if (preset == CameraQualityGraphPreset.PhotoHighStillProbeV2) {
                    provider.bindToLifecycle(owner, selector, group.build())
                    provider.bindToLifecycle(owner, selector, photo)
                } else provider.bindToLifecycle(owner, selector, group.addUseCase(photo).build())
                return CameraGraphBinding(camera, preview, analysis, photo, null, preset)
            }
            val recorder = Recorder.Builder().apply {
                when (preset) {
                    CameraQualityGraphPreset.VideoLow -> setQualitySelector(QualitySelector.from(Quality.SD, FallbackStrategy.higherQualityOrLowerThan(Quality.SD)))
                    CameraQualityGraphPreset.VideoHighFhdProbe -> setQualitySelector(QualitySelector.from(Quality.FHD, FallbackStrategy.lowerQualityOrHigherThan(Quality.FHD)))
                    CameraQualityGraphPreset.VideoUhdProbe -> setQualitySelector(QualitySelector.from(Quality.UHD, FallbackStrategy.lowerQualityOrHigherThan(Quality.UHD)))
                    else -> Unit
                }
            }.build()
            val video = VideoCapture.Builder(recorder).setTargetRotation(outputRotation).build()
            val camera = provider.bindToLifecycle(owner, selector, group.addUseCase(video).build())
            return CameraGraphBinding(camera, preview, analysis, null, video, preset)
        } catch (error: Exception) { analysis?.clearAnalyzer(); provider.unbindAll(); throw error }
    }

    /** Caller owns the returned bitmap; consumes/recycles the old bitmap only when scaled. */
    fun lowPhoto(bitmap: Bitmap): Bitmap {
        val pixels = bitmap.width.toLong() * bitmap.height
        if (pixels <= CAMERA_QUALITY_CALIBRATION_LOW_PHOTO_MAX_PIXELS) return bitmap
        val scale = kotlin.math.sqrt(CAMERA_QUALITY_CALIBRATION_LOW_PHOTO_MAX_PIXELS.toDouble() / pixels)
        return Bitmap.createScaledBitmap(bitmap, kotlin.math.round(bitmap.width * scale).toInt().coerceAtLeast(1),
            kotlin.math.round(bitmap.height * scale).toInt().coerceAtLeast(1), true).also { if (it !== bitmap) bitmap.recycle() }
    }
}

/** Source sensor orientation mapping; unknown input leaves the caller's previous rotation intact. */
object CameraOutputRotation {
    fun fromOrientationDegrees(degrees: Int): Int? {
        if (degrees == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) return null
        require(degrees in 0..359)
        return when (degrees) {
            in 45..134 -> android.view.Surface.ROTATION_270
            in 135..224 -> android.view.Surface.ROTATION_180
            in 225..314 -> android.view.Surface.ROTATION_90
            else -> android.view.Surface.ROTATION_0
        }
    }
}
