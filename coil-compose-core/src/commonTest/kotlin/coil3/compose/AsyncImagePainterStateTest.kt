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
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class AsyncImagePainterStateTest : RobolectricTest() {

    private var executions = 0
    private val fetchGate = MutableStateFlow(true)
    private val imageLoader = ImageLoader.Builder(context)
        .diskCache(null)
        .memoryCache(null)
        .build()

    /**
     * Regression test: https://github.com/coil-kt/coil/issues/2959
     *
     * `movableContentOf` detaches and re-attaches a modifier node without forgetting the painter
     * itself, which used to restart a request that had already completed and flicker.
     */
    @Test
    fun doesNotRestartACompletedRequestWhenItIsRememberedAgain() = runTest {
        val states = mutableListOf<State>()
        val painter = newPainter().apply {
            scope = backgroundScope
            onState = { states += it }
        }

        painter.onRemembered()
        assertIs<State.Success>(painter.state.first { it is State.Success })
        assertEquals(1, executions)

        // Simulate the painter's modifier node being detached and re-attached.
        states.clear()
        painter.onForgotten()
        painter.onRemembered()
        runCurrent()

        assertEquals(1, executions)
        assertTrue(states.isEmpty())
        assertIs<State.Success>(painter.state.value)
    }

    @Test
    fun restartsAnInProgressRequestWhenItIsRememberedAgain() = runTest {
        val painter = newPainter()
        painter.scope = backgroundScope
        fetchGate.value = false

        // Forget the painter before its request completes.
        painter.onRemembered()
        runCurrent()
        painter.onForgotten()
        assertEquals(1, executions)

        painter.onRemembered()
        fetchGate.value = true

        assertIs<State.Success>(painter.state.first { it is State.Success })
        assertEquals(2, executions)
    }

    @Test
    fun restartsTheRequestIfTheInputChanges() = runTest {
        val painter = newPainter()
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
        val painter = newPainter()
        painter.scope = backgroundScope

        painter.onRemembered()
        painter.state.first { it is State.Success }
        assertEquals(1, executions)

        painter.restart()
        assertIs<State.Success>(painter.state.first { it is State.Success })
        assertEquals(2, executions)
    }

    /**
     * A request must not record itself as completed if a callback (e.g. `onState`) started a newer
     * request while it was delivering its result.
     */
    @Test
    fun doesNotRecordARequestSupersededByARestartInACallback() = runTest {
        fetchGate.value = false

        lateinit var painter: AsyncImagePainter
        painter = newPainter().apply {
            scope = backgroundScope
            onState = { state ->
                if (state is State.Success && executions == 1) {
                    fetchGate.value = false
                    painter.restart()
                }
            }
        }

        painter.onRemembered()
        runCurrent()
        assertEquals(1, executions)

        fetchGate.value = true
        runCurrent()
        assertEquals(2, executions)
        assertIs<State.Loading>(painter.state.value)

        // Detach while the callback-triggered request is still loading. It must be restarted.
        painter.onForgotten()
        painter.onRemembered()
        runCurrent()
        assertIs<State.Loading>(painter.state.value)
        assertEquals(3, executions)

        fetchGate.value = true
        assertIs<State.Success>(painter.state.first { it is State.Success })
    }

    /** `restart()` must invalidate a completed request even while the painter is forgotten. */
    @Test
    fun restartWhileForgottenLaunchesANewRequestWhenRememberedAgain() = runTest {
        val painter = newPainter()
        painter.scope = backgroundScope

        painter.onRemembered()
        painter.state.first { it is State.Success }
        assertEquals(1, executions)

        painter.onForgotten()
        painter.restart()
        painter.onRemembered()

        assertIs<State.Success>(painter.state.first { it is State.Success })
        assertEquals(2, executions)
    }

    private fun newPainter() = AsyncImagePainter(newInput())

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
