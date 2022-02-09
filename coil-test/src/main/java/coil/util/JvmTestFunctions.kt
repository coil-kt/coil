package coil.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.Headers.Companion.headersOf
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.buffer
import okio.sink
import okio.source
import org.junit.Assume
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/** Alias for [Assume.assumeTrue]. */
fun assumeTrue(actual: Boolean, message: String = "") {
    if (message.isBlank()) {
        Assume.assumeTrue(actual)
    } else {
        Assume.assumeTrue(message, actual)
    }
}

fun createMockWebServer(vararg images: String): MockWebServer {
    val server = MockWebServer()
    images.forEach { server.enqueueImage(it) }
    return server.apply { start() }
}

fun MockWebServer.enqueueImage(image: String, headers: Headers = headersOf()): Long {
    val buffer = Buffer()
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.assets.open(image).source().buffer().readAll(buffer)
    enqueue(MockResponse().setHeaders(headers).setBody(buffer))
    return buffer.size
}

fun Context.decodeBitmapAsset(
    fileName: String,
    options: BitmapFactory.Options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
): Bitmap {
    // Retry multiple times as the emulator can be flaky.
    var failures = 0
    while (true) {
        try {
            return BitmapFactory.decodeStream(assets.open(fileName), null, options)!!
        } catch (e: Exception) {
            if (failures++ > 5) throw e
        }
    }
}

fun Context.copyAssetToFile(fileName: String): File {
    val source = assets.open(fileName).source()
    val file = File(filesDir.absolutePath + File.separator + fileName)
    val sink = file.sink().buffer()
    source.use { sink.use { sink.writeAll(source) } }
    return file
}

@OptIn(ExperimentalCoroutinesApi::class)
fun runTestMain(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Unit
) = runTest(context) {
    withContext(Dispatchers.Main.immediate, block)
}

@OptIn(ExperimentalCoroutinesApi::class)
fun runTestAsync(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Unit
) = runTest(context) {
    withContext(Dispatchers.IO, block)
}
