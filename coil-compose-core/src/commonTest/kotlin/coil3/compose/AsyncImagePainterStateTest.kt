package coil3.compose

import coil3.ImageLoader
import coil3.compose.AsyncImagePainter.Input
import coil3.compose.AsyncImagePainter.State
import coil3.decode.DataSource
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.ImageRequest
import coil3.test.utils.FakeImage
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class AsyncImagePainterStateTest : RobolectricTest() {

    private var executions = 0
    private val fetchGate = MutableStateFlow(true)
    private val imageLoader = ImageLoader.Builder(context)
        .diskCache(null)
        .memoryCache(null)
        .build()

    /** Regression test: https://github.com/coil-kt/coil/issues/2959 */
    @Test
    fun doesNotRestartACompletedRequestWhenItIsRememberedAgain() = runTest {
        val painter = newPainter(newInput())
        painter.scope = backgroundScope

        painter.onRemembered()
        assertIs<State.Success>(painter.state.first { it is State.Success })
        assertEquals(1, executions)

        // Simulate the painter's modifier node being detached and re-attached without the painter
        // itself being forgotten, e.g. by 'movableContentOf'.
        painter.onForgotten()
        painter.onRemembered()

        assertEquals(1, executions)
        assertIs<State.Success>(painter.state.value)
    }

    @Test
    fun restartsAnInProgressRequestWhenItIsRememberedAgain() = runTest {
        val painter = newPainter(newInput())
        painter.scope = backgroundScope
        fetchGate.value = false

        // Forget the painter before its request completes.
        painter.onRemembered()
        painter.onForgotten()
        assertEquals(1, executions)

        painter.onRemembered()
        fetchGate.value = true

        assertIs<State.Success>(painter.state.first { it is State.Success })
        assertEquals(2, executions)
    }

    @Test
    fun restartsTheRequestIfTheInputChanges() = runTest {
        val painter = newPainter(newInput())
        painter.scope = backgroundScope

        painter.onRemembered()
        painter.state.first { it is State.Success }
        assertEquals(1, executions)

        painter._input = newInput(data = "different")
        assertIs<State.Success>(painter.state.first { it is State.Success })
        assertEquals(2, executions)
    }

    @Test
    fun restartAlwaysRestartsTheRequest() = runTest {
        val painter = newPainter(newInput())
        painter.scope = backgroundScope

        painter.onRemembered()
        painter.state.first { it is State.Success }
        assertEquals(1, executions)

        painter.restart()
        painter.state.first { it is State.Success }
        assertEquals(2, executions)
    }

    private fun newPainter(input: Input) = AsyncImagePainter(input)

    private fun newInput(data: Any = Unit) = Input(
        imageLoader = imageLoader,
        request = ImageRequest.Builder(context)
            .data(data)
            .coroutineContext(EmptyCoroutineContext)
            .fetcherFactory<Any> { _, _, _ ->
                Fetcher {
                    executions++
                    fetchGate.first { it }
                    ImageFetchResult(
                        image = FakeImage(),
                        isSampled = false,
                        dataSource = DataSource.MEMORY,
                    )
                }
            }
            .build(),
        modelEqualityDelegate = AsyncImageModelEqualityDelegate.Default,
    )
}
