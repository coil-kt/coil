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
import coil.transform.Transformation
import coil.transition.NoneTransition
import coil.transition.Transition
import coil.transition.TransitionTarget

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

    /**
     * Called before any [Transformation]s are applied.
     *
     * This is skipped if [ImageRequest.transformations] is empty.
     *
     * @param input The [Image] that will be transformed.
     */
    @WorkerThread
    open fun transformStart(request: ImageRequest, input: Image) {}

    /**
     * Called after any [Transformation]s are applied.
     *
     * This is skipped if [ImageRequest.transformations] is empty.
     *
     * @param output The [Image] that was transformed.
     */
    @WorkerThread
    open fun transformEnd(request: ImageRequest, output: Image) {}

    /**
     * Called before [Transition.transition].
     *
     * This is skipped if [transition] is a [NoneTransition]
     * or [ImageRequest.target] does not implement [TransitionTarget].
     */
    @MainThread
    open fun transitionStart(request: ImageRequest, transition: Transition) {}

    /**
     * Called after [Transition.transition].
     *
     * This is skipped if [transition] is a [NoneTransition]
     * or [ImageRequest.target] does not implement [TransitionTarget].
     */
    @MainThread
    open fun transitionEnd(request: ImageRequest, transition: Transition) {}

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
