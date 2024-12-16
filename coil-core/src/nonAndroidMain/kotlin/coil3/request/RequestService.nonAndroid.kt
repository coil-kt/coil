package coil3.request

import coil3.ImageLoader
import coil3.memory.MemoryCache
import coil3.size.Size
import coil3.util.Logger
import coil3.util.SystemCallbacks
import kotlinx.coroutines.Job

internal actual fun RequestService(
    imageLoader: ImageLoader,
    systemCallbacks: SystemCallbacks,
    logger: Logger?,
): RequestService = NonAndroidRequestService(imageLoader)

/** Handles operations that act on [ImageRequest]s. */
internal class NonAndroidRequestService(
    private val imageLoader: ImageLoader,
) : RequestService {

    override fun requestDelegate(
        request: ImageRequest,
        job: Job,
        findLifecycle: Boolean,
    ): RequestDelegate {
        return BaseRequestDelegate(job)
    }

    override fun updateRequest(request: ImageRequest): ImageRequest {
        return request.newBuilder()
            .defaults(imageLoader.defaults)
            .build()
    }

    override fun options(request: ImageRequest, size: Size): Options {
        return Options(
            request.context,
            size,
            request.scale,
            request.precision,
            request.diskCacheKey,
            request.fileSystem,
            request.memoryCachePolicy,
            request.diskCachePolicy,
            request.networkCachePolicy,
            request.extras,
        )
    }

    override fun updateOptions(options: Options): Options {
        return options
    }

    override fun isCacheValueValidForHardware(
        request: ImageRequest,
        cacheValue: MemoryCache.Value,
    ): Boolean {
        return true
    }
}
