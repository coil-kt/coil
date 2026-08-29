package coil3.test.esmodules

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class CoilCoreEsModulesTest {
    @Test
    fun decodeBasic() = runTest {
        val context = PlatformContext.INSTANCE
        val imageLoader = ImageLoader(context)
        try {
            val request = ImageRequest.Builder(context)
                .data(PNG_DATA_URI)
                .build()
            val result = imageLoader.execute(request)
            if (result is ErrorResult) throw result.throwable

            val image = assertIs<SuccessResult>(result).image
            assertEquals(1, image.width)
            assertEquals(1, image.height)
        } finally {
            imageLoader.shutdown()
        }
    }
}

private const val PNG_DATA_URI =
    "data:image/png;base64," +
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
