package coil.request

import coil.ImageLoader
import coil.annotation.MainThread
import coil.annotation.WorkerThread
import coil.memory.MemoryCache
import coil.size.Size
import coil.util.Logger
import coil.util.SystemCallbacks
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
            request.fallbackFactory() ?: request.errorFactory()
        } else {
            request.errorFactory()
        },
        request = request,
        throwable = throwable,
    )
}
