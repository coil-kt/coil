package coil.request

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.VisibleForTesting
import coil.ComponentRegistry
import coil.DefaultRequestOptions
import coil.EventListener
import coil.annotation.ExperimentalCoilApi
import coil.bitmappool.BitmapPool
import coil.decode.Decoder
import coil.decode.DrawableDecoderService
import coil.decode.EmptyDecoder
import coil.decode.Options
import coil.fetch.DrawableResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.memory.RequestService
import coil.size.Scale
import coil.size.Size
import coil.size.SizeResolver
import coil.transform.Transformation
import coil.util.Logger
import coil.util.SystemCallbacks
import coil.util.closeQuietly
import coil.util.foldIndices
import coil.util.log
import coil.util.mapIndices
import coil.util.safeConfig
import coil.util.toDrawable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoilApi::class)
internal class RequestExecutor(
    private val context: Context,
    private val defaults: DefaultRequestOptions,
    private val bitmapPool: BitmapPool,
    private val requestService: RequestService,
    private val drawableDecoder: DrawableDecoderService,
    private val systemCallbacks: SystemCallbacks,
    private val registry: ComponentRegistry,
    private val logger: Logger?
) {

    private val requestPool = mutableMapOf<Key, Value>()

    /** Load the [mappedData] as a [Drawable]. Apply any [Transformation]s. */
    suspend inline fun loadData(
        mappedData: Any,
        fetcher: Fetcher<Any>,
        request: Request,
        sizeResolver: SizeResolver,
        size: Size,
        scale: Scale,
        eventListener: EventListener
    ): DrawableResult = withContext(request.dispatcher ?: defaults.dispatcher) {
        val options = requestService.options(request, sizeResolver, size, scale, systemCallbacks.isOnline)

        val transformationKeys = request.transformations.mapIndices { it.key() }
        val key = Key(mappedData, size, options, fetcher, request.decoder, transformationKeys)

        val value = synchronized(requestPool) {
            requestPool[key]
        }
        if (value != null) {
            val result = value.deferred.await()
            if ((result.drawable as? BitmapDrawable))
        }

        eventListener.fetchStart(request, fetcher, options)
        val fetchResult = fetcher.fetch(bitmapPool, mappedData, size, options)
        eventListener.fetchEnd(request, fetcher, options)

        val baseResult = when (fetchResult) {
            is SourceResult -> {
                val decodeResult = try {
                    // Check if we're cancelled.
                    ensureActive()

                    // Find the applicable decoder.
                    val decoder = if (request.isDiskOnlyPreload()) {
                        // Skip decoding the result if we are preloading the data and writing to the memory cache is
                        // disabled. Instead, we exhaust the source and return an empty result.
                        EmptyDecoder
                    } else {
                        request.decoder ?: registry.requireDecoder(request.data!!, fetchResult.source, fetchResult.mimeType)
                    }

                    // Decode the stream.
                    eventListener.decodeStart(request, decoder, options)
                    val decodeResult = decoder.decode(bitmapPool, fetchResult.source, size, options)
                    eventListener.decodeEnd(request, decoder, options)
                    decodeResult
                } catch (exception: Exception) {
                    // We only close the stream automatically if there is an uncaught exception.
                    // This allows custom decoders to continue to read the source after returning a result.
                    fetchResult.source.closeQuietly()
                    throw exception
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
        ensureActive()

        // Apply any transformations.
        val finalResult = applyTransformations(this, baseResult, request, size, options, eventListener)

        // Assume we are going to draw the drawable almost immediately after the request completes.
        (finalResult.drawable as? BitmapDrawable)?.bitmap?.prepareToDraw()

        return@withContext finalResult
    }

    /** Apply any [Transformation]s and return an updated [DrawableResult]. */
    @VisibleForTesting
    internal suspend inline fun applyTransformations(
        scope: CoroutineScope,
        result: DrawableResult,
        request: Request,
        size: Size,
        options: Options,
        eventListener: EventListener
    ): DrawableResult = scope.run {
        // Skip this step if there are no transformations.
        val transformations = request.transformations
        if (transformations.isEmpty()) return@run result

        // Convert the drawable into a bitmap with a valid config.
        eventListener.transformStart(request)
        val baseBitmap = if (result.drawable is BitmapDrawable) {
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
        val transformedBitmap = transformations.foldIndices(baseBitmap) { bitmap, transformation ->
            transformation.transform(bitmapPool, bitmap, size).also { ensureActive() }
        }
        val transformedResult = result.copy(drawable = transformedBitmap.toDrawable(context))
        eventListener.transformEnd(request)
        return@run transformedResult
    }

    /** Return true if the returned drawable will be discarded and we can skip decoding the drawable. */
    private fun Request.isDiskOnlyPreload(): Boolean {
        return this is LoadRequest && target == null && !(memoryCachePolicy ?: defaults.memoryCachePolicy).writeEnabled
    }

    data class Key(
        val mappedData: Any,
        val size: Size,
        val options: Options,
        val fetcher: Fetcher<Any>,
        val decoder: Decoder?,
        val transformationsKeys: List<String>
    )

    class Value(
        val request: Request,
        val deferred: Deferred<DrawableResult>
    )

    companion object {
        private const val TAG = "RequestExecutor"
    }
}
