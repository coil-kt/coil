package coil3.intercept

import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import coil3.EventListener
import coil3.ImageLoader
import coil3.RealImageLoader
import coil3.asCoilImage
import coil3.decode.DataSource
import coil3.drawable
import coil3.intercept.EngineInterceptor.ExecuteResult
import coil3.key.Keyer
import coil3.request.Options
import coil3.request.RequestService
import coil3.request.transformations
import coil3.size.Size
import coil3.transform.CircleCropTransformation
import coil3.util.SystemCallbacks
import coil3.util.WithPlatformContext
import coil3.util.createRequest
import coil3.util.size
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EngineInterceptorTest : WithPlatformContext() {

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
