package org.sakos.camera.capture.camerax

import android.util.Rational
import androidx.camera.core.ViewPort
import androidx.camera.view.PreviewView

public enum class CameraPreviewFraming(
    val diagnosticsLabel: String,
    val previewViewScaleType: PreviewView.ScaleType,
    val viewPortScaleType: Int,
    val viewfinderAspectRatio: Float?,
) {
    FillCrop(
        diagnosticsLabel = "fill_crop",
        previewViewScaleType = PreviewView.ScaleType.FILL_START,
        viewPortScaleType = ViewPort.FILL_CENTER,
        viewfinderAspectRatio = null,
    ),
    FitFourThree(
        diagnosticsLabel = "fit_4_3",
        previewViewScaleType = PreviewView.ScaleType.FIT_CENTER,
        viewPortScaleType = ViewPort.FIT,
        viewfinderAspectRatio = 3f / 4f,
    ),
}

public fun cameraPreviewViewPortAspectRatio(
    framing: CameraPreviewFraming,
    displayWidthPixels: Int,
    displayHeightPixels: Int,
): Rational = when (framing) {
    CameraPreviewFraming.FillCrop -> Rational(
        displayWidthPixels.coerceAtLeast(1),
        displayHeightPixels.coerceAtLeast(1),
    )
    CameraPreviewFraming.FitFourThree -> Rational(3, 4)
}
