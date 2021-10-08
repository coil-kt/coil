package coil

import android.graphics.Bitmap
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import coil.EventListener.Factory
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.key.Keyer
import coil.map.Mapper
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.Options
import coil.request.SuccessResult
import coil.size.Size
import coil.size.SizeResolver
import coil.transform.Transformation
import coil.transition.NoneTransition
import coil.transition.Transition
import coil.transition.TransitionTarget

/**
 * A listener for tracking the progress of an image request. This class is useful for
 * measuring analytics, performance, or other metrics tracking.
 *
 * @see ImageLoader.Builder.eventListenerFactory
 */
interface EventListener : ImageRequest.Listener {

    /**
     * @see ImageRequest.Listener.onStart
     */
    @MainThread
    override fun onStart(request: ImageRequest) {}

    /**
     * Called before [SizeResolver.size].
     */
    @MainThread
    fun resolveSizeStart(request: ImageRequest) {}

    /**
     * Called after [SizeResolver.size].
     *
     * @param size The resolved [Size] for this request.
     */
    @MainThread
    fun resolveSizeEnd(request: ImageRequest, size: Size) {}

    /**
     * Called before [Mapper.map].
     *
     * @param input The data that will be converted.
     */
    @MainThread
    fun mapStart(request: ImageRequest, input: Any) {}

    /**
     * Called after [Mapper.map].
     *
     * @param output The data after it has been converted. If there were no
     *  applicable mappers, [output] will be the same as [ImageRequest.data].
     */
    @MainThread
    fun mapEnd(request: ImageRequest, output: Any) {}

    /**
     * Called before [Keyer.key].
     *
     * @param input The data that will be converted.
     */
    @MainThread
    fun keyStart(request: ImageRequest, input: Any) {}

    /**
     * Called after [Keyer.key].
     *
     * @param output The data after it has been converted into a string key.
     *  If [output] is 'null' it will not be cached in the memory cache.
     */
    @MainThread
    fun keyEnd(request: ImageRequest, output: String?) {}

    /**
     * Called before [Fetcher.fetch].
     *
     * @param fetcher The [Fetcher] that will be used to handle the request.
     * @param options The [Options] that will be passed to [Fetcher.fetch].
     */
    @WorkerThread
    fun fetchStart(request: ImageRequest, fetcher: Fetcher, options: Options) {}

    /**
     * Called after [Fetcher.fetch].
     *
     * @param fetcher The [Fetcher] that was used to handle the request.
     * @param options The [Options] that were passed to [Fetcher.fetch].
     * @param result The result of [Fetcher.fetch].
     */
    @WorkerThread
    fun fetchEnd(request: ImageRequest, fetcher: Fetcher, options: Options, result: FetchResult?) {}

    /**
     * Called before [Decoder.decode].
     *
     * This is skipped if [Fetcher.fetch] does not return a [SourceResult].
     *
     * @param decoder The [Decoder] that will be used to handle the request.
     * @param options The [Options] that will be passed to [Decoder.decode].
     */
    @WorkerThread
    fun decodeStart(request: ImageRequest, decoder: Decoder, options: Options) {}

    /**
     * Called after [Decoder.decode].
     *
     * This is skipped if [Fetcher.fetch] does not return a [SourceResult].
     *
     * @param decoder The [Decoder] that was used to handle the request.
     * @param options The [Options] that were passed to [Decoder.decode].
     * @param result The result of [Decoder.decode].
     */
    @WorkerThread
    fun decodeEnd(request: ImageRequest, decoder: Decoder, options: Options, result: DecodeResult?) {}

    /**
     * Called before any [Transformation]s are applied.
     *
     * This is skipped if [ImageRequest.transformations] is empty.
     *
     * @param input The [Bitmap] that will be transformed.
     */
    @WorkerThread
    fun transformStart(request: ImageRequest, input: Bitmap) {}

    /**
     * Called after any [Transformation]s are applied.
     *
     * This is skipped if [ImageRequest.transformations] is empty.
     *
     * @param output The [Bitmap] that was transformed.
     */
    @WorkerThread
    fun transformEnd(request: ImageRequest, output: Bitmap) {}

    /**
     * Called before [Transition.transition].
     *
     * This is skipped if [transition] is a [NoneTransition]
     * or [ImageRequest.target] does not implement [TransitionTarget].
     */
    @MainThread
    fun transitionStart(request: ImageRequest, transition: Transition) {}

    /**
     * Called after [Transition.transition].
     *
     * This is skipped if [transition] is a [NoneTransition]
     * or [ImageRequest.target] does not implement [TransitionTarget].
     */
    @MainThread
    fun transitionEnd(request: ImageRequest, transition: Transition) {}

    /**
     * @see ImageRequest.Listener.onCancel
     */
    @MainThread
    override fun onCancel(request: ImageRequest) {}

    /**
     * @see ImageRequest.Listener.onError
     */
    @MainThread
    override fun onError(request: ImageRequest, result: ErrorResult) {}

    /**
     * @see ImageRequest.Listener.onSuccess
     */
    @MainThread
    override fun onSuccess(request: ImageRequest, result: SuccessResult) {}

    fun interface Factory {

        fun create(request: ImageRequest): EventListener

        companion object {
            @JvmField val NONE = Factory { EventListener.NONE }
        }
    }

    companion object {
        @JvmField val NONE = object : EventListener {}
    }
}
