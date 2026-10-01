package org.sakos.camera.capture.video

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.sakos.camera.safety.core.*
import org.sakos.camera.safety.opennsfw2.OpenNsfw2ModelPreflight

/** Encodes solid YUV patterns locally; never imports or reads external media. */
@RunWith(AndroidJUnit4::class)
class SyntheticVideoDecoderTest {
    @Test fun encodedPatternsDecodeCloseReviewPromoteAndRecover() = runBlocking<Unit> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = AndroidVideoPrivateStagingStore(context)
        val sessions = VideoStagingSessionManager(store)
        sessions.recoverAbandonedSessions()
        val session = (sessions.startRecording(0) as VideoRecordingStartResult.Started).session
        val file = store.recordingOutputFile(session.id)
        encodePatterns(file)
        val decoder = AndroidVideoFrameDecoder(file)
        assertTrue(decoder.durationMillis > 0)
        val sample = VideoTemporalSamplePlanner.buildBasePlan(decoder.durationMillis).baseSamples.first()
        val decoded = decoder.decode(sample)
        assertTrue(decoded.value.width > 0)
        decoded.close()
        assertTrue(decoded.value.isRecycled)
        decoder.close()
        decoder.close()
        sessions.markReviewing(session.id)
        val reviewDecoder = AndroidVideoFrameDecoder(file)
        var promotedId: VideoStagingSessionId? = null
        val result = ManagedVideoCapturePipeline(sessions).reviewPrepared(session, reviewDecoder.durationMillis, reviewDecoder,
            VideoFrameEvaluator { _, _ -> VideoFrameEvaluation.Decision(SafetyDecision.Allow, .1f, VideoFrameEvidenceKind.Context) },
            ApprovedVideoPromoter { promotedId = it.id })
        assertTrue(result is ManagedVideoReviewResult.Promoted)
        assertEquals(session.id, promotedId)
        assertFalse(file.exists())
        val abandoned = (sessions.startRecording(1) as VideoRecordingStartResult.Started).session
        store.recordingOutputFile(abandoned.id).writeBytes(byteArrayOf(1, 2, 3))
        assertTrue(VideoStagingSessionManager(AndroidVideoPrivateStagingStore(context)).recoverAbandonedSessions().removedSessionIds.contains(abandoned.id))
        assertFalse(store.recordingOutputFile(abandoned.id).exists())
        // recordingOutputFile creates a directory; purge the synthetic empty orphan as well.
        sessions.recoverAbandonedSessions()
    }

    @Test fun simulatedTemporalAllowSavesStandaloneLibraryAndPrivatePlaybackLease() = runBlocking<Unit> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = AndroidVideoPrivateStagingStore(context); val sessions = VideoStagingSessionManager(store)
        sessions.recoverAbandonedSessions()
        val session = (sessions.startRecording(0) as VideoRecordingStartResult.Started).session
        val file = store.recordingOutputFile(session.id); encodePatterns(file); sessions.markReviewing(session.id)
        val library = PrivateReviewedMediaLibrary(File(context.noBackupFilesDir, "sakos-synthetic-reviewed-library"), OpenNsfw2ModelPreflight.configuration)
        val decoder = AndroidVideoFrameDecoder(file)
        val result = ManagedVideoCapturePipeline(sessions).reviewPreparedBound(session, decoder.durationMillis, decoder,
            VideoFrameEvaluator { _, _ -> VideoFrameEvaluation.Decision(SafetyDecision.Allow, .01f, VideoFrameEvidenceKind.Context) }) { promoting, review ->
            assertEquals(VideoTemporalReviewDecision.Allow, review.decision); assertEquals(session.id, promoting.id)
            val capture = SafetyCaptureContext(SafetyCaptureId("synthetic:${session.id.value}"), 0, 64, 64, 0, false)
            val decision = SafetyEvaluationOutcome.Decision(capture.captureId, SafetyEvaluationReceiptId("simulated-temporal-allow"), OpenNsfw2ModelPreflight.configuration, SafetyDecision.Allow)
            library.save(ReviewedMediaKind.Video, capture, requireNotNull(decision.approvalForManagedCapture(capture))) { output -> file.inputStream().use { it.copyTo(output) } }
        }
        assertTrue(result is ManagedVideoReviewResult.Promoted); assertFalse(file.exists())
        val item = library.items().first()
        val client = org.sakos.camera.capture.camerax.AndroidReviewedMediaClient(context, library)
        val bitmap = client.decodePreview(item, 64); assertNotNull(bitmap); bitmap?.recycle()
        val lease = client.playback(item); val playback = File(requireNotNull(lease.uri.path))
        assertTrue(playback.exists()); lease.close(); assertFalse(playback.exists())
        var authorized = false; var copied = false
        val export = ReviewedMediaExporter(library).export(listOf(item), { authorized = true; false }) { copied = true; error("No destination without authorization") }
        assertTrue(authorized); assertFalse(copied); assertEquals(1, export.denied)
        library.items().forEach { library.delete(it) }
    }

    @Test fun missingAndMalformedClipsFailClosed() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.noBackupFilesDir, "synthetic-invalid.mp4")
        try {
            file.writeBytes(byteArrayOf(1, 2, 3))
            assertThrows(Exception::class.java) { AndroidVideoFrameDecoder(file) }
        } finally { file.delete() }
        assertThrows(Exception::class.java) { AndroidVideoFrameDecoder(file) }
    }

    @Test fun missingFinalizedInputRequestsCleanupWithoutSaving() = runBlocking<Unit> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = AndroidVideoPrivateStagingStore(context); val sessions = VideoStagingSessionManager(store)
        sessions.recoverAbandonedSessions()
        val session = (sessions.startRecording(0) as VideoRecordingStartResult.Started).session
        sessions.markReviewing(session.id)
        val library = PrivateReviewedMediaLibrary(File(context.noBackupFilesDir, "sakos-synthetic-missing-library"), OpenNsfw2ModelPreflight.configuration)
        org.sakos.camera.safety.opennsfw2.OpenNsfw2BitmapRuntime.open(context).use { runtime ->
            val result = AndroidVideoReviewBridge(store, ManagedVideoCapturePipeline(sessions)).reviewInto(session, runtime, library, false)
            assertTrue(result is ManagedVideoReviewResult.Rejected)
            assertTrue(library.items().isEmpty())
            assertFalse(store.recordingOutputFile(session.id).exists())
        }
        sessions.recoverAbandonedSessions()
    }

    @Test fun twoPlaybackClientsRetainLiveLeasesAndRetryOrphanCleanup() = runBlocking<Unit> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val library = PrivateReviewedMediaLibrary(File(context.noBackupFilesDir, "sakos-synthetic-playback-owners"), OpenNsfw2ModelPreflight.configuration)
        val capture = SafetyCaptureContext(SafetyCaptureId("synthetic-lease:${java.util.UUID.randomUUID()}"), 0, 64, 64, 0, false)
        val decision = SafetyEvaluationOutcome.Decision(capture.captureId, SafetyEvaluationReceiptId("simulated-lease-allow"), OpenNsfw2ModelPreflight.configuration, SafetyDecision.Allow)
        val input = File(context.noBackupFilesDir, "synthetic-lease-input.mp4")
        encodePatterns(input)
        val item = library.save(ReviewedMediaKind.Video, capture, requireNotNull(decision.approvalForManagedCapture(capture))) { output -> input.inputStream().use { it.copyTo(output) } }
        val first = org.sakos.camera.capture.camerax.AndroidReviewedMediaClient(context, library)
        val firstLease = first.playback(item); val firstFile = File(requireNotNull(firstLease.uri.path))
        try {
            val second = org.sakos.camera.capture.camerax.AndroidReviewedMediaClient(context, library)
            assertTrue(firstFile.exists())
            val secondLease = second.playback(item); val secondFile = File(requireNotNull(secondLease.uri.path))
            try {
                val orphan = File(firstFile.parentFile, "synthetic-orphan.mp4").apply { writeBytes(byteArrayOf(1)) }
                second.retryCleanup(); assertFalse(orphan.exists()); assertTrue(firstFile.exists()); assertTrue(secondFile.exists())
                val blocked = File(firstFile.parentFile, "synthetic-cleanup-failure.mp4").apply { mkdir() }
                val child = File(blocked, "synthetic-partial").apply { writeBytes(byteArrayOf(1)) }
                try { assertThrows(IllegalStateException::class.java) { second.retryCleanup() }; assertTrue(firstFile.exists()) }
                finally { child.delete(); blocked.delete() }
                second.retryCleanup(); assertTrue(firstFile.exists()); assertTrue(secondFile.exists())
                // Synthetic filesystem obstruction exercises retryable lease
                // cleanup without device-gallery or private input access.
                check(firstFile.delete()); check(firstFile.mkdir())
                val obstruction = File(firstFile, "synthetic-obstruction").apply { writeBytes(byteArrayOf(1)) }
                try {
                    assertThrows(IllegalStateException::class.java) { firstLease.close() }
                    org.sakos.camera.capture.camerax.AndroidReviewedMediaClient(context, library).retryCleanup()
                    assertTrue(firstFile.isDirectory); assertTrue(secondFile.exists())
                } finally { obstruction.delete(); firstFile.delete() }
                firstLease.close(); assertFalse(firstFile.exists()); assertTrue(secondFile.exists())
            } finally { secondLease.close() }
            assertFalse(secondFile.exists())
        } finally { firstLease.close(); input.delete(); library.delete(item) }
    }

    private fun encodePatterns(file: File) {
        val codec = MediaCodec.createEncoderByType("video/avc")
        val muxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var started = false
        try {
            val format = MediaFormat.createVideoFormat("video/avc", 64, 64).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, 10)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            var inputFrames = 0
            var track = -1
            var finished = false
            val info = MediaCodec.BufferInfo()
            val deadline = android.os.SystemClock.elapsedRealtime() + 30_000
            while (!finished && android.os.SystemClock.elapsedRealtime() < deadline) {
                if (inputFrames <= 20) {
                    val input = codec.dequeueInputBuffer(10_000)
                    if (input >= 0) {
                        val buffer = requireNotNull(codec.getInputBuffer(input))
                        buffer.clear()
                        if (inputFrames == 20) {
                            codec.queueInputBuffer(input, 0, 0, inputFrames * 100_000L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        } else {
                            buffer.put(ByteArray(64 * 64) { (40 + inputFrames * 4).toByte() })
                            buffer.put(ByteArray(64 * 64 / 2) { 128.toByte() })
                            codec.queueInputBuffer(input, 0, 64 * 64 * 3 / 2, inputFrames * 100_000L, 0)
                        }
                        inputFrames++
                    }
                }
                val output = codec.dequeueOutputBuffer(info, 10_000)
                if (output == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    track = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                    started = true
                } else if (output >= 0) {
                    val buffer = requireNotNull(codec.getOutputBuffer(output))
                    if (info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                        check(started)
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        muxer.writeSampleData(track, buffer, info)
                    }
                    finished = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    codec.releaseOutputBuffer(output, false)
                }
            }
            check(finished) { "Synthetic encoder timed out." }
        } finally {
            codec.release()
            if (started) muxer.stop()
            muxer.release()
        }
    }
}
