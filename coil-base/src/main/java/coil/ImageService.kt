package coil

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import coil.annotation.ExperimentalCoilApi
import coil.bitmappool.BitmapPool
import coil.decode.DataSource
import coil.decode.DecodeUtils
import coil.decode.DrawableDecoderService
import coil.decode.EmptyDecoder
import coil.decode.Options
import coil.fetch.DrawableResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.memory.BitmapReferenceCounter
import coil.memory.MemoryCache
import coil.memory.RealMemoryCache
import coil.memory.RequestService
import coil.memory.StrongMemoryCache
import coil.memory.TargetDelegate
import coil.memory.WeakMemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.Metadata
import coil.request.SuccessResult
import coil.size.OriginalSize
import coil.size.PixelSize
import coil.size.Scale
import coil.size.Size
import coil.size.SizeResolver
import coil.transform.Transformation
import coil.util.Logger
import coil.util.SystemCallbacks
import coil.util.Utils.REQUEST_TYPE_ENQUEUE
import coil.util.closeQuietly
import coil.util.fetcher
import coil.util.foldIndices
import coil.util.invoke
import coil.util.log
import coil.util.mapData
import coil.util.requireDecoder
import coil.util.requireFetcher
import coil.util.safeConfig
import coil.util.set
import coil.util.takeIf
import coil.util.toDrawable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

@OptIn(ExperimentalCoilApi::class)
internal class ImageService(
    private val context: Context,
    private val defaults: DefaultRequestOptions,
    private val registry: ComponentRegistry,
    private val bitmapPool: BitmapPool,
    private val referenceCounter: BitmapReferenceCounter,
    private val strongMemoryCache: StrongMemoryCache,
    private val weakMemoryCache: WeakMemoryCache,
    private val requestService: RequestService,
    private val systemCallbacks: SystemCallbacks,
    private val drawableDecoder: DrawableDecoderService,
    private val logger: Logger?
) {

    @AnyThread
    suspend fun execute(
        request: ImageRequest,
        type: Int,
        data: Any,
        targetDelegate: TargetDelegate,
        eventListener: EventListener
    ): SuccessResult {
        // Prepare to resolve the size.
        val sizeResolver = requestService.sizeResolver(request, context)
        val scale = requestService.scale(request, sizeResolver)
        val lazySizeResolver = LazySizeResolver(sizeResolver, targetDelegate, request, defaults, eventListener)

        // Invalidate the bitmap if it was provided as input.
        when (data) {
            is BitmapDrawable -> referenceCounter.invalidate(data.bitmap)
            is Bitmap -> referenceCounter.invalidate(data)
        }

        // Perform any data mapping.
        eventListener.mapStart(request, data)
        val mappedData = registry.mapData(data) { lazySizeResolver.size() }
        eventListener.mapEnd(request, mappedData)

        // Check the memory cache.
        val fetcher = request.fetcher(mappedData) ?: registry.requireFetcher(mappedData)
        val key = request.key ?: computeKey(request, mappedData, fetcher) { lazySizeResolver.size() }
        val memoryCachePolicy = request.memoryCachePolicy ?: defaults.memoryCachePolicy
        val value = takeIf(memoryCachePolicy.readEnabled) {
            key?.let { strongMemoryCache.get(it) ?: weakMemoryCache.get(it) }
        }

        // Ignore the cached bitmap if it is hardware-backed and the request disallows hardware bitmaps.
        val cachedDrawable = value?.bitmap
            ?.takeIf { requestService.isConfigValidForHardware(request, it.safeConfig) }
            ?.toDrawable(context)

        // Resolve the size if it has not been resolved already.
        val size = lazySizeResolver.size(cachedDrawable)

        // Short circuit if the cached bitmap is valid.
        if (cachedDrawable != null && isCachedValueValid(key, value, request, sizeResolver, size, scale)) {
            return SuccessResult(
                drawable = value.bitmap.toDrawable(context),
                metadata = Metadata(key, value.isSampled, DataSource.MEMORY_CACHE)
            )
        }

        // Fetch and decode the image.
        val (drawable, isSampled, dataSource) = loadData(mappedData, fetcher, request, type, memoryCachePolicy,
            sizeResolver, size, scale, eventListener)

        // Cache the result in the memory cache.
        val isCached = memoryCachePolicy.writeEnabled && strongMemoryCache.set(key, drawable, isSampled)

        return SuccessResult(
            drawable = drawable,
            metadata = Metadata(key.takeIf { isCached }, isSampled, dataSource)
        )
    }

    /** Compute the complex cache key for this request. */
    @VisibleForTesting
    internal inline fun computeKey(
        request: ImageRequest,
        data: Any,
        fetcher: Fetcher<Any>,
        lazySize: () -> Size
    ): MemoryCache.Key? {
        val base = fetcher.key(data) ?: return null
        return if (request.transformations.isEmpty()) {
            MemoryCache.Key(base, request.parameters)
        } else {
            MemoryCache.Key(base, request.transformations, lazySize(), request.parameters)
        }
    }

    /** Return true if [cacheValue] satisfies the [request]. */
    @VisibleForTesting
    internal fun isCachedValueValid(
        cacheKey: MemoryCache.Key?,
        cacheValue: RealMemoryCache.Value,
        request: ImageRequest,
        sizeResolver: SizeResolver,
        size: Size,
        scale: Scale
    ): Boolean {
        // Ensure the size of the cached bitmap is valid for the request.
        if (!isSizeValid(cacheKey, cacheValue, request, sizeResolver, size, scale)) {
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
    @VisibleForTesting
    internal fun isSizeValid(
        cacheKey: MemoryCache.Key?,
        cacheValue: RealMemoryCache.Value,
        request: ImageRequest,
        sizeResolver: SizeResolver,
        size: Size,
        scale: Scale
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

                // Short circuit the size check if the size is at most 1 pixel off in either dimension.
                // This accounts for the fact that downsampling can often produce images with one dimension
                // at most one pixel off due to rounding.
                if (abs(cachedWidth - size.width) <= 1 && abs(cachedHeight - size.height) <= 1) {
                    return true
                }

                val multiple = DecodeUtils.computeSizeMultiplier(
                    srcWidth = cachedWidth,
                    srcHeight = cachedHeight,
                    dstWidth = size.width,
                    dstHeight = size.height,
                    scale = scale
                )
                if (multiple != 1.0 && !requestService.allowInexactSize(request, sizeResolver)) {
                    logger?.log(TAG, Log.DEBUG) {
                        "${request.data}: Cached image's request size ($cachedWidth, $cachedHeight) " +
                            "does not exactly match the requested size (${size.width}, ${size.height}, $scale)."
                    }
                    return false
                }
                if (multiple > 1.0 && cacheValue.isSampled) {
                    logger?.log(TAG, Log.DEBUG) {
                        "${request.data}: Cached image's request size ($cachedWidth, $cachedHeight) " +
                            "is smaller than the requested size (${size.width}, ${size.height}, $scale)."
                    }
                    return false
                }
            }
        }

        return true
    }

    /** Load the [data] as a [Drawable]. Apply any [Transformation]s. */
    @VisibleForTesting
    internal suspend inline fun loadData(
        data: Any,
        fetcher: Fetcher<Any>,
        request: ImageRequest,
        type: Int,
        memoryCachePolicy: CachePolicy,
        sizeResolver: SizeResolver,
        size: Size,
        scale: Scale,
        eventListener: EventListener
    ): DrawableResult = withContext(request.dispatcher ?: defaults.dispatcher) {
        val options = requestService.options(request, sizeResolver, size, scale, systemCallbacks.isOnline)

        eventListener.fetchStart(request, fetcher, options)
        val fetchResult = fetcher.fetch(bitmapPool, data, size, options)
        eventListener.fetchEnd(request, fetcher, options, fetchResult)

        val baseResult = when (fetchResult) {
            is SourceResult -> {
                val decodeResult = try {
                    // Check if we're cancelled.
                    coroutineContext.ensureActive()

                    // Find the relevant decoder.
                    val isDiskOnlyPreload = type == REQUEST_TYPE_ENQUEUE && request.target == null && !memoryCachePolicy.writeEnabled
                    val decoder = if (isDiskOnlyPreload) {
                        // Skip decoding the result if we are preloading the data and writing to the memory cache is
                        // disabled. Instead, we exhaust the source and return an empty result.
                        EmptyDecoder
                    } else {
                        request.decoder ?: registry.requireDecoder(request.data!!, fetchResult.source, fetchResult.mimeType)
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
        finalResult
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
            logger?.log(TAG, Log.INFO) {
                "Converting drawable of type ${result.drawable::class.java.canonicalName} " +
                    "to apply transformations: $transformations"
            }
            drawableDecoder.convert(result.drawable, options.config, size, options.scale, options.allowInexactSize)
        }
        eventListener.transformStart(request, input)
        val output = transformations.foldIndices(input) { bitmap, transformation ->
            transformation.transform(bitmapPool, bitmap, size).also { coroutineContext.ensureActive() }
        }
        eventListener.transformEnd(request, output)
        return result.copy(drawable = output.toDrawable(context))
    }

    /** Lazily resolves and caches a request's size. */
    class LazySizeResolver(
        private val sizeResolver: SizeResolver,
        private val targetDelegate: TargetDelegate,
        private val request: ImageRequest,
        private val defaults: DefaultRequestOptions,
        private val eventListener: EventListener
    ) {

        private var size: Size? = null

        /** Calls [TargetDelegate.start], [SizeResolver.size], and caches the resolved size. */
        @MainThread
        suspend inline fun size(cached: BitmapDrawable? = null): Size {
            // Return the size if it has already been resolved.
            size?.let { return it }

            targetDelegate.start(cached, cached ?: request.placeholder ?: defaults.placeholder)
            eventListener.onStart(request)
            request.listener?.onStart(request)

            eventListener.resolveSizeStart(request, sizeResolver)
            val size = sizeResolver.size().also { size = it }
            eventListener.resolveSizeEnd(request, sizeResolver, size)

            coroutineContext.ensureActive()

            return size
        }
    }

    companion object {
        private const val TAG = "ImageEngine"
    }
}
