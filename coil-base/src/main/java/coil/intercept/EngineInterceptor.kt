package coil.intercept

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.VisibleForTesting
import coil.ComponentRegistry
import coil.EventListener
import coil.bitmap.BitmapPool
import coil.bitmap.BitmapReferenceCounter
import coil.decode.DataSource
import coil.decode.DecodeUtils
import coil.decode.DrawableDecoderService
import coil.decode.EmptyDecoder
import coil.decode.Options
import coil.fetch.DrawableResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.memory.MemoryCache
import coil.memory.MemoryCacheService
import coil.memory.RealMemoryCache
import coil.memory.RequestService
import coil.memory.StrongMemoryCache
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.ImageResult.Metadata
import coil.request.SuccessResult
import coil.size.OriginalSize
import coil.size.PixelSize
import coil.size.Size
import coil.transform.Transformation
import coil.util.Logger
import coil.util.SystemCallbacks
import coil.util.Utils.REQUEST_TYPE_ENQUEUE
import coil.util.allowInexactSize
import coil.util.closeQuietly
import coil.util.fetcher
import coil.util.foldIndices
import coil.util.invoke
import coil.util.log
import coil.util.mapData
import coil.util.requireDecoder
import coil.util.requireFetcher
import coil.util.safeConfig
import coil.util.setValid
import coil.util.toDrawable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

/** The last interceptor in the chain which executes the [ImageRequest]. */
internal class EngineInterceptor(
    private val registry: ComponentRegistry,
    private val bitmapPool: BitmapPool,
    private val referenceCounter: BitmapReferenceCounter,
    private val strongMemoryCache: StrongMemoryCache,
    private val memoryCacheService: MemoryCacheService,
    private val requestService: RequestService,
    private val systemCallbacks: SystemCallbacks,
    private val drawableDecoder: DrawableDecoderService,
    private val logger: Logger?
) : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        try {
            // This interceptor uses some internal APIs.
            check(chain is RealInterceptorChain)

            val request = chain.request
            val context = request.context
            val data = request.data
            val size = chain.size
            val eventListener = chain.eventListener

            // Perform any data mapping.
            eventListener.mapStart(request, data)
            val mappedData = registry.mapData(data)
            eventListener.mapEnd(request, mappedData)

            // Check the memory cache.
            val fetcher = request.fetcher(mappedData) ?: registry.requireFetcher(mappedData)
            val memoryCacheKey = request.memoryCacheKey ?: computeMemoryCacheKey(request, mappedData, fetcher, size)
            val value = if (request.memoryCachePolicy.readEnabled) memoryCacheService[memoryCacheKey] else null

            // Short circuit if the cached bitmap is valid.
            if (value != null && isCachedValueValid(memoryCacheKey, value, request, size)) {
                return SuccessResult(
                    drawable = value.bitmap.toDrawable(context),
                    request = request,
                    metadata = Metadata(
                        memoryCacheKey = memoryCacheKey,
                        isSampled = value.isSampled,
                        dataSource = DataSource.MEMORY_CACHE,
                        isPlaceholderMemoryCacheKeyPresent = chain.cached != null
                    )
                )
            }

            // Fetch, decode, transform, and cache the image on a background dispatcher.
            return withContext(request.dispatcher) {
                // Mark the input data as ineligible for pooling (if necessary).
                invalidateData(request.data)

                // Decrement the value from the memory cache if it was not used.
                if (value != null) referenceCounter.decrement(value.bitmap)

                // Fetch and decode the image.
                val (drawable, isSampled, dataSource) =
                    execute(mappedData, fetcher, request, chain.requestType, size, eventListener)

                // Mark the drawable's bitmap as eligible for pooling.
                validateDrawable(drawable)

                // Cache the result in the memory cache.
                val isCached = writeToMemoryCache(request, memoryCacheKey, drawable, isSampled)

                // Return the result.
                SuccessResult(
                    drawable = drawable,
                    request = request,
                    metadata = Metadata(
                        memoryCacheKey = memoryCacheKey.takeIf { isCached },
                        isSampled = isSampled,
                        dataSource = dataSource,
                        isPlaceholderMemoryCacheKeyPresent = chain.cached != null
                    )
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

    /** Compute the complex cache key for this request. */
    @VisibleForTesting
    internal fun computeMemoryCacheKey(
        request: ImageRequest,
        data: Any,
        fetcher: Fetcher<Any>,
        size: Size
    ): MemoryCache.Key? {
        val base = fetcher.key(data) ?: return null
        return if (request.transformations.isEmpty()) {
            MemoryCache.Key(base, request.parameters)
        } else {
            MemoryCache.Key(base, request.transformations, size, request.parameters)
        }
    }

    /** Return true if [cacheValue] satisfies the [request]. */
    @VisibleForTesting
    internal fun isCachedValueValid(
        cacheKey: MemoryCache.Key?,
        cacheValue: RealMemoryCache.Value,
        request: ImageRequest,
        size: Size
    ): Boolean {
        // Ensure the size of the cached bitmap is valid for the request.
        if (!isSizeValid(cacheKey, cacheValue, request, size)) {
            return false
        }

        // Ensure we don't return a hardware bitmap if the request doesn't allow it.
        if (!requestService.isConfigValidForHardware(request, cacheValue.bitmap.safeConfig)) {
            logger?.log(TAG, Log.DEBUG) {
                "${request.data}: Cached bitmap is hardware-backed, which is incompatible with the request."
            }
            return false
        }

        // Else, the cached drawable is valid and we can short circuit the request.
        return true
    }

    /** Return true if [cacheValue]'s size satisfies the [request]. */
    private fun isSizeValid(
        cacheKey: MemoryCache.Key?,
        cacheValue: RealMemoryCache.Value,
        request: ImageRequest,
        size: Size
    ): Boolean {
        when (size) {
            is OriginalSize -> {
                if (cacheValue.isSampled) {
                    logger?.log(TAG, Log.DEBUG) {
                        "${request.data}: Requested original size, but cached image is sampled."
                    }
                    return false
                }
            }
            is PixelSize -> {
                val cachedWidth: Int
                val cachedHeight: Int
                when (val cachedSize = (cacheKey as? MemoryCache.Key.Complex)?.size) {
                    is PixelSize -> {
                        cachedWidth = cachedSize.width
                        cachedHeight = cachedSize.height
                    }
                    OriginalSize, null -> {
                        val bitmap = cacheValue.bitmap
                        cachedWidth = bitmap.width
                        cachedHeight = bitmap.height
                    }
                }

                val multiple = DecodeUtils.computeSizeMultiplier(
                    srcWidth = cachedWidth,
                    srcHeight = cachedHeight,
                    dstWidth = size.width,
                    dstHeight = size.height,
                    scale = request.scale
                )

                // Short circuit the size check if the size is at most 1 pixel off in either dimension.
                // This accounts for the fact that downsampling can often produce images with one dimension
                // at most one pixel off due to rounding.
                if (abs(cachedWidth - (multiple * cachedWidth)) <= 1 && abs(cachedHeight - (multiple * cachedHeight)) <= 1) {
                    return true
                }

                if (multiple != 1.0 && !request.allowInexactSize) {
                    logger?.log(TAG, Log.DEBUG) {
                        "${request.data}: Cached image's request size ($cachedWidth, $cachedHeight) " +
                            "does not exactly match the requested size (${size.width}, ${size.height}, ${request.scale})."
                    }
                    return false
                }
                if (multiple > 1.0 && cacheValue.isSampled) {
                    logger?.log(TAG, Log.DEBUG) {
                        "${request.data}: Cached image's request size ($cachedWidth, $cachedHeight) " +
                            "is smaller than the requested size (${size.width}, ${size.height}, ${request.scale})."
                    }
                    return false
                }
            }
        }

        return true
    }

    /** Prevent pooling the input data's bitmap. */
    @Suppress("USELESS_CAST")
    private fun invalidateData(data: Any) {
        when (data) {
            is BitmapDrawable -> referenceCounter.setValid(data.bitmap as Bitmap?, false)
            is Bitmap -> referenceCounter.setValid(data, false)
        }
    }

    /** Allow pooling the successful drawable's bitmap. */
    private fun validateDrawable(drawable: Drawable) {
        val bitmap = (drawable as? BitmapDrawable)?.bitmap
        if (bitmap != null) {
            // Mark this bitmap as valid for pooling (if it has not already been made invalid).
            referenceCounter.setValid(bitmap, true)

            // Eagerly increment the bitmap's reference count to prevent it being pooled on another thread.
            referenceCounter.increment(bitmap)
        }
    }

    /** Load the [data] as a [Drawable]. Apply any [Transformation]s. */
    private suspend inline fun execute(
        data: Any,
        fetcher: Fetcher<Any>,
        request: ImageRequest,
        type: Int,
        size: Size,
        eventListener: EventListener
    ): DrawableResult {
        val options = requestService.options(request, size, systemCallbacks.isOnline)

        eventListener.fetchStart(request, fetcher, options)
        val fetchResult = fetcher.fetch(bitmapPool, data, size, options)
        eventListener.fetchEnd(request, fetcher, options, fetchResult)

        val baseResult = when (fetchResult) {
            is SourceResult -> {
                val decodeResult = try {
                    // Check if we're cancelled.
                    coroutineContext.ensureActive()

                    // Find the relevant decoder.
                    val isDiskOnlyPreload = type == REQUEST_TYPE_ENQUEUE &&
                        request.target == null &&
                        !request.memoryCachePolicy.writeEnabled
                    val decoder = if (isDiskOnlyPreload) {
                        // Skip decoding the result if we are preloading the data and writing to the memory cache is
                        // disabled. Instead, we exhaust the source and return an empty result.
                        EmptyDecoder
                    } else {
                        request.decoder ?: registry.requireDecoder(request.data, fetchResult.source, fetchResult.mimeType)
                    }

                    // Decode the stream.
                    eventListener.decodeStart(request, decoder, options)
                    val decodeResult = decoder.decode(bitmapPool, fetchResult.source, size, options)
                    eventListener.decodeEnd(request, decoder, options, decodeResult)
                    decodeResult
                } catch (throwable: Throwable) {
                    // Only close the stream automatically if there is an uncaught exception.
                    // This allows custom decoders to continue to read the source after returning a drawable.
                    fetchResult.source.closeQuietly()
                    throw throwable
                }

                // Combine the fetch and decode operations' results.
                DrawableResult(
                    drawable = decodeResult.drawable,
                    isSampled = decodeResult.isSampled,
                    dataSource = fetchResult.dataSource
                )
            }
            is DrawableResult -> fetchResult
        }

        // Check if we're cancelled.
        coroutineContext.ensureActive()

        // Apply any transformations and prepare to draw.
        val finalResult = applyTransformations(baseResult, request, size, options, eventListener)
        (finalResult.drawable as? BitmapDrawable)?.bitmap?.prepareToDraw()
        return finalResult
    }

    /** Apply any [Transformation]s and return an updated [DrawableResult]. */
    @VisibleForTesting
    internal suspend inline fun applyTransformations(
        result: DrawableResult,
        request: ImageRequest,
        size: Size,
        options: Options,
        eventListener: EventListener
    ): DrawableResult {
        val transformations = request.transformations
        if (transformations.isEmpty()) return result

        // Convert the drawable into a bitmap with a valid config.
        val input = if (result.drawable is BitmapDrawable) {
            val resultBitmap = result.drawable.bitmap
            if (resultBitmap.safeConfig in RequestService.VALID_TRANSFORMATION_CONFIGS) {
                resultBitmap
            } else {
                logger?.log(TAG, Log.INFO) {
                    "Converting bitmap with config ${resultBitmap.safeConfig} to apply transformations: $transformations"
                }
                drawableDecoder.convert(result.drawable, options.config, size, options.scale, options.allowInexactSize)
            }
        } else {
            if (request.allowConversionToBitmap) {
                logger?.log(TAG, Log.INFO) {
                    "Converting drawable of type ${result.drawable::class.java.canonicalName} to apply transformations: $transformations"
                }
                drawableDecoder.convert(result.drawable, options.config, size, options.scale, options.allowInexactSize)
            } else {
                logger?.log(TAG, Log.INFO) {
                    "allowConversionToBitmap=false, skipping transformations for type ${result.drawable::class.java.canonicalName}"
                }
                return result
            }
        }
        eventListener.transformStart(request, input)
        val output = transformations.foldIndices(input) { bitmap, transformation ->
            transformation.transform(bitmapPool, bitmap, size).also { coroutineContext.ensureActive() }
        }
        eventListener.transformEnd(request, output)
        return result.copy(drawable = output.toDrawable(request.context))
    }

    /** Write [drawable] to the memory cache. Return true if it was added to the cache. */
    private fun writeToMemoryCache(
        request: ImageRequest,
        key: MemoryCache.Key?,
        drawable: Drawable,
        isSampled: Boolean
    ): Boolean {
        if (!request.memoryCachePolicy.writeEnabled) {
            return false
        }

        if (key != null) {
            val bitmap = (drawable as? BitmapDrawable)?.bitmap
            if (bitmap != null) {
                strongMemoryCache.set(key, bitmap, isSampled)
                return true
            }
        }
        return false
    }

    companion object {
        private const val TAG = "EngineInterceptor"
    }
}
