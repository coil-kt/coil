package coil3

import coil3.EventListener.Factory
import coil3.annotation.MainThread
import coil3.annotation.WorkerThread
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.SuccessResult
import coil3.size.Size
import kotlin.jvm.JvmField

actual abstract class EventListener : ImageRequest.Listener {

    @MainThread
    actual override fun onStart(request: ImageRequest) {}

    @MainThread
    actual open fun resolveSizeStart(request: ImageRequest) {}

    @MainThread
    actual open fun resolveSizeEnd(request: ImageRequest, size: Size) {}

    @MainThread
    actual open fun mapStart(request: ImageRequest, input: Any) {}

    @MainThread
    actual open fun mapEnd(request: ImageRequest, output: Any) {}

    @MainThread
    actual open fun keyStart(request: ImageRequest, input: Any) {}

    @MainThread
    actual open fun keyEnd(request: ImageRequest, output: String?) {}

    @WorkerThread
    actual open fun fetchStart(
        request: ImageRequest,
        fetcher: Fetcher,
        options: Options,
    ) {}

    @WorkerThread
    actual open fun fetchEnd(
        request: ImageRequest,
        fetcher: Fetcher,
        options: Options,
        result: FetchResult?,
    ) {}

    @WorkerThread
    actual open fun decodeStart(
        request: ImageRequest,
        decoder: Decoder,
        options: Options,
    ) {}

    @WorkerThread
    actual open fun decodeEnd(
        request: ImageRequest,
        decoder: Decoder,
        options: Options,
        result: DecodeResult?,
    ) {}

    @MainThread
    actual override fun onCancel(request: ImageRequest) {}

    @MainThread
    actual override fun onError(request: ImageRequest, result: ErrorResult) {}

    @MainThread
    actual override fun onSuccess(request: ImageRequest, result: SuccessResult) {}

    actual fun interface Factory {

        actual fun create(request: ImageRequest): EventListener

        actual companion object {
            @JvmField actual val NONE = Factory { EventListener.NONE }
        }
    }

    actual companion object {
        @JvmField actual val NONE = object : EventListener() {}
    }
}
