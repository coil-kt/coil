@file:Suppress("EXPERIMENTAL_API_USAGE", "NOTHING_TO_INLINE", "unused")

package coil.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import coil.request.ImageRequest
import coil.size.PixelSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.setMain
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import kotlin.coroutines.CoroutineContext

public fun createMockWebServer(context: Context, vararg images: String): MockWebServer {
    return MockWebServer().apply {
        if (images.isNotEmpty()) {
            images.forEach { image ->
                val buffer = Buffer()
                context.assets.open(image).source().buffer().readAll(buffer)
                enqueue(MockResponse().setBody(buffer))
            }
        } else {
            enqueue(
                MockResponse()
                    .setResponseCode(404)
                    .addHeader("Cache-Control", "public,max-age=60")
            )
        }
        start()
    }
}

public fun Context.decodeBitmapAsset(
    fileName: String,
    options: BitmapFactory.Options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
): Bitmap = checkNotNull(BitmapFactory.decodeStream(assets.open(fileName), null, options))

public fun Context.copyAssetToFile(fileName: String): File {
    val source = assets.open(fileName).source()
    val file = File(filesDir.absolutePath + File.separator + fileName)
    val sink = file.sink().buffer()
    source.use { sink.use { sink.writeAll(source) } }
    return file
}

public val Bitmap.size: PixelSize
    get() = PixelSize(width, height)

public inline fun createRequest(
    context: Context,
    builder: ImageRequest.Builder.() -> Unit = {}
): ImageRequest = ImageRequest.Builder(context).data(Unit).apply(builder).build()

/** Runs the given [block] on the main thread by default and returns [Unit]. */
public fun runBlockingTest(
    context: CoroutineContext = Dispatchers.Main.immediate,
    block: suspend CoroutineScope.() -> Unit
): Unit = runBlocking(context, block)

public fun createTestMainDispatcher(): TestCoroutineDispatcher {
    return TestCoroutineDispatcher().apply { Dispatchers.setMain(this) }
}
