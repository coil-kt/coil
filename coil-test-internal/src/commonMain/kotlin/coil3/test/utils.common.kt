package coil3.test

import coil3.Image
import coil3.PlatformContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

expect abstract class RobolectricTest()

expect val context: PlatformContext

const val DEFAULT_FAKE_IMAGE_SIZE = 4 * 100 * 100

fun FakeImage(
    width: Int = 100,
    height: Int = 100,
    size: Long = 4L * width * height,
    shareable: Boolean = true,
) = object : Image {
    override val size get() = size
    override val width get() = width
    override val height get() = height
    override val shareable get() = shareable
}

fun runTestMain(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Unit
) = runTest(context) {
    withContext(Dispatchers.Main.immediate, block)
}

fun runTestAsync(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Unit
) = runTest(context) {
    withContext(Dispatchers.Default, block)
}
