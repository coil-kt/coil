package coil

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import coil.annotation.ExperimentalCoilApi
import coil.decode.DataSource
import coil.decode.Decoder
import coil.decode.Options
import coil.fetch.Fetcher
import coil.map.Mapper
import coil.map.MeasuredMapper
import coil.request.Request
import coil.size.Size
import coil.size.SizeResolver
import coil.transform.Transformation

/**
 * An [ImageLoader]-scoped listener for tracking the progress of an image request.
 * This class is useful for measuring analytics, performance, or other metrics tracking.
 *
 * @see ImageLoaderBuilder.eventListener
 */
@ExperimentalCoilApi
interface EventListener : Request.Listener {

    companion object {
        @JvmField
        val EMPTY = object : EventListener {}
    }

    /**
     * Called when the request is started.
     */
    @MainThread
    override fun onStart(request: Request) {}

    /**
     * Called before any [Mapper]s or [MeasuredMapper]s are called to convert the request's data.
     */
    @MainThread
    fun mapStart(request: Request) {}

    /**
     * Called after the request's data has been converted.
     */
    @MainThread
    fun mapEnd(request: Request, mappedData: Any) {}

    /**
     * Called before [SizeResolver.size] to await the request's size.
     */
    @MainThread
    fun resolveSizeStart(request: Request) {}

    /**
     * Called after the request's size has been resolved.
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
     * @param decoder The [Decoder] that will be used to handle the request.
     * @param options The [Options] that will be passed to [Decoder.decode].
     */
    @WorkerThread
    fun decodeStart(request: Request, decoder: Decoder, options: Options) {}

    /**
     * Called after [Decoder.decode].
     *
     * @param decoder The [Decoder] that was used to handle the request.
     * @param options The [Options] that were passed to [Decoder.decode].
     */
    @WorkerThread
    fun decodeEnd(request: Request, decoder: Decoder, options: Options) {}

    /**
     * Called before any [Transformation]s are applied.
     */
    @WorkerThread
    fun transformStart(request: Request) {}

    /**
     * Called after any [Transformation]s are applied.
     */
    @WorkerThread
    fun transformEnd(request: Request) {}

    /**
     * Called when the request is successful.
     */
    @MainThread
    override fun onSuccess(request: Request, source: DataSource) {}

    /**
     * Called when the request is cancelled.
     */
    @MainThread
    override fun onCancel(request: Request) {}

    /**
     * Called when the request fails.
     */
    @MainThread
    override fun onError(request: Request, throwable: Throwable) {}

    /** A factory that creates new [EventListener] instances. */
    interface Factory {

        companion object {
            @JvmField
            val EMPTY = Factory(EventListener.EMPTY)

            /** Create an [EventListener.Factory] that returns the same [listener]. */
            @JvmStatic
            @JvmName("create")
            operator fun invoke(listener: EventListener): Factory {
                return object : Factory {
                    override fun newListener(request: Request) = listener
                }
            }
        }

        /** Return a new [EventListener]. */
        fun newListener(request: Request): EventListener
    }
}
