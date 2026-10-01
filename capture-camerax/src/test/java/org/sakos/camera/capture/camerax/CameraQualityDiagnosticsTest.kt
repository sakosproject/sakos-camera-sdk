package org.sakos.camera.capture.camerax

import kotlin.test.Test
import kotlin.test.assertEquals

class CameraQualityDiagnosticsTest {
    @Test
    fun megapixelLabelsKeepSubTenValuesReadable() {
        assertEquals("0.9 MP", cameraDiagnosticMegapixelLabel(1280, 720))
        assertEquals("12 MP", cameraDiagnosticMegapixelLabel(4000, 3000))
    }

    @Test
    fun aspectRatioLabelsReduceCommonCameraShapes() {
        assertEquals("16:9", cameraDiagnosticAspectRatioLabel(1920, 1080))
        assertEquals("4:3", cameraDiagnosticAspectRatioLabel(4032, 3024))
        assertEquals("unknown ratio", cameraDiagnosticAspectRatioLabel(0, 1080))
    }

    @Test
    fun videoTierLabelsUseActualSavedDimensions() {
        assertEquals("UHD/4K actual", cameraDiagnosticVideoTierLabel(3840, 2160))
        assertEquals("FHD/1080p actual", cameraDiagnosticVideoTierLabel(1080, 1920))
        assertEquals("HD/720p actual", cameraDiagnosticVideoTierLabel(1280, 720))
        assertEquals("SD/480p actual", cameraDiagnosticVideoTierLabel(640, 480))
        assertEquals("unknown video quality", cameraDiagnosticVideoTierLabel(null, null))
    }

    @Test
    fun videoFileSummaryUsesActualDimensionsWhenMetadataIsPresent() {
        val summary = cameraDiagnosticVideoFileSummary(
            label = "Latest reviewed video",
            durationMillis = 10_000L,
            fileSizeBytes = 2L * 1024L * 1024L,
            facts = CameraDiagnosticVideoFileFacts(
                width = 1920,
                height = 1080,
                rotationDegrees = 90,
                metadataDurationMillis = 12_400L,
            ),
        )

        assertEquals(
            "Latest reviewed video: 1920x1080 (2.1 MP, 16:9), FHD/1080p actual, rotation 90, duration 0:12, size 2.0 MB",
            summary,
        )
    }

    @Test
    fun videoFileSummaryStaysExplicitWhenDimensionsAreUnavailable() {
        val summary = cameraDiagnosticVideoFileSummary(
            label = "Latest temporary video",
            durationMillis = 1_000L,
            fileSizeBytes = 512L,
            facts = null,
        )

        assertEquals(
            "Latest temporary video: actual dimensions unavailable, duration 0:01, size 512 B",
            summary,
        )
    }

    @Test
    fun exportTokenSanitizerKeepsAdbFileNamesStable() {
        assertEquals("front-video-hd-check", cameraDiagnosticSafeExportToken(" Front Video HD Check! "))
        assertEquals("snapshot", cameraDiagnosticSafeExportToken("../"))
        assertEquals(
            "abcdefghijklmnopqrstuvwxyz0123456789abcdefghijkl",
            cameraDiagnosticSafeExportToken("abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnop"),
        )
    }
}
