package coil3.request

import coil3.ImageLoader
import coil3.annotation.MainThread
import coil3.annotation.WorkerThread
import coil3.memory.MemoryCache
import coil3.size.Size
import coil3.util.Logger
import coil3.util.SystemCallbacks
import kotlinx.coroutines.Job

internal expect fun RequestService(
    imageLoader: ImageLoader,
    systemCallbacks: SystemCallbacks,
    logger: Logger?,
): RequestService

/** Handles operations that act on [ImageRequest]s. */
internal interface RequestService {

    @MainThread
    fun requestDelegate(request: ImageRequest, job: Job): RequestDelegate

    @MainThread
    fun errorResult(request: ImageRequest, throwable: Throwable): ErrorResult

    @MainThread
    fun options(request: ImageRequest, size: Size): Options

    @WorkerThread
    fun updateOptionsOnWorkerThread(options: Options): Options

    @WorkerThread
    fun isCacheValueValidForHardware(request: ImageRequest, cacheValue: MemoryCache.Value): Boolean
}

internal fun commonErrorResult(
    request: ImageRequest,
    throwable: Throwable,
): ErrorResult {
    return ErrorResult(
        image = if (throwable is NullRequestDataException) {
            request.fallback() ?: request.error()
        } else {
            request.error()
        },
        request = request,
        throwable = throwable,
    )
}
