package coil.transition

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.test.core.app.ApplicationProvider
import coil.annotation.ExperimentalCoilApi
import coil.decode.DataSource
import coil.drawable.CrossfadeDrawable
import coil.request.ErrorResult
import coil.request.SuccessResult
import coil.util.createTestMainDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoilApi::class, ExperimentalCoroutinesApi::class)
class CrossfadeTransitionTest {

    private lateinit var context: Context

    private lateinit var mainDispatcher: TestCoroutineDispatcher
    private lateinit var transition: CrossfadeTransition

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        mainDispatcher = createTestMainDispatcher()
        transition = CrossfadeTransition()
    }

    @After
    fun after() {
        Dispatchers.resetMain()
    }

    @Test
    fun `success - memory cache`() {
        val drawable = ColorDrawable()
        var onSuccessCalled = false

        runBlocking {
            transition.transition(
                target = createTransitionTarget(
                    onSuccess = { result ->
                        assertFalse(onSuccessCalled)
                        onSuccessCalled = true

                        assertEquals(drawable, result)
                    }
                ),
                result = SuccessResult(drawable, DataSource.MEMORY_CACHE)
            )
        }

        assertTrue(onSuccessCalled)
    }

    @Test
    fun `success - disk`() {
        val drawable = ColorDrawable()
        var onSuccessCalled = false

        runBlocking {
            transition.transition(
                target = createTransitionTarget(
                    onSuccess = { result ->
                        assertFalse(onSuccessCalled)
                        onSuccessCalled = true

                        assertTrue(result is CrossfadeDrawable)
                        assertEquals(drawable, result.end)

                        // Stop the transition early to simulate the end of the animation.
                        result.stop()
                    }
                ),
                result = SuccessResult(drawable, DataSource.DISK)
            )
        }

        assertTrue(onSuccessCalled)
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/304 */
    @Test
    fun `success - view not visible`() {
        val drawable = ColorDrawable()
        val imageView = ImageView(context)
        imageView.isVisible = false
        var onSuccessCalled = false

        runBlocking {
            transition.transition(
                target = createTransitionTarget(
                    imageView = imageView,
                    onSuccess = { result ->
                        assertFalse(onSuccessCalled)
                        onSuccessCalled = true

                        assertFalse(result is CrossfadeDrawable)
                    }
                ),
                result = SuccessResult(drawable, DataSource.NETWORK)
            )
        }
    }

    @Test
    fun `failure - disk`() {
        val drawable = ColorDrawable()
        var onSuccessCalled = false

        runBlocking {
            transition.transition(
                target = createTransitionTarget(
                    onError = { error ->
                        assertFalse(onSuccessCalled)
                        onSuccessCalled = true

                        assertTrue(error is CrossfadeDrawable)
                        assertEquals(drawable, error.end)

                        // Stop the animation early to simulate the end of the animation.
                        error.stop()
                    }
                ),
                result = ErrorResult(drawable, Throwable())
            )
        }

        assertTrue(onSuccessCalled)
    }

    private inline fun createTransitionTarget(
        imageView: ImageView = ImageView(context),
        crossinline onStart: (placeholder: Drawable?) -> Unit = { fail() },
        crossinline onError: (error: Drawable?) -> Unit = { fail() },
        crossinline onSuccess: (result: Drawable) -> Unit = { fail() }
    ): TransitionTarget<*> {
        return object : TransitionTarget<ImageView> {
            override val view = imageView
            override val drawable: Drawable?
                get() = imageView.drawable
            override fun onStart(placeholder: Drawable?) = onStart(placeholder)
            override fun onError(error: Drawable?) = onError(error)
            override fun onSuccess(result: Drawable) = onSuccess(result)
        }
    }
}
