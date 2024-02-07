package coil3.intercept

import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import coil3.EventListener
import coil3.ImageLoader
import coil3.RealImageLoader
import coil3.asCoilImage
import coil3.decode.DataSource
import coil3.intercept.EngineInterceptor.ExecuteResult
import coil3.key.Keyer
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.RequestService
import coil3.request.transformations
import coil3.size.Size
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import coil3.test.utils.size
import coil3.transform.CircleCropTransformation
import coil3.util.SystemCallbacks
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlinx.coroutines.test.runTest
import org.junit.Test

class EngineInterceptorTest : RobolectricTest() {

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
            request = ImageRequest.Builder(context)
                .data(Unit)
                .transformations(CircleCropTransformation())
                .build(),
            options = Options(context, size = size),
            eventListener = EventListener.NONE,
            logger = null,
        )

        val resultDrawable = result.image.asDrawable(context.resources)
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
            request = ImageRequest.Builder(context)
                .data(Unit)
                .transformations(emptyList())
                .build(),
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
            .build() as RealImageLoader
        val systemCallbacks = SystemCallbacks(imageLoader)
        return EngineInterceptor(
            imageLoader = imageLoader,
            systemCallbacks = systemCallbacks,
            requestService = RequestService(imageLoader, systemCallbacks, null),
            logger = null,
        )
    }

    companion object {
        private const val TEST_KEY = "test_key"
    }
}
