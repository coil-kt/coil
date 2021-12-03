package coil.intercept

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import androidx.test.core.app.ApplicationProvider
import coil.EventListener
import coil.ImageLoader
import coil.RealImageLoader
import coil.decode.DataSource
import coil.intercept.EngineInterceptor.ExecuteResult
import coil.key.Keyer
import coil.request.Options
import coil.request.RequestService
import coil.size
import coil.size.Size
import coil.transform.CircleCropTransformation
import coil.util.SystemCallbacks
import coil.util.createRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class EngineInterceptorTest {

    private lateinit var context: Context

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `applyTransformations - transformations convert drawable to bitmap`() = runTest {
        val interceptor = newInterceptor()
        val drawable = ColorDrawable(Color.BLACK)
        val size = Size(100, 100)
        val result = interceptor.transform(
            result = ExecuteResult(
                drawable = drawable,
                isSampled = false,
                dataSource = DataSource.MEMORY,
                diskCacheKey = null
            ),
            request = createRequest(context) {
                transformations(CircleCropTransformation())
            },
            options = Options(context, size = size),
            eventListener = EventListener.NONE
        )

        val resultDrawable = result.drawable
        assertTrue(resultDrawable is BitmapDrawable)
        assertEquals(resultDrawable.bitmap.size, size)
    }

    @Test
    fun `applyTransformations - empty transformations does not convert drawable to bitmap`() = runTest {
        val interceptor = newInterceptor()
        val drawable = ColorDrawable(Color.BLACK)
        val result = interceptor.transform(
            result = ExecuteResult(
                drawable = drawable,
                isSampled = false,
                dataSource = DataSource.MEMORY,
                diskCacheKey = null
            ),
            request = createRequest(context) {
                transformations(emptyList())
            },
            options = Options(context, size = Size(100, 100)),
            eventListener = EventListener.NONE
        )

        assertSame(drawable, result.drawable)
    }

    private fun newInterceptor(key: String? = TEST_KEY): EngineInterceptor {
        val imageLoader = ImageLoader.Builder(context)
            .components {
                add(Keyer { _: Any, _ -> key })
            }
            .build()
        val systemCallbacks = SystemCallbacks(imageLoader as RealImageLoader, context, true)
        return EngineInterceptor(
            imageLoader = imageLoader,
            requestService = RequestService(imageLoader, systemCallbacks, null),
            logger = null
        )
    }

    companion object {
        private const val TEST_KEY = "test_key"
    }
}
