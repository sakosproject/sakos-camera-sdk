package org.sakos.camera.sample

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SampleUiStateTest {
    @Test
    fun defaultStateDoesNotAdvertiseFunctionalCapture() {
        val state = SampleUiState()

        assertEquals(SampleCaptureAvailability.RuntimeUnavailable, state.availability)
        assertTrue(state.availabilityMessage.contains("unavailable"))
        assertEquals(0, state.approvedMediaCount)
    }

    @Test
    fun modeSelectionAndCleanupStateAreExplicit() {
        val video = SampleUiReducer.selectMode(SampleUiState(), SampleCaptureMode.Video)
        val cleanupBlocked = SampleUiReducer.reportCleanupRetry(video)

        assertEquals(SampleCaptureMode.Video, cleanupBlocked.mode)
        assertEquals(SampleCaptureAvailability.CleanupRetryRequired, cleanupBlocked.availability)
    }

    @Test
    fun negativeApprovedMediaCountIsRejected() {
        assertFailsWith<IllegalArgumentException> { SampleUiState(approvedMediaCount = -1) }
    }
}
