package coil.request

import coil.ImageLoader
import coil.memory.MemoryCache
import coil.size.Size
import coil.util.Logger
import coil.util.SystemCallbacks
import coil.util.allowInexactSize
import coil.util.defaultFileSystem
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
            request.fileSystem ?: defaultFileSystem(),
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
