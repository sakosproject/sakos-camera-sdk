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
import org.sakos.camera.safety.core.SafetyDecision

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

    @Test fun missingAndMalformedClipsFailClosed() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.noBackupFilesDir, "synthetic-invalid.mp4")
        try {
            file.writeBytes(byteArrayOf(1, 2, 3))
            assertThrows(Exception::class.java) { AndroidVideoFrameDecoder(file) }
        } finally { file.delete() }
        assertThrows(Exception::class.java) { AndroidVideoFrameDecoder(file) }
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
