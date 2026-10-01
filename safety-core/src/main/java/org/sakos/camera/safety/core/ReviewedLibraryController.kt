package org.sakos.camera.safety.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun interface ReviewedLibraryRepository { suspend fun load(): List<ReviewedMediaEntry> }
sealed interface ReviewedLibraryState {
    data class Loading(val previous: List<ReviewedMediaEntry>?) : ReviewedLibraryState
    data class Ready(val items: List<ReviewedMediaEntry>) : ReviewedLibraryState
    data class RecoverableError(val previous: List<ReviewedMediaEntry>?) : ReviewedLibraryState
}

/** Source gallery refresh semantics with host-owned scope; stale responses cannot overwrite newer loads. */
class ReviewedLibraryController(private val scope: CoroutineScope, private val repository: ReviewedLibraryRepository) : AutoCloseable {
    private val mutable = MutableStateFlow<ReviewedLibraryState>(ReviewedLibraryState.Loading(null))
    val state: StateFlow<ReviewedLibraryState> = mutable.asStateFlow()
    private var generation = 0L
    private var job: Job? = null
    private var closed = false
    @Synchronized fun refresh() {
        check(!closed)
        val current = ++generation
        val previous = when (val state = mutable.value) {
            is ReviewedLibraryState.Ready -> state.items
            is ReviewedLibraryState.Loading -> state.previous
            is ReviewedLibraryState.RecoverableError -> state.previous
        }
        mutable.value = ReviewedLibraryState.Loading(previous); job?.cancel()
        job = scope.launch {
            try {
                val items = repository.load()
                synchronized(this@ReviewedLibraryController) {
                    if (!closed && current == generation) mutable.value = ReviewedLibraryState.Ready(items)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { synchronized(this@ReviewedLibraryController) {
                if (!closed && current == generation) mutable.value = ReviewedLibraryState.RecoverableError(previous)
            } }
        }
    }
    @Synchronized override fun close() { closed = true; generation++; job?.cancel() }
}
