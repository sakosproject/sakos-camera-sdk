package org.sakos.camera.capture.video

import kotlin.test.Test
import kotlin.test.assertEquals

class CameraVideoBackProtectionTest {
    @Test
    fun backStaysProtectedAcrossTheRecordingAndSaveLifecycle() {
        assertEquals(CameraVideoBackProtection.None, protection())
        // Pending output protects the interval before CameraX returns a Recording or emits Start.
        assertEquals(CameraVideoBackProtection.Recording, protection(pending = true))
        assertEquals(CameraVideoBackProtection.Recording, protection(active = true))
        // Pause and resume retain the same recording handle/in-flight state.
        assertEquals(CameraVideoBackProtection.Recording, protection(active = true, inFlight = true))
        assertEquals(CameraVideoBackProtection.Recording, protection(inFlight = true))
        assertEquals(
            CameraVideoBackProtection.Finishing,
            protection(pending = true, active = true, inFlight = true, stopping = true),
        )
        // Finalize clears capture state and starts review in the same main-thread callback.
        assertEquals(CameraVideoBackProtection.Finishing, protection(reviewing = true))
        // Review remains in flight through promotion; all terminal outcomes release Back.
        assertEquals(CameraVideoBackProtection.None, protection())
    }

    @Test
    fun finalizeFailureReleasesBackWithoutRequiringReview() {
        assertEquals(CameraVideoBackProtection.Finishing, protection(active = true, stopping = true))
        assertEquals(CameraVideoBackProtection.None, protection())
        assertEquals(CameraVideoBackProtection.Recording, protection(pending = true))
    }

    @Test
    fun finishingTakesPriorityOverRemainingCaptureFlags() {
        assertEquals(CameraVideoBackProtection.Finishing, protection(stopping = true))
        assertEquals(CameraVideoBackProtection.Finishing, protection(active = true, reviewing = true))
    }

    private fun protection(
        pending: Boolean = false,
        active: Boolean = false,
        inFlight: Boolean = false,
        stopping: Boolean = false,
        reviewing: Boolean = false,
    ) = cameraVideoBackProtection(
        capturePending = pending,
        recordingActive = active,
        recordingInFlight = inFlight,
        stopRequested = stopping,
        reviewInFlight = reviewing,
    )
}
