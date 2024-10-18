package coil3.request

import kotlin.jvm.JvmInline
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job

internal interface RequestDelegate {

    /** Throw a [CancellationException] if this request should be cancelled before starting. */
    fun assertActive() {}

    /** Register all lifecycle observers. */
    fun start() {}

    /** Wait until the request's associated lifecycle is started. */
    suspend fun awaitStarted() {}

    /** Called when this request's job is cancelled or completes successfully/unsuccessfully. */
    fun complete() {}

    /** Cancel this request's job and clear all lifecycle observers. */
    fun dispose() {}
}

/** A request delegate for a one-shot requests. */
@JvmInline
internal value class BaseRequestDelegate(
    private val job: Job,
) : RequestDelegate {

    override fun dispose() {
        job.cancel()
    }
}
