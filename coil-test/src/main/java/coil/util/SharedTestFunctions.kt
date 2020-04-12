@file:Suppress("EXPERIMENTAL_API_USAGE", "NOTHING_TO_INLINE", "unused")

package coil.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import androidx.annotation.RequiresApi
import coil.decode.Options
import coil.request.CachePolicy
import coil.request.GetRequest
import coil.request.GetRequestBuilder
import coil.request.LoadRequest
import coil.request.LoadRequestBuilder
import coil.request.Parameters
import coil.size.PixelSize
import coil.size.Scale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import kotlin.coroutines.CoroutineContext

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
    scale: Scale = Scale.FILL,
    allowInexactSize: Boolean = false,
    allowRgb565: Boolean = false,
    headers: Headers = Headers.Builder().build(),
    parameters: Parameters = Parameters.Builder().build(),
    memoryCachePolicy: CachePolicy = CachePolicy.ENABLED,
    diskCachePolicy: CachePolicy = CachePolicy.ENABLED,
    networkCachePolicy: CachePolicy = CachePolicy.ENABLED
): Options {
    return Options(
        config,
        null,
        scale,
        allowInexactSize,
        allowRgb565,
        headers,
        parameters,
        memoryCachePolicy,
        diskCachePolicy,
        networkCachePolicy
    )
}

@RequiresApi(26)
fun createOptions(
    config: Bitmap.Config = Bitmap.Config.ARGB_8888,
    colorSpace: ColorSpace? = null,
    scale: Scale = Scale.FILL,
    allowInexactSize: Boolean = false,
    allowRgb565: Boolean = false,
    headers: Headers = Headers.Builder().build(),
    parameters: Parameters = Parameters.Builder().build(),
    memoryCachePolicy: CachePolicy = CachePolicy.ENABLED,
    diskCachePolicy: CachePolicy = CachePolicy.ENABLED,
    networkCachePolicy: CachePolicy = CachePolicy.ENABLED
): Options {
    return Options(
        config,
        colorSpace,
        scale,
        allowInexactSize,
        allowRgb565,
        headers,
        parameters,
        memoryCachePolicy,
        diskCachePolicy,
        networkCachePolicy
    )
}

fun Context.decodeBitmapAsset(
    fileName: String,
    options: BitmapFactory.Options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
): Bitmap = checkNotNull(BitmapFactory.decodeStream(assets.open(fileName), null, options))

fun Context.copyAssetToFile(fileName: String): File {
    val source = assets.open(fileName).source()
    val file = File(filesDir.absolutePath + File.separator + fileName)
    val sink = file.sink().buffer()
    source.use { sink.use { sink.writeAll(source) } }
    return file
}

val Bitmap.size: PixelSize
    get() = PixelSize(width, height)

inline fun createGetRequest(
    context: Context,
    builder: GetRequestBuilder.() -> Unit = {}
): GetRequest = GetRequest.Builder(context).data(Unit).apply(builder).build()

inline fun createLoadRequest(
    context: Context,
    builder: LoadRequestBuilder.() -> Unit = {}
): LoadRequest = LoadRequest.Builder(context).data(Unit).apply(builder).build()

/** Runs the given [block] on the main thread by default and returns [Unit]. */
fun runBlockingTest(
    context: CoroutineContext = Dispatchers.Main.immediate,
    block: suspend CoroutineScope.() -> Unit
) = runBlocking(context, block)
