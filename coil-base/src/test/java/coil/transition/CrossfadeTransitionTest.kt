package coil.transition

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import coil.decode.DataSource
import coil.drawable.CrossfadeDrawable
import coil.request.ErrorResult
import coil.request.SuccessResult
import coil.util.createRequest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CrossfadeTransitionTest {

    private lateinit var context: Context
    private lateinit var transitionFactory: CrossfadeTransition.Factory

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        transitionFactory = CrossfadeTransition.Factory()
    }

    @Test
    fun `success - memory cache`() {
        val drawable = ColorDrawable()
        var onSuccessCalled = false

        val target = createTransitionTarget(
            onSuccess = { result ->
                assertFalse(onSuccessCalled)
                onSuccessCalled = true
                assertEquals(drawable, result)
            }
        )
        val result = SuccessResult(
            drawable = drawable,
            request = createRequest(context),
            dataSource = DataSource.MEMORY_CACHE
        )
        transitionFactory.create(target, result).transition()

        assertTrue(onSuccessCalled)
    }

    @Test
    fun `success - disk`() {
        val drawable = ColorDrawable()
        var onSuccessCalled = false

        val target = createTransitionTarget(
            onSuccess = { result ->
                assertFalse(onSuccessCalled)
                onSuccessCalled = true

                assertTrue(result is CrossfadeDrawable)

                // Stop the transition early to simulate the end of the animation.
                result.stop()
            }
        )
        val result = SuccessResult(
            drawable = drawable,
            request = createRequest(context),
            dataSource = DataSource.DISK
        )
        transitionFactory.create(target, result).transition()

        assertTrue(onSuccessCalled)
    }

    @Test
    fun `failure - disk`() = runTest {
        val drawable = ColorDrawable()
        var onSuccessCalled = false

        transitionFactory.create(
            target = createTransitionTarget(
                onError = { error ->
                    assertFalse(onSuccessCalled)
                    assertTrue(error !is CrossfadeDrawable)
                    onSuccessCalled = true
                }
            ),
            result = ErrorResult(
                drawable = drawable,
                request = createRequest(context),
                throwable = Throwable()
            )
        ).transition()

        assertTrue(onSuccessCalled)
    }

    private inline fun createTransitionTarget(
        imageView: ImageView = ImageView(context),
        crossinline onStart: (placeholder: Drawable?) -> Unit = { fail() },
        crossinline onError: (error: Drawable?) -> Unit = { fail() },
        crossinline onSuccess: (result: Drawable) -> Unit = { fail() }
    ) = object : TransitionTarget {
        override val view = imageView
        override val drawable: Drawable? get() = imageView.drawable
        override fun onStart(placeholder: Drawable?) = onStart(placeholder)
        override fun onError(error: Drawable?) = onError(error)
        override fun onSuccess(result: Drawable) = onSuccess(result)
    }
}
