package coil3.request

import kotlinx.coroutines.Job

/** A request delegate for a one-shot requests. */
internal class BaseRequestDelegate(
    private val job: Job,
) : RequestDelegate {

    override fun dispose() {
        job.cancel()
    }
}
