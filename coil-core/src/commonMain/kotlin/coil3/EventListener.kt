package coil3

import coil3.annotation.MainThread
import coil3.annotation.WorkerThread
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.map.Mapper
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.SuccessResult
import coil3.size.Size
import coil3.size.SizeResolver
import kotlin.jvm.JvmField

/**
 * A listener for tracking the progress of an image request. This class is useful for
 * measuring analytics, performance, or other metrics tracking.
 *
 * @see ImageLoader.Builder.eventListenerFactory
 */
expect abstract class EventListener : ImageRequest.Listener {

    /**
     * @see ImageRequest.Listener.onStart
     */
    @MainThread
    override fun onStart(request: ImageRequest)

    /**
     * Called before [SizeResolver.size].
     */
    @MainThread
    open fun resolveSizeStart(request: ImageRequest)

    /**
     * Called after [SizeResolver.size].
     *
     * @param size The resolved [Size] for this request.
     */
    @MainThread
    open fun resolveSizeEnd(request: ImageRequest, size: Size)

    /**
     * Called before [Mapper.map].
     *
     * @param input The data that will be converted.
     */
    @MainThread
    open fun mapStart(request: ImageRequest, input: Any)

    /**
     * Called after [Mapper.map].
     *
     * @param output The data after it has been converted. If there were no
     *  applicable mappers, [output] will be the same as [ImageRequest.data].
     */
    @MainThread
    open fun mapEnd(request: ImageRequest, output: Any)

    /**
     * Called before [Keyer.key].
     *
     * @param input The data that will be converted.
     */
    @MainThread
    open fun keyStart(request: ImageRequest, input: Any)

    /**
     * Called after [Keyer.key].
     *
     * @param output The data after it has been converted into a string key.
     *  If [output] is 'null' it will not be cached in the memory cache.
     */
    @MainThread
    open fun keyEnd(request: ImageRequest, output: String?)

    /**
     * Called before [Fetcher.fetch].
     *
     * @param fetcher The [Fetcher] that will be used to handle the request.
     * @param options The [Options] that will be passed to [Fetcher.fetch].
     */
    @WorkerThread
    open fun fetchStart(request: ImageRequest, fetcher: Fetcher, options: Options)

    /**
     * Called after [Fetcher.fetch].
     *
     * @param fetcher The [Fetcher] that was used to handle the request.
     * @param options The [Options] that were passed to [Fetcher.fetch].
     * @param result The result of [Fetcher.fetch].
     */
    @WorkerThread
    open fun fetchEnd(request: ImageRequest, fetcher: Fetcher, options: Options, result: FetchResult?)

    /**
     * Called before [Decoder.decode].
     *
     * This is skipped if [Fetcher.fetch] does not return a [SourceFetchResult].
     *
     * @param decoder The [Decoder] that will be used to handle the request.
     * @param options The [Options] that will be passed to [Decoder.decode].
     */
    @WorkerThread
    open fun decodeStart(request: ImageRequest, decoder: Decoder, options: Options)

    /**
     * Called after [Decoder.decode].
     *
     * This is skipped if [Fetcher.fetch] does not return a [SourceFetchResult].
     *
     * @param decoder The [Decoder] that was used to handle the request.
     * @param options The [Options] that were passed to [Decoder.decode].
     * @param result The result of [Decoder.decode].
     */
    @WorkerThread
    open fun decodeEnd(request: ImageRequest, decoder: Decoder, options: Options, result: DecodeResult?)

    /**
     * @see ImageRequest.Listener.onCancel
     */
    @MainThread
    override fun onCancel(request: ImageRequest)

    /**
     * @see ImageRequest.Listener.onError
     */
    @MainThread
    override fun onError(request: ImageRequest, result: ErrorResult)

    /**
     * @see ImageRequest.Listener.onSuccess
     */
    @MainThread
    override fun onSuccess(request: ImageRequest, result: SuccessResult)

    fun interface Factory {

        fun create(request: ImageRequest): EventListener

        companion object {
            @JvmField val NONE: Factory
        }
    }

    companion object {
        @JvmField val NONE: EventListener
    }
}
