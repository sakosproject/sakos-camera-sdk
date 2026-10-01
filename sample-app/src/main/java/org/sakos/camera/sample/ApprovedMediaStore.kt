package org.sakos.camera.sample

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.runBlocking
import org.sakos.camera.capture.camerax.AndroidReviewedMediaLibrary
import org.sakos.camera.safety.core.*
import org.sakos.camera.safety.opennsfw2.OpenNsfw2ModelPreflight

/** Minimal sample adapter over the reusable standalone approved library. */
internal class ApprovedMediaStore(context: Context) {
    private val androidLibrary = AndroidReviewedMediaLibrary(context, OpenNsfw2ModelPreflight.configuration)
    val library get() = androidLibrary.library
    fun items() = library.items()
    fun savePhoto(bitmap: Bitmap, capture: SafetyCaptureContext, approval: ManagedCaptureApproval, stillActive: () -> Boolean = { true }) =
        runBlocking { androidLibrary.savePhoto(bitmap, capture, approval, stillActive) }
}
