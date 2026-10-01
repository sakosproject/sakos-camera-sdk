package org.sakos.camera.safety.core

import java.io.*
import java.nio.file.Files
import kotlin.test.*
import kotlinx.coroutines.*

class PrivateReviewedMediaLibraryTest {
    private val config = SafetyConfigurationVersion(SafetyComponentVersion("synthetic", "1"), SafetyComponentVersion("pixels", "1"), SafetyComponentVersion("mock", "1"))
    private fun capture(id: String = "synthetic") = SafetyCaptureContext(SafetyCaptureId(id), 0, 8, 8, 0, false)
    private fun approval(capture: SafetyCaptureContext) = requireNotNull(SafetyEvaluationOutcome.Decision(capture.captureId,
        SafetyEvaluationReceiptId("simulated"), config, SafetyDecision.Allow).approvalForManagedCapture(capture))
    private fun fixture(action: suspend (File, PrivateReviewedMediaLibrary) -> Unit) = runBlocking {
        val root = Files.createTempDirectory("sakos-synthetic-library").toFile()
        try { action(root, PrivateReviewedMediaLibrary(root, config)) } finally { root.deleteRecursively() }
    }
    @Test fun approvedOnlyCommitMembershipReplayAndReopen() = fixture { root, library ->
        val capture = capture()
        val item = library.save(ReviewedMediaKind.Photo, capture, approval(capture)) { it.write(byteArrayOf(1, 2, 3)) }
        assertEquals(listOf(item), library.items()); assertContentEquals(byteArrayOf(1, 2, 3), library.open(item).use { it.readBytes() })
        assertFailsWith<IllegalArgumentException> { library.save(ReviewedMediaKind.Photo, capture, approval(capture)) { it.write(1) } }
        assertEquals(listOf(item), PrivateReviewedMediaLibrary(root, config).items())
        assertFailsWith<IllegalArgumentException> { library.open(item.copy(id = "../outside")) }
    }
    @Test fun wrongCaptureOrConfigurationCannotWrite() = fixture { root, library ->
        val other = capture("other")
        assertFailsWith<IllegalArgumentException> { library.save(ReviewedMediaKind.Photo, other, approval(capture())) { fail("No write") } }
        val alternate = PrivateReviewedMediaLibrary(root, config.copy(policy = SafetyComponentVersion("alternate", "1")))
        assertFailsWith<IllegalArgumentException> { alternate.save(ReviewedMediaKind.Photo, other, approval(other)) { fail("No write") } }
        assertTrue(library.items().isEmpty())
    }
    @Test fun failedEmptyAndCancelledWritesNeverBecomeItems() = fixture { root, library ->
        for (mode in 0..2) {
            assertFails { library.save(ReviewedMediaKind.Photo, capture(), approval(capture())) {
                if (mode == 0) throw IOException("simulated failure")
                if (mode == 1) throw CancellationException("simulated cancellation")
            } }
            assertTrue(library.items().isEmpty()); assertTrue(root.listFiles().orEmpty().isEmpty())
        }
    }
    @Test fun terminalHostGuardPreventsLateCommit() = fixture { root, library ->
        var active = true
        assertFailsWith<IllegalStateException> { library.save(ReviewedMediaKind.Photo, capture(), approval(capture()), { active }) {
            it.write(1); active = false
        } }
        assertTrue(root.listFiles().orEmpty().isEmpty())
    }
    @Test fun pendingRecoveryAndTamperExcludeUnapprovedBytes() = fixture { root, library ->
        val item = library.save(ReviewedMediaKind.Photo, capture(), approval(capture())) { it.write(1) }
        val pending = File(root, "interrupted.pending").apply { mkdir() }; File(pending, "partial").writeBytes(byteArrayOf(1))
        PrivateReviewedMediaLibrary(root, config); assertFalse(pending.exists())
        File(root, "${item.id}/media.jpg").writeBytes(byteArrayOf(2))
        assertTrue(library.items().isEmpty()); assertFailsWith<IllegalArgumentException> { library.open(item) }
    }
    @Test fun deniedSaveNeverCreatesDestinationAndFailedCopyRollsBack() = fixture { _, library ->
        val item = library.save(ReviewedMediaKind.Photo, capture(), approval(capture())) { it.write(1) }
        var begins = 0; var rollbacks = 0
        val destination = ReviewedExportDestination {
            begins++
            object : ReviewedExportTransaction {
                override fun output() = object : OutputStream() { override fun write(value: Int) { throw IOException("synthetic output failure") } }
                override fun commit() { fail("No commit") }
                override fun close() { rollbacks++ }
            }
        }
        val exporter = ReviewedMediaExporter(library)
        assertEquals(ReviewedExportResult(1, 0, 1, 0), exporter.export(listOf(item), { false }, destination)); assertEquals(0, begins)
        assertEquals(ReviewedExportResult(1, 0, 0, 1), exporter.export(listOf(item), { true }, destination)); assertEquals(1, rollbacks)
    }
    @Test fun authorizedExportCopiesExactApprovedBytesAndCommits() = fixture { _, library ->
        val data = ByteArray(20_000) { (it % 7).toByte() }
        val item = library.save(ReviewedMediaKind.Video, capture(), approval(capture())) { it.write(data) }
        val output = ByteArrayOutputStream(); var committed = false; var closed = false
        val result = ReviewedMediaExporter(library).export(listOf(item), { true }) {
            object : ReviewedExportTransaction {
                override fun output(): OutputStream = output
                override fun commit() { committed = true }
                override fun close() { closed = true }
            }
        }
        assertEquals(1, result.saved); assertTrue(committed && closed); assertContentEquals(data, output.toByteArray())
    }
    @Test fun cancelledExportPropagatesAndRollsBack() = fixture { _, library ->
        val item = library.save(ReviewedMediaKind.Photo, capture(), approval(capture())) { it.write(1) }
        var closed = false
        assertFailsWith<CancellationException> { ReviewedMediaExporter(library).export(listOf(item), { true }) {
            object : ReviewedExportTransaction {
                override fun output() = object : OutputStream() { override fun write(value: Int) { throw CancellationException("synthetic cancel") } }
                override fun commit() { fail("No commit") }
                override fun close() { closed = true }
            }
        } }
        assertTrue(closed); assertEquals(1, library.items().size)
    }
    @Test fun staleRefreshCannotReplaceNewerInventoryAndCloseIsTerminal() = runBlocking {
        val first = CompletableDeferred<List<ReviewedMediaEntry>>()
        var calls = 0
        val controller = ReviewedLibraryController(this) {
            if (++calls == 1) withContext(NonCancellable) { first.await() } else emptyList()
        }
        controller.refresh(); yield(); controller.refresh(); yield()
        first.complete(listOf(ReviewedMediaEntry("old", ReviewedMediaKind.Photo, 1, 0, 1, 1, "x", "x"))); yield()
        assertEquals(ReviewedLibraryState.Ready(emptyList()), controller.state.value)
        controller.close(); assertFailsWith<IllegalStateException> { controller.refresh() }; Unit
    }
}
