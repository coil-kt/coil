package coil3.request

import coil3.annotation.MainThread
import kotlinx.coroutines.CancellationException

internal interface RequestDelegate {

    /** Throw a [CancellationException] if this request should be cancelled before starting. */
    @MainThread
    fun assertActive() {}

    /** Register all lifecycle observers. */
    @MainThread
    fun start() {}

    /** Called when this request's job is cancelled or completes successfully/unsuccessfully. */
    @MainThread
    fun complete() {}

    /** Cancel this request's job and clear all lifecycle observers. */
    @MainThread
    fun dispose() {}
}
