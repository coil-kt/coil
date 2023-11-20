package coil.transition

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import coil.Image
import coil.asCoilImage
import coil.decode.DataSource
import coil.drawable
import coil.request.ErrorResult
import coil.request.SuccessResult
import coil.util.createRequest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertIsNot
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
                assertEquals(drawable, result.drawable)
            }
        )
        val result = SuccessResult(
            image = drawable.asCoilImage(),
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

                val crossfadeDrawable = assertIs<CrossfadeDrawable>(result.drawable)

                // Stop the transition early to simulate the end of the animation.
                crossfadeDrawable.stop()
            }
        )
        val result = SuccessResult(
            image = drawable.asCoilImage(),
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
                    assertIsNot<CrossfadeDrawable>(error?.drawable)
                    onSuccessCalled = true
                }
            ),
            result = ErrorResult(
                image = drawable.asCoilImage(),
                request = createRequest(context),
                throwable = Throwable()
            )
        ).transition()

        assertTrue(onSuccessCalled)
    }

    private inline fun createTransitionTarget(
        imageView: ImageView = ImageView(context),
        crossinline onStart: (placeholder: Image?) -> Unit = { fail() },
        crossinline onError: (error: Image?) -> Unit = { fail() },
        crossinline onSuccess: (result: Image) -> Unit = { fail() }
    ) = object : TransitionTarget {
        override val view = imageView
        override val drawable: Drawable? get() = imageView.drawable
        override fun onStart(placeholder: Image?) = onStart(placeholder)
        override fun onError(error: Image?) = onError(error)
        override fun onSuccess(result: Image) = onSuccess(result)
    }
}
