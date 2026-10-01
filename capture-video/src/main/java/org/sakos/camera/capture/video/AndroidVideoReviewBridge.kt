package org.sakos.camera.capture.video

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import java.io.File
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive
import org.sakos.camera.safety.core.approvalForManagedCapture
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

/** Source base context sweep plus selected full spatial strategy on escalation. Runtime remains host-owned. */
class OpenNsfw2VideoFrameEvaluator(private val runtime: OpenNsfw2BitmapRuntime) : VideoFrameEvaluator<Bitmap> {
    override suspend fun evaluate(frame: Bitmap, sample: VideoGateSamplePlan): VideoFrameEvaluation {
        coroutineContext.ensureActive()
        return try {
            val result = if (sample.pass == VideoGateSamplePass.Base) runtime.evaluateVideoBaseSweep(frame) else runtime.evaluate(frame)
            coroutineContext.ensureActive()
            val representative = result.evaluatedViews.first { it.cropWindow.label == result.representativeViewLabel && it.cropWindow.boundsLabel() == result.representativeViewBounds }
            VideoFrameEvaluation.Decision(
                if (result.checkResult.isSafe) SafetyDecision.Allow else SafetyDecision.Block,
                result.policyNsfwEvidence,
                when {
                    representative.cropWindow.role == SakosCompatibleCropRole.Context -> VideoFrameEvidenceKind.Context
                    result.policyTriggerLabel.contains("near-floor lateral", true) -> VideoFrameEvidenceKind.NearFloorLateral
                    result.policyTriggerLabel.contains("extremely unsafe", true) -> VideoFrameEvidenceKind.IsolatedExtreme
                    result.policyTriggerLabel.contains("multiple detection", true) || result.supportiveDetectionCount >= 2 -> VideoFrameEvidenceKind.CorroboratedDetection
                    else -> VideoFrameEvidenceKind.Crop
                },
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
    /** Recommended standalone save path. Binds the temporal Allow to this finalized session and runtime configuration. */
    suspend fun reviewInto(
        session: VideoStagingSession,
        runtime: OpenNsfw2BitmapRuntime,
        library: org.sakos.camera.safety.core.PrivateReviewedMediaLibrary,
        frontFacing: Boolean,
        stillActive: () -> Boolean = { true },
    ): ManagedVideoReviewResult {
        val file = store.recordingOutputFile(session.id)
        val expectedHash = try { file.inputStream().use { input ->
            val digest = java.security.MessageDigest.getInstance("SHA-256"); val buffer = ByteArray(8192)
            while (true) { coroutineContext.ensureActive(); val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
            digest.digest()
        } } catch (failure: kotlinx.coroutines.CancellationException) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { pipeline.discard(session) }
            throw failure
        } catch (_: Exception) {
            pipeline.discard(session)
            return ManagedVideoReviewResult.Rejected("Clip unavailable; cleanup requested.")
        }
        val decoder = try { AndroidVideoFrameDecoder(file) }
        catch (_: Exception) { pipeline.discard(session); return ManagedVideoReviewResult.Rejected("Clip unavailable; cleanup requested.") }
        return pipeline.reviewPreparedBound(session, decoder.durationMillis, decoder, OpenNsfw2VideoFrameEvaluator(runtime)) { promoting, review ->
            require(promoting.id == session.id && promoting.state == VideoStagingState.Promoting && review.decision == VideoTemporalReviewDecision.Allow)
            coroutineContext.ensureActive(); check(stillActive())
            val facts = requireNotNull(org.sakos.camera.capture.camerax.readCameraDiagnosticVideoFileFacts(file))
            val capture = org.sakos.camera.safety.core.SafetyCaptureContext(
                org.sakos.camera.safety.core.SafetyCaptureId("video:${session.id.value}"), session.createdAtEpochMillis,
                requireNotNull(facts.width), requireNotNull(facts.height), facts.rotationDegrees ?: 0, frontFacing)
            val outcome = org.sakos.camera.safety.core.SafetyEvaluationOutcome.Decision(capture.captureId,
                org.sakos.camera.safety.core.SafetyEvaluationReceiptId("${session.id.value}:temporal:${runtime.strategy.id}"), runtime.configuration, SafetyDecision.Allow)
            val approval = requireNotNull(outcome.approvalForManagedCapture(capture))
            val reviewContext = coroutineContext
            library.save(org.sakos.camera.safety.core.ReviewedMediaKind.Video, capture, approval, stillActive) { output ->
                file.inputStream().use { input ->
                    val digest = java.security.MessageDigest.getInstance("SHA-256"); val buffer = ByteArray(8192)
                    while (true) { reviewContext.ensureActive(); check(stillActive()); val count = input.read(buffer); if (count < 0) break
                        digest.update(buffer, 0, count); output.write(buffer, 0, count) }
                    check(digest.digest().contentEquals(expectedHash)) { "Staged bytes changed after review." }
                }
            }
        }
    }
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
