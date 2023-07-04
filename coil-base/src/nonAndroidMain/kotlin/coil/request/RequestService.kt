package coil.request

import coil.ImageLoader
import coil.size.Size
import coil.util.Logger
import coil.util.SystemCallbacks
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
        TODO()
    }

    override fun allowHardwareWorkerThread(options: Options): Boolean {
        return true
    }
}
