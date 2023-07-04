package coil

import coil.EventListener.Factory
import coil.annotation.MainThread
import coil.annotation.WorkerThread
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.Options
import coil.request.SuccessResult
import coil.size.Size

actual abstract class EventListener : ImageRequest.Listener {

    @MainThread
    actual override fun onStart(request: ImageRequest) {}

    @MainThread
    actual fun resolveSizeStart(request: ImageRequest) {}

    @MainThread
    actual fun resolveSizeEnd(request: ImageRequest, size: Size) {}

    @MainThread
    actual fun mapStart(request: ImageRequest, input: Any) {}

    @MainThread
    actual fun mapEnd(request: ImageRequest, output: Any) {}

    @MainThread
    actual fun keyStart(request: ImageRequest, input: Any) {}

    @MainThread
    actual fun keyEnd(request: ImageRequest, output: String?) {}

    @WorkerThread
    actual fun fetchStart(
        request: ImageRequest,
        fetcher: Fetcher,
        options: Options,
    ) {}

    @WorkerThread
    actual fun fetchEnd(
        request: ImageRequest,
        fetcher: Fetcher,
        options: Options,
        result: FetchResult?,
    ) {}

    @WorkerThread
    actual fun decodeStart(
        request: ImageRequest,
        decoder: Decoder,
        options: Options,
    ) {}

    @WorkerThread
    actual fun decodeEnd(
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
            actual val NONE = Factory { EventListener.NONE }
        }
    }

    actual companion object {
        actual val NONE = object : EventListener() {}
    }
}
