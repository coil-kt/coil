package coil3.test.utils

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.ActivityAction
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.BitmapImage
import coil3.Image
import coil3.PlatformContext
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.sink
import okio.source
import org.junit.Assume
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(AndroidJUnit4::class)
actual abstract class AndroidJUnit4Test

@RunWith(RobolectricTestRunner::class)
actual abstract class RobolectricTest

actual val context: PlatformContext
    get() = ApplicationProvider.getApplicationContext()

actual fun decodeBitmapResource(
    path: String,
): CoilBitmap {
    val options: BitmapFactory.Options = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }

    // Retry multiple times as the emulator can be flaky.
    var failures = 0
    while (true) {
        try {
            val stream = FileSystem.RESOURCES.source(path.toPath()).buffer().inputStream()
            return BitmapFactory.decodeStream(stream, null, options)!!.toCoilBitmap()
        } catch (e: Exception) {
            if (failures++ > 5) throw e
        }
    }
}

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

val Image.bitmap: Bitmap
    get() = (this as BitmapImage).bitmap
