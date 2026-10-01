package org.sakos.camera.capture.video

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import java.io.File
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive
import org.sakos.camera.safety.core.SafetyDecision
import org.sakos.camera.safety.core.SafetyFailureReason
import org.sakos.camera.safety.opennsfw2.OpenNsfw2BitmapRuntime
import org.sakos.camera.safety.opennsfw2.SakosCompatibleCropRole

/** Owns a retriever for one finalized private clip; no URI or corpus intake. */
class AndroidVideoFrameDecoder(file: File) : VideoFrameDecoder<Bitmap> {
    private val retriever = MediaMetadataRetriever()
    private var closed = false
    val durationMillis: Long

    init {
        try {
            require(file.isFile && file.length() > 0) { "Finalized clip is empty or missing." }
            retriever.setDataSource(file.absolutePath)
            durationMillis = requireNotNull(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)).toLong()
            require(durationMillis > 0) { "Clip duration is invalid." }
        } catch (failure: Throwable) {
            retriever.release()
            throw failure
        }
    }

    override suspend fun decode(sample: VideoGateSamplePlan): VideoDecodedFrame<Bitmap> {
        coroutineContext.ensureActive()
        check(!closed) { "Decoder is closed." }
        require(sample.sampledAtMillis in 0..durationMillis) { "Timestamp is outside this clip." }
        val bitmap = requireNotNull(retriever.getFrameAtTime(
            Math.multiplyExact(sample.sampledAtMillis, 1_000L), MediaMetadataRetriever.OPTION_CLOSEST,
        )) { "Could not decode planned frame." }
        return object : VideoDecodedFrame<Bitmap> {
            override val value = bitmap
            override fun close() { if (!bitmap.isRecycled) bitmap.recycle() }
        }
    }

    override fun close() {
        if (!closed) { closed = true; retriever.release() }
    }
}

/** Applies the bundled fixed-view runtime; ownership of the shared runtime stays with the host. */
class OpenNsfw2VideoFrameEvaluator(private val runtime: OpenNsfw2BitmapRuntime) : VideoFrameEvaluator<Bitmap> {
    override suspend fun evaluate(frame: Bitmap, sample: VideoGateSamplePlan): VideoFrameEvaluation {
        coroutineContext.ensureActive()
        return try {
            val result = runtime.evaluate(frame)
            coroutineContext.ensureActive()
            val representative = result.evaluatedViews.first { it.cropWindow.label == result.representativeViewLabel }
            VideoFrameEvaluation.Decision(
                if (result.checkResult.isSafe) SafetyDecision.Allow else SafetyDecision.Block,
                result.policyNsfwEvidence,
                if (representative.cropWindow.role == SakosCompatibleCropRole.Context)
                    VideoFrameEvidenceKind.Context else VideoFrameEvidenceKind.Crop,
            )
        } catch (failure: kotlinx.coroutines.CancellationException) {
            throw failure
        } catch (_: Exception) {
            VideoFrameEvaluation.Failure(SafetyFailureReason.InferenceFailure)
        }
    }
}

/** Opens only the supplied session's private output and uses the decoded container duration. */
class AndroidVideoReviewBridge(
    private val store: VideoPrivateStagingFileStore,
    private val pipeline: ManagedVideoCapturePipeline,
) {
    suspend fun review(
        session: VideoStagingSession,
        runtime: OpenNsfw2BitmapRuntime,
        promoter: ApprovedVideoPromoter,
    ): ManagedVideoReviewResult {
        val decoder = try { AndroidVideoFrameDecoder(store.recordingOutputFile(session.id)) }
        catch (_: Exception) {
            pipeline.discard(session)
            return ManagedVideoReviewResult.Rejected("Clip could not be opened; cleanup requested.")
        }
        return pipeline.reviewPrepared(session, decoder.durationMillis, decoder, OpenNsfw2VideoFrameEvaluator(runtime), promoter)
    }
}
