package coil3.intercept

import coil3.ComponentRegistry
import coil3.EventListener
import coil3.Image
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.DecodeResult
import coil3.decode.FileImageSource
import coil3.fetch.FetchResult
import coil3.fetch.ImageFetchResult
import coil3.fetch.SourceFetchResult
import coil3.intercept.EngineInterceptor.ExecuteResult
import coil3.memory.MemoryCacheService
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.Options
import coil3.request.RequestService
import coil3.request.SuccessResult
import coil3.util.ErrorResult
import coil3.util.Logger
import coil3.util.SystemCallbacks
import coil3.util.addFirst
import coil3.util.closeQuietly
import coil3.util.eventListener
import coil3.util.isPlaceholderCached
import coil3.util.memoryCacheKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

/** The last interceptor in the chain which executes the [ImageRequest]. */
internal class EngineInterceptor(
    private val imageLoader: ImageLoader,
    private val systemCallbacks: SystemCallbacks,
    private val requestService: RequestService,
    private val memoryCacheService: MemoryCacheService,
    private val logger: Logger?,
) : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        return withContext(chain.request.fetcherCoroutineContext) {
            try {
                val request = chain.request
                val eventListener = chain.eventListener
                val options = requestService.options(request, chain.size)
                val mappedData = memoryCacheService.mapData(request, options, eventListener)

                // Fetch and decode the image.
                val result = execute(request, mappedData, options, eventListener)

                // Register memory pressure callbacks.
                systemCallbacks.registerMemoryPressureCallbacks()

                // Write the result to the memory cache.
                val memoryCacheKey = memoryCacheService.setCacheValue(chain.memoryCacheKey, request, result)

                // Return the result.
                SuccessResult(
                    image = result.image,
                    request = request,
                    dataSource = result.dataSource,
                    memoryCacheKey = memoryCacheKey,
                    diskCacheKey = result.diskCacheKey,
                    isSampled = result.isSampled,
                    isPlaceholderCached = chain.isPlaceholderCached,
                )
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) {
                    throw throwable
                } else {
                    ErrorResult(chain.request, throwable)
                }
            }
        }
    }

    private suspend fun execute(
        request: ImageRequest,
        mappedData: Any,
        options: Options,
        eventListener: EventListener,
    ): ExecuteResult {
        @Suppress("NAME_SHADOWING")
        var options = options
        var components = imageLoader.components
        var fetchResult: FetchResult? = null
        val executeResult = try {
            options = requestService.updateOptions(options)

            if (request.fetcherFactory != null || request.decoderFactory != null) {
                components = components.newBuilder()
                    .addFirst(request.fetcherFactory)
                    .addFirst(request.decoderFactory)
                    .build()
            }

            // Fetch the data.
            fetchResult = fetch(components, request, mappedData, options, eventListener)

            // Decode the data.
            when (fetchResult) {
                is SourceFetchResult -> withContext(request.decoderCoroutineContext) {
                    decode(fetchResult, components, request, mappedData, options, eventListener)
                }
                is ImageFetchResult -> {
                    ExecuteResult(
                        image = fetchResult.image,
                        isSampled = fetchResult.isSampled,
                        dataSource = fetchResult.dataSource,
                        diskCacheKey = null, // This result has no file source.
                    )
                }
            }
        } finally {
            // Ensure the fetch result's source is always closed.
            (fetchResult as? SourceFetchResult)?.source?.closeQuietly()
        }

        // Apply any transformations.
        return transform(executeResult, request, options, eventListener, logger)
    }

    private suspend fun fetch(
        components: ComponentRegistry,
        request: ImageRequest,
        mappedData: Any,
        options: Options,
        eventListener: EventListener,
    ): FetchResult {
        val fetchResult: FetchResult
        var searchIndex = 0
        while (true) {
            val pair = components.newFetcher(mappedData, options, imageLoader, searchIndex)
            checkNotNull(pair) { "Unable to create a fetcher that supports: $mappedData" }
            val fetcher = pair.first
            searchIndex = pair.second + 1

            eventListener.fetchStart(request, fetcher, options)
            val result = fetcher.fetch()
            try {
                eventListener.fetchEnd(request, fetcher, options, result)
            } catch (throwable: Throwable) {
                // Ensure the source is closed if an exception occurs before returning the result.
                (result as? SourceFetchResult)?.source?.closeQuietly()
                throw throwable
            }

            if (result != null) {
                fetchResult = result
                break
            }
        }
        return fetchResult
    }

    private suspend fun decode(
        fetchResult: SourceFetchResult,
        components: ComponentRegistry,
        request: ImageRequest,
        mappedData: Any,
        options: Options,
        eventListener: EventListener,
    ): ExecuteResult {
        val decodeResult: DecodeResult
        var searchIndex = 0
        while (true) {
            val pair = components.newDecoder(fetchResult, options, imageLoader, searchIndex)
            checkNotNull(pair) { "Unable to create a decoder that supports: $mappedData" }
            val decoder = pair.first
            searchIndex = pair.second + 1

            eventListener.decodeStart(request, decoder, options)
            val result = decoder.decode()
            eventListener.decodeEnd(request, decoder, options, result)

            if (result != null) {
                decodeResult = result
                break
            }
        }

        // Combine the fetch and decode operations' results.
        return ExecuteResult(
            image = decodeResult.image,
            isSampled = decodeResult.isSampled,
            dataSource = fetchResult.dataSource,
            diskCacheKey = (fetchResult.source as? FileImageSource)?.diskCacheKey,
        )
    }

    data class ExecuteResult(
        val image: Image,
        val isSampled: Boolean,
        val dataSource: DataSource,
        val diskCacheKey: String?,
    )

    companion object {
        internal const val TAG = "EngineInterceptor"
    }
}

internal expect suspend fun transform(
    result: ExecuteResult,
    request: ImageRequest,
    options: Options,
    eventListener: EventListener,
    logger: Logger?,
): ExecuteResult

internal expect fun prepareToDraw(
    image: Image,
)
