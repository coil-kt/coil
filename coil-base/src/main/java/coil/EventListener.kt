package coil

import android.graphics.Bitmap
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import coil.EventListener.Factory
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.decode.Options
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.map.Mapper
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.size.Size
import coil.size.SizeResolver
import coil.transform.Transformation
import coil.transition.Transition
import coil.transition.TransitionTarget

/**
 * A listener for tracking the progress of an image request. This class is useful for
 * measuring analytics, performance, or other metrics tracking.
 *
 * @see ImageLoader.Builder.eventListener
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
    @AnyThread
    fun mapStart(request: ImageRequest, input: Any) {}

    /**
     * Called after [Mapper.map].
     *
     * @param output The data after it has been converted. If there were no applicable mappers,
     *  [output] will be the same as [ImageRequest.data].
     */
    @AnyThread
    fun mapEnd(request: ImageRequest, output: Any) {}

    /**
     * Called before [Fetcher.fetch].
     *
     * @param fetcher The [Fetcher] that will be used to handle the request.
     * @param options The [Options] that will be passed to [Fetcher.fetch].
     */
    @WorkerThread
    fun fetchStart(request: ImageRequest, fetcher: Fetcher<*>, options: Options) {}

    /**
     * Called after [Fetcher.fetch].
     *
     * @param fetcher The [Fetcher] that was used to handle the request.
     * @param options The [Options] that were passed to [Fetcher.fetch].
     * @param result The result of [Fetcher.fetch]. **Do not** keep a reference to [result] or its data
     *  outside the scope of this method.
     */
    @WorkerThread
    fun fetchEnd(request: ImageRequest, fetcher: Fetcher<*>, options: Options, result: FetchResult) {}

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
     * @param result The result of [Decoder.decode]. **Do not** keep a reference to [result] or its data
     *  outside the scope of this method.
     */
    @WorkerThread
    fun decodeEnd(request: ImageRequest, decoder: Decoder, options: Options, result: DecodeResult) {}

    /**
     * Called before any [Transformation]s are applied.
     *
     * This is skipped if [ImageRequest.transformations] is empty.
     *
     * @param input The [Bitmap] that will be transformed. **Do not** keep a reference to [input] outside
     *  the scope of this method.
     */
    @WorkerThread
    fun transformStart(request: ImageRequest, input: Bitmap) {}

    /**
     * Called after any [Transformation]s are applied.
     *
     * This is skipped if [ImageRequest.transformations] is empty.
     *
     * @param output The [Bitmap] that was transformed. **Do not** keep a reference to [output] outside
     *  the scope of this method.
     */
    @WorkerThread
    fun transformEnd(request: ImageRequest, output: Bitmap) {}

    /**
     * Called before [Transition.transition].
     *
     * This is skipped if [ImageRequest.transition] is [Transition.NONE]
     * or [ImageRequest.target] does not implement [TransitionTarget].
     */
    @MainThread
    fun transitionStart(request: ImageRequest) {}

    /**
     * Called after [Transition.transition].
     *
     * This is skipped if [ImageRequest.transition] is [Transition.NONE]
     * or [ImageRequest.target] does not implement [TransitionTarget].
     */
    @MainThread
    fun transitionEnd(request: ImageRequest) {}

    /**
     * @see ImageRequest.Listener.onCancel
     */
    @MainThread
    override fun onCancel(request: ImageRequest) {}

    /**
     * @see ImageRequest.Listener.onError
     */
    @MainThread
    override fun onError(request: ImageRequest, throwable: Throwable) {}

    /**
     * @see ImageRequest.Listener.onSuccess
     */
    @MainThread
    override fun onSuccess(request: ImageRequest, metadata: ImageResult.Metadata) {}

    /** A factory that creates new [EventListener] instances. */
    fun interface Factory {

        companion object {
            @JvmField val NONE = Factory(EventListener.NONE)

            /** Create an [EventListener.Factory] that always returns [listener]. */
            @JvmStatic
            @JvmName("create")
            operator fun invoke(listener: EventListener) = Factory { listener }
        }

        /** Return a new [EventListener]. */
        fun create(request: ImageRequest): EventListener
    }

    companion object {
        @JvmField val NONE = object : EventListener {}
    }
}
