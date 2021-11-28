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
import coil.decode.DecodeUtils
import coil.decode.FileImageSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.Options
import coil.request.RequestService
import coil.request.SuccessResult
import coil.size.Dimension
import coil.size.Scale
import coil.size.Size
import coil.size.isOriginal
import coil.size.pxOrElse
import coil.transform.Transformation
import coil.util.DrawableUtils
import coil.util.Logger
import coil.util.VALID_TRANSFORMATION_CONFIGS
import coil.util.addFirst
import coil.util.allowInexactSize
import coil.util.closeQuietly
import coil.util.foldIndices
import coil.util.forEachIndexedIndices
import coil.util.log
import coil.util.pxString
import coil.util.safeConfig
import coil.util.toDrawable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.collections.set
import kotlin.math.abs

/** The last interceptor in the chain which executes the [ImageRequest]. */
internal class EngineInterceptor(
    private val imageLoader: ImageLoader,
    private val requestService: RequestService,
    private val logger: Logger?
) : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        try {
            val request = chain.request
            val context = request.context
            val data = request.data
            val size = chain.size
            val eventListener = chain.eventListener
            val options = requestService.options(request, size)

            // Perform any data mapping.
            eventListener.mapStart(request, data)
            val mappedData = imageLoader.components.map(data, options)
            eventListener.mapEnd(request, mappedData)

            // Check the memory cache.
            val cacheKey = getMemoryCacheKey(request, mappedData, options, eventListener)
            val cacheValue = getMemoryCacheValue(request, cacheKey)

            // Fast path: return the value from the memory cache.
            if (cacheValue != null && isMemoryCacheValueValid(cacheValue, request, size)) {
                return SuccessResult(
                    drawable = cacheValue.bitmap.toDrawable(context),
                    request = request,
                    dataSource = DataSource.MEMORY_CACHE,
                    memoryCacheKey = cacheKey,
                    diskCacheKey = cacheValue.diskCacheKey,
                    isSampled = cacheValue.isSampled,
                    isPlaceholderCached = chain.isPlaceholderCached,
                )
            }

            // Slow path: fetch, decode, transform, and cache the image.
            return withContext(request.fetcherDispatcher) {
                // Fetch and decode the image.
                val result = execute(request, mappedData, options, eventListener)

                // Write the result to the memory cache.
                val isMemoryCached = setMemoryCacheValue(cacheKey, request, result, options)

                // Return the result.
                SuccessResult(
                    drawable = result.drawable,
                    request = request,
                    dataSource = result.dataSource,
                    memoryCacheKey = cacheKey.takeIf { isMemoryCached },
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

    /** Get the memory cache key for this request. */
    @VisibleForTesting
    internal fun getMemoryCacheKey(
        request: ImageRequest,
        mappedData: Any,
        options: Options,
        eventListener: EventListener
    ): MemoryCache.Key? {
        // Fast path: an explicit memory cache key has been set.
        request.memoryCacheKey?.let { return it }

        // Slow path: create a new memory cache key.
        eventListener.keyStart(request, mappedData)
        val base = imageLoader.components.key(mappedData, options)
        eventListener.keyEnd(request, base)
        if (base == null) return null

        val extras = mutableMapOf<String, String>()
        if (request.transformations.isNotEmpty()) {
            request.transformations.forEachIndexedIndices { index, transformation ->
                extras["coil#transformation_$index"] = transformation.cacheKey
            }
            extras["coil#request_size"] = options.size.toString()
        }
        extras.putAll(request.parameters.cacheKeys())
        return MemoryCache.Key(base, extras)
    }

    /** Return 'true' if [cacheValue] satisfies the [request]. */
    @VisibleForTesting
    internal fun isMemoryCacheValueValid(
        cacheValue: MemoryCache.Value,
        request: ImageRequest,
        size: Size
    ): Boolean {
        // Ensure we don't return a hardware bitmap if the request doesn't allow it.
        if (!requestService.isConfigValidForHardware(request, cacheValue.bitmap.safeConfig)) {
            logger?.log(TAG, Log.DEBUG) {
                "${request.data}: Cached bitmap is hardware-backed, which is incompatible with the request."
            }
            return false
        }

        // Ensure the size of the cached bitmap is valid for the request.
        return isSizeValid(cacheValue, request, size)
    }

    /** Return 'true' if [cacheValue]'s size satisfies the [request]. */
    private fun isSizeValid(
        cacheValue: MemoryCache.Value,
        request: ImageRequest,
        size: Size
    ): Boolean {
        // The cached value must not be sampled if the image's original size is requested.
        if (size.isOriginal) {
            if (cacheValue.isSampled) {
                logger?.log(TAG, Log.DEBUG) {
                    "${request.data}: Requested original size, but cached image is sampled."
                }
                return false
            } else {
                return true
            }
        }

        val srcWidth = cacheValue.bitmap.width
        val srcHeight = cacheValue.bitmap.height
        val dstWidth = size.width.pxOrElse {
            when {
                !cacheValue.isSampled -> srcWidth
                else -> when (request.scale) {
                    Scale.FIT -> Int.MAX_VALUE
                    Scale.FILL -> Int.MIN_VALUE
                }
            }
        }
        val dstHeight = size.height.pxOrElse {
            when {
                !cacheValue.isSampled -> srcHeight
                else -> when (request.scale) {
                    Scale.FIT -> Int.MAX_VALUE
                    Scale.FILL -> Int.MIN_VALUE
                }
            }
        }
        val multiplier = DecodeUtils.computeSizeMultiplier(
            srcWidth = srcWidth,
            srcHeight = srcHeight,
            dstWidth = dstWidth,
            dstHeight = dstHeight,
            scale = request.scale
        )

        // Short circuit the size check if the size is at most 1 pixel off in either dimension.
        // This accounts for the fact that downsampling can often produce images with dimensions
        // at most one pixel off due to rounding.
        if (request.allowInexactSize) {
            val downsampleMultiplier = multiplier.coerceAtMost(1.0)
            if ((size.width is Dimension.Original || abs(dstWidth - (downsampleMultiplier * srcWidth)) <= 1) ||
                (size.height is Dimension.Original || abs(dstHeight - (downsampleMultiplier * srcHeight)) <= 1)) {
                return true
            }
        } else {
            if (abs(dstWidth - srcWidth) <= 1 && abs(dstHeight - srcHeight) <= 1) {
                return true
            }
        }

        // The cached value must be equal to the requested size if exact size is requested.
        if (multiplier != 1.0 && !request.allowInexactSize) {
            logger?.log(TAG, Log.DEBUG) {
                "${request.data}: Cached image's request size " +
                    "($srcWidth, $srcHeight) does not exactly match the requested size " +
                    "(${size.width.pxString()}, ${size.height.pxString()}, ${request.scale})."
            }
            return false
        }

        // The cached value must be larger than the requested size if the cached value is sampled.
        if (multiplier >= 1.0 && cacheValue.isSampled) {
            logger?.log(TAG, Log.DEBUG) {
                "${request.data}: Cached image's request size " +
                    "($srcWidth, $srcHeight) is smaller than the requested size " +
                    "(${size.width.pxString()}, ${size.height.pxString()}, ${request.scale})."
            }
            return false
        }

        return true
    }

    /** Execute the [Fetcher], decode any data into a [Drawable], and apply any [Transformation]s. */
    private suspend inline fun execute(
        request: ImageRequest,
        mappedData: Any,
        requestOptions: Options,
        eventListener: EventListener
    ): ExecuteResult {
        var options = requestOptions
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

    /** Get the memory cache value for this request. */
    private fun getMemoryCacheValue(
        request: ImageRequest,
        cacheKey: MemoryCache.Key?
    ): MemoryCache.Value? {
        if (!request.memoryCachePolicy.readEnabled) return null
        val memoryCache = imageLoader.memoryCache
        if (memoryCache == null || cacheKey == null) return null
        return memoryCache[cacheKey]
    }

    /** Write [drawable] to the memory cache. Return 'true' if it was added to the cache. */
    private fun setMemoryCacheValue(
        cacheKey: MemoryCache.Key?,
        request: ImageRequest,
        result: ExecuteResult,
        options: Options
    ): Boolean {
        if (!request.memoryCachePolicy.writeEnabled) return false
        val memoryCache = imageLoader.memoryCache
        if (memoryCache == null || cacheKey == null) return false
        val bitmap = (result.drawable as? BitmapDrawable)?.bitmap ?: return false

        // Create and set the memory cache value.
        val extras = mutableMapOf<String, Any>()
        extras[EXTRA_REQUEST_WIDTH] = options.size.width
        extras[EXTRA_REQUEST_HEIGHT] = options.size.height
        extras[EXTRA_IS_SAMPLED] = result.isSampled
        result.diskCacheKey?.let { extras[EXTRA_DISK_CACHE_KEY] = it }
        memoryCache[cacheKey] = MemoryCache.Value(bitmap, extras)
        return true
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

    private val MemoryCache.Value.isSampled: Boolean
        get() = (extras[EXTRA_IS_SAMPLED] as? Boolean) ?: false

    private val MemoryCache.Value.diskCacheKey: String?
        get() = extras[EXTRA_DISK_CACHE_KEY] as? String

    private val Interceptor.Chain.isPlaceholderCached: Boolean
        get() = this is RealInterceptorChain && isPlaceholderCached

    private val Interceptor.Chain.eventListener: EventListener
        get() = if (this is RealInterceptorChain) eventListener else EventListener.NONE

    @VisibleForTesting
    internal class ExecuteResult(
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
        @VisibleForTesting internal const val EXTRA_REQUEST_WIDTH = "coil#request_width"
        @VisibleForTesting internal const val EXTRA_REQUEST_HEIGHT = "coil#request_height"
        @VisibleForTesting internal const val EXTRA_IS_SAMPLED = "coil#is_sampled"
        @VisibleForTesting internal const val EXTRA_DISK_CACHE_KEY = "coil#disk_cache_key"
    }
}
