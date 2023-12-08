package coil3.test

import coil3.PlatformContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

expect abstract class RobolectricTest()

expect val context: PlatformContext

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
