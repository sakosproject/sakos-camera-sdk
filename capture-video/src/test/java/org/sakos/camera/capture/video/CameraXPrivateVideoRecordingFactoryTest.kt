package org.sakos.camera.capture.video

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CameraXPrivateVideoRecordingFactoryTest {
    private val root = createTempDirectory("sakos-video-camera-").toFile()

    @AfterTest
    fun removeTemporaryRoot() {
        root.deleteRecursively()
    }

    @Test
    fun preparesCameraXFileOutputOnlyForRecordingSession() {
        val store = AndroidVideoPrivateStagingStore(root)
        val factory = CameraXPrivateVideoRecordingFactory(store)
        val recording = VideoStagingSession(VideoStagingSessionId("recording"), VideoStagingState.Recording, 1L)

        factory.outputOptions(recording)

        assertTrue(requireNotNull(File(root, "content/${recording.id.value}/recording.mp4").parentFile).isDirectory)
    }

    @Test
    fun rejectsOutputForNonRecordingSession() {
        val store = AndroidVideoPrivateStagingStore(root)
        val factory = CameraXPrivateVideoRecordingFactory(store)
        val reviewing = VideoStagingSession(VideoStagingSessionId("reviewing"), VideoStagingState.Reviewing, 1L)

        assertFailsWith<IllegalArgumentException> { factory.outputOptions(reviewing) }
    }
}
