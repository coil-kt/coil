package coil.request

import coil.ImageLoader
import coil.memory.MemoryCache
import coil.size.Size
import coil.util.Logger
import coil.util.SystemCallbacks
import coil.util.allowInexactSize
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
            context = request.context,
            size = size,
            scale = request.scale,
            allowInexactSize = request.allowInexactSize,
            diskCacheKey = request.diskCacheKey,
            memoryCachePolicy = request.memoryCachePolicy,
            diskCachePolicy = request.diskCachePolicy,
            networkCachePolicy = request.networkCachePolicy,
            extras = request.extras,
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
