package coil.util

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.ActivityAction
import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okio.buffer
import okio.sink
import okio.source
import org.junit.Assume

/** Alias for [Assume.assumeTrue]. */
fun assumeTrue(actual: Boolean, message: String = "") {
    if (message.isBlank()) {
        Assume.assumeTrue(actual)
    } else {
        Assume.assumeTrue(message, actual)
    }
}

/** Launch [T] and invoke [action]. */
inline fun <reified T : Activity> launchActivity(action: ActivityAction<T>) {
    launchActivity<T>().use { scenario ->
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.onActivity(action)
    }
}

/**
 * Get a reference to the [ActivityScenario]'s [Activity].
 *
 * NOTE: [ActivityScenario.onActivity] explicitly recommends against holding a
 * reference to the [Activity] outside of its scope. However, it should be safe
 * as long we use [ActivityScenarioRule].
 */
val <T : Activity> ActivityScenario<T>.activity: T
    get() {
        lateinit var activity: T
        runBlocking(Dispatchers.Main.immediate) {
            // onActivity is executed synchronously when called from the main thread.
            onActivity { activity = it }
        }
        return activity
    }

fun Context.decodeBitmapAsset(
    fileName: String,
    options: BitmapFactory.Options = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.ARGB_8888
    },
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
