@file:Suppress("NAME_SHADOWING")

package coil.intercept

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.VisibleForTesting
import coil.ComponentRegistry
import coil.EventListener
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.DecodeResult
import coil.decode.FileImageSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.memory.MemoryCacheService
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.Options
import coil.request.RequestService
import coil.request.SuccessResult
import coil.transform.Transformation
import coil.util.DrawableUtils
import coil.util.Logger
import coil.util.VALID_TRANSFORMATION_CONFIGS
import coil.util.addFirst
import coil.util.closeQuietly
import coil.util.eventListener
import coil.util.foldIndices
import coil.util.isPlaceholderCached
import coil.util.log
import coil.util.safeConfig
import coil.util.toDrawable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** The last interceptor in the chain which executes the [ImageRequest]. */
internal class EngineInterceptor(
    private val imageLoader: ImageLoader,
    private val requestService: RequestService,
    private val logger: Logger?,
) : Interceptor {

    private val memoryCacheService = MemoryCacheService(imageLoader, requestService, logger)

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        try {
            val request = chain.request
            val data = request.data
            val size = chain.size
            val eventListener = chain.eventListener
            val options = requestService.options(request, size)

            // Perform any data mapping.
            eventListener.mapStart(request, data)
            val mappedData = imageLoader.components.map(data, options)
            eventListener.mapEnd(request, mappedData)

            // Check the memory cache.
            val cacheKey = memoryCacheService.getKey(request, mappedData, options, eventListener)
            val cacheValue = cacheKey?.let { memoryCacheService.getValue(request, it, size) }

            // Fast path: return the value from the memory cache.
            if (cacheValue != null) {
                return memoryCacheService.newResult(chain, request, cacheKey, cacheValue)
            }

            // Slow path: fetch, decode, transform, and cache the image.
            return withContext(request.fetcherDispatcher) {
                // Fetch and decode the image.
                val result = execute(request, mappedData, options, eventListener)

                // Write the result to the memory cache.
                val isCached = memoryCacheService.setValue(cacheKey, request, result, options.size)

                // Return the result.
                SuccessResult(
                    drawable = result.drawable,
                    request = request,
                    dataSource = result.dataSource,
                    memoryCacheKey = cacheKey.takeIf { isCached },
                    diskCacheKey = result.diskCacheKey,
                    isSampled = result.isSampled,
                    isPlaceholderCached = chain.isPlaceholderCached,
                )
            }
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) {
                throw throwable
            } else {
                return requestService.errorResult(chain.request, throwable)
            }
        }
    }

    /** Execute the [Fetcher], decode any data into a [Drawable], and apply any [Transformation]s. */
    private suspend inline fun execute(
        request: ImageRequest,
        mappedData: Any,
        options: Options,
        eventListener: EventListener
    ): ExecuteResult {
        var options = options
        var components = imageLoader.components
        var fetchResult: FetchResult? = null
        val executeResult = try {
            if (!requestService.allowHardwareWorkerThread(options)) {
                options = options.copy(config = Bitmap.Config.ARGB_8888)
            }
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
                is SourceResult -> withContext(request.decoderDispatcher) {
                    decode(fetchResult, components, request, mappedData, options, eventListener)
                }
                is DrawableResult -> {
                    ExecuteResult(
                        drawable = fetchResult.drawable,
                        isSampled = fetchResult.isSampled,
                        dataSource = fetchResult.dataSource,
                        diskCacheKey = null // This result has no file source.
                    )
                }
            }
        } finally {
            // Ensure the fetch result's source is always closed.
            (fetchResult as? SourceResult)?.source?.closeQuietly()
        }

        // Apply any transformations and prepare to draw.
        val finalResult = transform(executeResult, request, options, eventListener)
        (finalResult.drawable as? BitmapDrawable)?.bitmap?.prepareToDraw()
        return finalResult
    }

    private suspend inline fun fetch(
        components: ComponentRegistry,
        request: ImageRequest,
        mappedData: Any,
        options: Options,
        eventListener: EventListener
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
                (result as? SourceResult)?.source?.closeQuietly()
                throw throwable
            }

            if (result != null) {
                fetchResult = result
                break
            }
        }
        return fetchResult
    }

    private suspend inline fun decode(
        fetchResult: SourceResult,
        components: ComponentRegistry,
        request: ImageRequest,
        mappedData: Any,
        options: Options,
        eventListener: EventListener
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
            drawable = decodeResult.drawable,
            isSampled = decodeResult.isSampled,
            dataSource = fetchResult.dataSource,
            diskCacheKey = (fetchResult.source as? FileImageSource)?.diskCacheKey
        )
    }

    /** Apply any [Transformation]s and return an updated [ExecuteResult]. */
    @VisibleForTesting
    internal suspend inline fun transform(
        result: ExecuteResult,
        request: ImageRequest,
        options: Options,
        eventListener: EventListener
    ): ExecuteResult {
        val transformations = request.transformations
        if (transformations.isEmpty()) return result

        // Skip the transformations as converting to a bitmap is disabled.
        if (result.drawable !is BitmapDrawable && !request.allowConversionToBitmap) {
            logger?.log(TAG, Log.INFO) {
                val type = result.drawable::class.java.canonicalName
                "allowConversionToBitmap=false, skipping transformations for type $type."
            }
            return result
        }

        // Apply the transformations.
        return withContext(request.transformationDispatcher) {
            val input = convertDrawableToBitmap(result.drawable, options, transformations)
            eventListener.transformStart(request, input)
            val output = transformations.foldIndices(input) { bitmap, transformation ->
                transformation.transform(bitmap, options.size).also { ensureActive() }
            }
            eventListener.transformEnd(request, output)
            result.copy(drawable = output.toDrawable(request.context))
        }
    }

    /** Convert [drawable] to a [Bitmap]. */
    private fun convertDrawableToBitmap(
        drawable: Drawable,
        options: Options,
        transformations: List<Transformation>
    ): Bitmap {
        // Fast path: return the existing bitmap.
        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            if (bitmap.safeConfig in VALID_TRANSFORMATION_CONFIGS) {
                return bitmap
            }
        }

        // Slow path: draw the drawable on a canvas.
        logger?.log(TAG, Log.INFO) {
            val type = drawable::class.java.canonicalName
            "Converting drawable of type $type to apply transformations: $transformations."
        }
        return DrawableUtils.convertToBitmap(
            drawable = drawable,
            config = options.config,
            size = options.size,
            scale = options.scale,
            allowInexactSize = options.allowInexactSize
        )
    }

    class ExecuteResult(
        val drawable: Drawable,
        val isSampled: Boolean,
        val dataSource: DataSource,
        val diskCacheKey: String?
    ) {
        fun copy(
            drawable: Drawable = this.drawable,
            isSampled: Boolean = this.isSampled,
            dataSource: DataSource = this.dataSource,
            diskCacheKey: String? = this.diskCacheKey
        ) = ExecuteResult(drawable, isSampled, dataSource, diskCacheKey)
    }

    companion object {
        private const val TAG = "EngineInterceptor"
    }
}
