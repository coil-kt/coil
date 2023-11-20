package coil.intercept

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import androidx.test.core.app.ApplicationProvider
import coil.EventListener
import coil.ImageLoader
import coil.RealImageLoader
import coil.asCoilImage
import coil.decode.DataSource
import coil.drawable
import coil.intercept.EngineInterceptor.ExecuteResult
import coil.key.Keyer
import coil.request.Options
import coil.request.RequestService
import coil.request.transformations
import coil.size.Size
import coil.transform.CircleCropTransformation
import coil.util.SystemCallbacks
import coil.util.createRequest
import coil.util.size
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EngineInterceptorTest {

    private lateinit var context: Context

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `applyTransformations - transformations convert drawable to bitmap`() = runTest {
        val image = ColorDrawable(Color.BLACK).asCoilImage()
        val size = Size(100, 100)
        val result = transform(
            result = ExecuteResult(
                image = image,
                isSampled = false,
                dataSource = DataSource.MEMORY,
                diskCacheKey = null
            ),
            request = createRequest(context) {
                transformations(CircleCropTransformation())
            },
            options = Options(context, size = size),
            eventListener = EventListener.NONE,
            logger = null,
        )

        val resultDrawable = result.image.drawable
        assertIs<BitmapDrawable>(resultDrawable)
        assertEquals(resultDrawable.bitmap.size, size)
    }

    @Test
    fun `applyTransformations - empty transformations does not convert drawable to bitmap`() = runTest {
        val image = ColorDrawable(Color.BLACK).asCoilImage()
        val result = transform(
            result = ExecuteResult(
                image = image,
                isSampled = false,
                dataSource = DataSource.MEMORY,
                diskCacheKey = null,
            ),
            request = createRequest(context) {
                transformations(emptyList())
            },
            options = Options(context, size = Size(100, 100)),
            eventListener = EventListener.NONE,
            logger = null,
        )

        assertSame(image, result.image)
    }

    private fun newInterceptor(key: String? = TEST_KEY): EngineInterceptor {
        val imageLoader = ImageLoader.Builder(context)
            .components {
                add(Keyer { _: Any, _ -> key })
            }
            .build()
        val options = (imageLoader as RealImageLoader).options
        val systemCallbacks = SystemCallbacks(options)
        return EngineInterceptor(
            imageLoader = imageLoader,
            requestService = RequestService(imageLoader, systemCallbacks, null),
            logger = null,
        )
    }

    companion object {
        private const val TEST_KEY = "test_key"
    }
}
