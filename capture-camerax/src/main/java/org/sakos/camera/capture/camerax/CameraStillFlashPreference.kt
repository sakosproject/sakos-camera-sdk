package org.sakos.camera.capture.camerax

import android.content.Context
import androidx.camera.core.ImageCapture
import java.util.Locale

public enum class CameraStillFlashMode(
    val displayLabel: String,
    val imageCaptureFlashMode: Int,
    val preferenceToken: String,
) {
    Off("OFF", ImageCapture.FLASH_MODE_OFF, "off"),
    Auto("AUTO", ImageCapture.FLASH_MODE_AUTO, "auto"),
    On("ON", ImageCapture.FLASH_MODE_ON, "on"),
    ;

    fun next(): CameraStillFlashMode = when (this) {
        Off -> Auto
        Auto -> On
        On -> Off
    }
}

public object CameraStillFlashPreferenceStore {
    private const val preferencesName = "sakos_camera_preferences"
    private const val stillFlashModeKey = "still_flash_mode"

    fun read(context: Context): CameraStillFlashMode {
        val preferences = context.applicationContext.getSharedPreferences(
            preferencesName,
            Context.MODE_PRIVATE,
        )
        val storedToken = runCatching {
            preferences.getString(stillFlashModeKey, null)
        }.getOrNull()
        return cameraStillFlashModeFromStoredToken(storedToken)
    }

    fun write(
        context: Context,
        mode: CameraStillFlashMode,
    ) {
        context.applicationContext
            .getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit()
            .putString(stillFlashModeKey, mode.preferenceToken)
            .apply()
    }
}

public fun cameraStillFlashModeFromStoredToken(storedToken: String?): CameraStillFlashMode =
    when (storedToken?.trim()?.lowercase(Locale.US)) {
        CameraStillFlashMode.Auto.preferenceToken -> CameraStillFlashMode.Auto
        CameraStillFlashMode.On.preferenceToken -> CameraStillFlashMode.On
        CameraStillFlashMode.Off.preferenceToken -> CameraStillFlashMode.Off
        else -> CameraStillFlashMode.Off
    }
