package org.sakos.camera.capture.camerax

import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import java.util.concurrent.Executor
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.sakos.camera.safety.core.SafetyCaptureContext

sealed interface ManagedPhotoCaptureCallbackResult {
    data class Reviewed(val result: ManagedPhotoResult) : ManagedPhotoCaptureCallbackResult
    data class CaptureFailure(val error: ImageCaptureException) : ManagedPhotoCaptureCallbackResult
    data class PipelineFailure(val error: Throwable) : ManagedPhotoCaptureCallbackResult
}

fun interface ManagedPhotoCaptureListener {
    fun onResult(result: ManagedPhotoCaptureCallbackResult)
}

/**
 * CameraX's in-memory callback bridge. [ManagedPhotoReviewPipeline] closes every delivered
 * [ImageProxy], including conversion/evaluation/output failures; this adapter never writes media.
 */
class ManagedPhotoCaptureCallback<Input : Any>(
    private val pipeline: ManagedPhotoReviewPipeline<Input>,
    private val capture: SafetyCaptureContext,
    private val convert: (ImageProxy) -> Input,
    private val listener: ManagedPhotoCaptureListener,
) : ImageCapture.OnImageCapturedCallback() {
    override fun onCaptureSuccess(image: ImageProxy) {
        val review: suspend () -> ManagedPhotoResult = {
            pipeline.reviewImageProxy(image, capture, convert)
        }
        review.startCoroutine(
            object : Continuation<ManagedPhotoResult> {
                override val context = EmptyCoroutineContext

                override fun resumeWith(result: Result<ManagedPhotoResult>) {
                    listener.onResult(
                        result.fold(
                            onSuccess = { ManagedPhotoCaptureCallbackResult.Reviewed(it) },
                            onFailure = { ManagedPhotoCaptureCallbackResult.PipelineFailure(it) },
                        ),
                    )
                }
            },
        )
    }

    override fun onError(exception: ImageCaptureException) {
        listener.onResult(ManagedPhotoCaptureCallbackResult.CaptureFailure(exception))
    }
}

/** Invokes CameraX in-memory capture with a managed callback; the host still owns camera binding. */
class ManagedPhotoCaptureLauncher(
    private val imageCapture: ImageCapture,
) {
    fun capture(executor: Executor, callback: ImageCapture.OnImageCapturedCallback) {
        imageCapture.takePicture(executor, callback)
    }
}
