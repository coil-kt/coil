package coil3.request

import coil3.ImageLoader
import coil3.memory.MemoryCache
import coil3.size.Size
import coil3.util.Logger
import coil3.util.SystemCallbacks
import coil3.util.allowInexactSize
import kotlinx.coroutines.Job

internal actual fun RequestService(
    imageLoader: ImageLoader,
    systemCallbacks: SystemCallbacks,
    logger: Logger?,
): RequestService = NonAndroidRequestService()

/** Handles operations that act on [ImageRequest]s. */
internal class NonAndroidRequestService : RequestService {

    override fun requestDelegate(request: ImageRequest, job: Job): RequestDelegate {
        return BaseRequestDelegate(job)
    }

    override fun errorResult(request: ImageRequest, throwable: Throwable): ErrorResult {
        return commonErrorResult(request, throwable)
    }

    override fun options(request: ImageRequest, size: Size): Options {
        return Options(
            request.context,
            size,
            request.scale,
            request.allowInexactSize,
            request.diskCacheKey,
            request.fileSystem,
            request.memoryCachePolicy,
            request.diskCachePolicy,
            request.networkCachePolicy,
            request.extras,
        )
    }

    override fun updateOptionsOnWorkerThread(options: Options): Options {
        return options
    }

    override fun isCacheValueValidForHardware(
        request: ImageRequest,
        cacheValue: MemoryCache.Value,
    ) = true
}
