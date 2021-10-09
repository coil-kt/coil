package coil.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

fun createMockWebServer(vararg images: String): MockWebServer {
    val server = MockWebServer()
    images.forEach { image ->
        server.enqueueImage(image)
    }
    return server.apply { start() }
}

fun MockWebServer.enqueueImage(image: String): Long {
    val buffer = Buffer()
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.assets.open(image).source().buffer().readAll(buffer)
    enqueue(MockResponse().setBody(buffer))
    return buffer.size
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

/** Runs the given [block] on the main thread by default. */
fun runBlockingTest(
    context: CoroutineContext = Dispatchers.Main.immediate,
    block: suspend CoroutineScope.() -> Unit
) = runBlocking(context, block)

@OptIn(ExperimentalCoroutinesApi::class)
fun createTestMainDispatcher(): TestCoroutineDispatcher {
    return TestCoroutineDispatcher().apply { Dispatchers.setMain(this) }
}
