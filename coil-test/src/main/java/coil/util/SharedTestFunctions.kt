@file:Suppress("EXPERIMENTAL_API_USAGE", "NOTHING_TO_INLINE", "unused")

package coil.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import coil.ComponentRegistry
import coil.DefaultRequestOptions
import coil.RealImageLoader
import coil.bitmappool.BitmapPool
import coil.decode.Options
import coil.memory.BitmapReferenceCounter
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.GetRequest
import coil.request.GetRequestBuilder
import coil.request.LoadRequest
import coil.request.LoadRequestBuilder
import coil.request.Parameters
import coil.size.Scale
import okhttp3.Call
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.buffer
import okio.source

internal fun createImageLoader(
    context: Context,
    defaults: DefaultRequestOptions = DefaultRequestOptions(),
    bitmapPool: BitmapPool = BitmapPool(Long.MAX_VALUE),
    referenceCounter: BitmapReferenceCounter = BitmapReferenceCounter(bitmapPool),
    memoryCache: MemoryCache = MemoryCache(referenceCounter, Int.MAX_VALUE),
    callFactory: Call.Factory = OkHttpClient(),
    registry: ComponentRegistry = ComponentRegistry()
): RealImageLoader {
    return RealImageLoader(
        context,
        defaults,
        bitmapPool,
        referenceCounter,
        memoryCache,
        callFactory,
        registry
    )
}

fun createMockWebServer(context: Context, vararg images: String): MockWebServer {
    return MockWebServer().apply {
        images.forEach { image ->
            val buffer = Buffer()
            context.assets.open(image).source().buffer().readAll(buffer)
            enqueue(MockResponse().setBody(buffer))
        }
        start()
    }
}

fun createOptions(
    config: Bitmap.Config = Bitmap.Config.ARGB_8888,
    colorSpace: ColorSpace? = null,
    scale: Scale = Scale.FILL,
    allowInexactSize: Boolean = false,
    allowRgb565: Boolean = false,
    headers: Headers = Headers.Builder().build(),
    parameters: Parameters = Parameters.Builder().build(),
    networkCachePolicy: CachePolicy = CachePolicy.ENABLED,
    diskCachePolicy: CachePolicy = CachePolicy.ENABLED
): Options {
    return Options(
        config,
        colorSpace,
        scale,
        allowInexactSize,
        allowRgb565,
        headers,
        parameters,
        networkCachePolicy,
        diskCachePolicy
    )
}

fun Context.decodeBitmapAsset(fileName: String): Bitmap {
    val options = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return checkNotNull(BitmapFactory.decodeStream(assets.open(fileName), null, options))
}

inline fun createGetRequest(
    builder: GetRequestBuilder.() -> Unit = {}
): GetRequest = GetRequestBuilder(DefaultRequestOptions()).data(Unit).apply(builder).build()

inline fun createLoadRequest(
    context: Context,
    builder: LoadRequestBuilder.() -> Unit = {}
): LoadRequest = LoadRequestBuilder(context, DefaultRequestOptions()).data(Unit).apply(builder).build()

inline fun error(): Nothing = throw IllegalStateException()
