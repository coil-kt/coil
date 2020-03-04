package coil.transition

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import coil.annotation.ExperimentalCoilApi
import coil.drawable.CrossfadeDrawable
import coil.util.createTestMainDispatcher
import coil.util.error
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
    fun `success - isMemoryCache=true`() {
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
                result = TransitionResult.Success(drawable, true)
            )
        }

        assertTrue(onSuccessCalled)
    }

    @Test
    fun `success - isMemoryCache=false`() {
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
                result = TransitionResult.Success(drawable, false)
            )
        }

        assertTrue(onSuccessCalled)
    }

    @Test
    fun failure() {
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
                result = TransitionResult.Error(drawable)
            )
        }

        assertTrue(onSuccessCalled)
    }

    private inline fun createTransitionTarget(
        crossinline onStart: (placeholder: Drawable?) -> Unit = { error() },
        crossinline onError: (error: Drawable?) -> Unit = { error() },
        crossinline onSuccess: (result: Drawable) -> Unit = { error() }
    ): TransitionTarget<*> {
        return object : TransitionTarget<ImageView> {
            override val view = ImageView(context)
            override val drawable: Drawable?
                get() = view.drawable
            override fun onStart(placeholder: Drawable?) = onStart(placeholder)
            override fun onError(error: Drawable?) = onError(error)
            override fun onSuccess(result: Drawable) = onSuccess(result)
        }
    }
}
