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
import coil3.size.SizeResolver
import kotlin.jvm.JvmField

actual abstract class EventListener : ImageRequest.Listener {

    actual override fun onStart(request: ImageRequest) {}

    actual open fun resolveSizeStart(request: ImageRequest, sizeResolver: SizeResolver) {}

    actual open fun resolveSizeEnd(request: ImageRequest, size: Size) {}

    actual open fun mapStart(request: ImageRequest, input: Any) {}

    actual open fun mapEnd(request: ImageRequest, output: Any) {}

    actual open fun keyStart(request: ImageRequest, input: Any) {}

    actual open fun keyEnd(request: ImageRequest, output: String?) {}

    actual open fun fetchStart(
        request: ImageRequest,
        fetcher: Fetcher,
        options: Options,
    ) {}

    actual open fun fetchEnd(
        request: ImageRequest,
        fetcher: Fetcher,
        options: Options,
        result: FetchResult?,
    ) {}

    actual open fun decodeStart(
        request: ImageRequest,
        decoder: Decoder,
        options: Options,
    ) {}

    actual open fun decodeEnd(
        request: ImageRequest,
        decoder: Decoder,
        options: Options,
        result: DecodeResult?,
    ) {}

    actual override fun onCancel(request: ImageRequest) {}

    actual override fun onError(request: ImageRequest, result: ErrorResult) {}

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
