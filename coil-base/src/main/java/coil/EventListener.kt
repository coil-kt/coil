package coil

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import coil.annotation.ExperimentalCoilApi
import coil.decode.DataSource
import coil.decode.Decoder
import coil.decode.Options
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.map.Mapper
import coil.map.MeasuredMapper
import coil.request.Request
import coil.size.Size
import coil.size.SizeResolver
import coil.transform.Transformation
import coil.transition.Transition
import coil.transition.TransitionTarget

/**
 * An [ImageLoader]-scoped listener for tracking the progress of an image request.
 * This class is useful for measuring analytics, performance, or other metrics tracking.
 *
 * @see ImageLoaderBuilder.eventListener
 */
@ExperimentalCoilApi
interface EventListener : Request.Listener {

    companion object {
        @JvmField val NONE = object : EventListener {}
    }

    /**
     * Called immediately after the request is dispatched.
     */
    @MainThread
    fun onDispatch(request: Request) {}

    /**
     * Called before any [Mapper]s and/or [MeasuredMapper]s are called to convert the request's data.
     *
     * @param data The data that will be converted.
     */
    @MainThread
    fun mapStart(request: Request, data: Any) {}

    /**
     * Called after the request's data has been converted by all applicable [Mapper]s and/or [MeasuredMapper]s.
     *
     * @param mappedData The data after it has been converted.
     *  If there were no applicable mappers, [mappedData] will be the same as [Request.data].
     */
    @MainThread
    fun mapEnd(request: Request, mappedData: Any) {}

    /**
     * @see Request.Listener.onStart
     */
    @MainThread
    override fun onStart(request: Request) {}

    /**
     * Called before [SizeResolver.size].
     */
    @MainThread
    fun resolveSizeStart(request: Request) {}

    /**
     * Called after [SizeResolver.size].
     *
     * @param size The resolved [Size] for this request.
     */
    @MainThread
    fun resolveSizeEnd(request: Request, size: Size) {}

    /**
     * Called before [Fetcher.fetch].
     *
     * @param fetcher The [Fetcher] that will be used to handle the request.
     * @param options The [Options] that will be passed to [Fetcher.fetch].
     */
    @WorkerThread
    fun fetchStart(request: Request, fetcher: Fetcher<*>, options: Options) {}

    /**
     * Called after [Fetcher.fetch].
     *
     * @param fetcher The [Fetcher] that was used to handle the request.
     * @param options The [Options] that were passed to [Fetcher.fetch].
     */
    @WorkerThread
    fun fetchEnd(request: Request, fetcher: Fetcher<*>, options: Options) {}

    /**
     * Called before [Decoder.decode].
     *
     * This is skipped if [Fetcher.fetch] does not return a [SourceResult].
     *
     * @param decoder The [Decoder] that will be used to handle the request.
     * @param options The [Options] that will be passed to [Decoder.decode].
     */
    @WorkerThread
    fun decodeStart(request: Request, decoder: Decoder, options: Options) {}

    /**
     * Called after [Decoder.decode].
     *
     * This is skipped if [Fetcher.fetch] does not return a [SourceResult].
     *
     * @param decoder The [Decoder] that was used to handle the request.
     * @param options The [Options] that were passed to [Decoder.decode].
     */
    @WorkerThread
    fun decodeEnd(request: Request, decoder: Decoder, options: Options) {}

    /**
     * Called before any [Transformation]s are applied.
     *
     * This is skipped if [Request.transformations] is empty.
     */
    @WorkerThread
    fun transformStart(request: Request) {}

    /**
     * Called after any [Transformation]s are applied.
     *
     * This is skipped if [Request.transformations] is empty.
     */
    @WorkerThread
    fun transformEnd(request: Request) {}

    /**
     * Called before [Transition.transition].
     *
     * This is skipped if [Request.transition] is [Transition.NONE]
     * or [Request.target] does not implement [TransitionTarget].
     */
    @MainThread
    fun transitionStart(request: Request, transition: Transition) {}

    /**
     * Called after [Transition.transition].
     *
     * This is skipped if [Request.transition] is [Transition.NONE]
     * or [Request.target] does not implement [TransitionTarget].
     */
    @MainThread
    fun transitionEnd(request: Request, transition: Transition) {}

    /**
     * @see Request.Listener.onSuccess
     */
    @MainThread
    override fun onSuccess(request: Request, source: DataSource) {}

    /**
     * @see Request.Listener.onCancel
     */
    @MainThread
    override fun onCancel(request: Request) {}

    /**
     * @see Request.Listener.onError
     */
    @MainThread
    override fun onError(request: Request, throwable: Throwable) {}

    /** A factory that creates new [EventListener] instances. */
    interface Factory {

        companion object {
            @JvmField val NONE = Factory(EventListener.NONE)

            /** Create an [EventListener.Factory] that always returns [listener]. */
            @JvmStatic
            @JvmName("create")
            operator fun invoke(listener: EventListener): Factory {
                return object : Factory {
                    override fun create(request: Request) = listener
                }
            }
        }

        /** Return a new [EventListener]. */
        fun create(request: Request): EventListener
    }
}
