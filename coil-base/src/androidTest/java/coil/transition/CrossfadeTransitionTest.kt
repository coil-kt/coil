package coil.transition

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.activityScenarioRule
import coil.decode.DataSource
import coil.drawable.CrossfadeDrawable
import coil.request.ErrorResult
import coil.request.ImageResult.Metadata
import coil.request.SuccessResult
import coil.util.TestActivity
import coil.util.activity
import coil.util.createRequest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class CrossfadeTransitionTest {

    private lateinit var context: Context
    private lateinit var transition: CrossfadeTransition

    @get:Rule
    val activityRule = activityScenarioRule<TestActivity>()

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        transition = CrossfadeTransition()
    }

    @Test
    fun successMemoryCache() {
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
                result = SuccessResult(
                    drawable = drawable,
                    request = createRequest(context),
                    metadata = Metadata(
                        memoryCacheKey = null,
                        isSampled = false,
                        dataSource = DataSource.MEMORY_CACHE,
                        isPlaceholderMemoryCacheKeyPresent = false
                    )
                )
            )
        }

        assertTrue(onSuccessCalled)
    }

    @Test
    fun successDisk() {
        val drawable = ColorDrawable()
        var onSuccessCalled = false

        runBlocking {
            transition.transition(
                target = createTransitionTarget(
                    onSuccess = { result ->
                        assertFalse(onSuccessCalled)
                        onSuccessCalled = true

                        assertTrue(result is CrossfadeDrawable)

                        // Stop the transition early to simulate the end of the animation.
                        result.stop()
                    }
                ),
                result = SuccessResult(
                    drawable = drawable,
                    request = createRequest(context),
                    metadata = Metadata(
                        memoryCacheKey = null,
                        isSampled = false,
                        dataSource = DataSource.DISK,
                        isPlaceholderMemoryCacheKeyPresent = false
                    )
                )
            )
        }

        assertTrue(onSuccessCalled)
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/304 */
    @Test
    fun successViewNotShown() {
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
                result = SuccessResult(
                    drawable = drawable,
                    request = createRequest(context),
                    metadata = Metadata(
                        memoryCacheKey = null,
                        isSampled = false,
                        dataSource = DataSource.NETWORK,
                        isPlaceholderMemoryCacheKeyPresent = false
                    )
                )
            )
        }
    }

    @Test
    fun failureDisk() {
        val drawable = ColorDrawable()
        var onSuccessCalled = false

        runBlocking {
            transition.transition(
                target = createTransitionTarget(
                    onError = { error ->
                        assertFalse(onSuccessCalled)
                        onSuccessCalled = true

                        assertTrue(error is CrossfadeDrawable)

                        // Stop the animation early to simulate the end of the animation.
                        error.stop()
                    }
                ),
                result = ErrorResult(
                    drawable = drawable,
                    request = createRequest(context),
                    throwable = Throwable()
                )
            )
        }

        assertTrue(onSuccessCalled)
    }

    private inline fun createTransitionTarget(
        imageView: ImageView = activityRule.scenario.activity.imageView,
        crossinline onStart: (placeholder: Drawable?) -> Unit = { fail() },
        crossinline onError: (error: Drawable?) -> Unit = { fail() },
        crossinline onSuccess: (result: Drawable) -> Unit = { fail() }
    ): TransitionTarget {
        return object : TransitionTarget {
            override val view = imageView
            override val drawable: Drawable? get() = imageView.drawable
            override fun onStart(placeholder: Drawable?) = onStart(placeholder)
            override fun onError(error: Drawable?) = onError(error)
            override fun onSuccess(result: Drawable) = onSuccess(result)
        }
    }
}
