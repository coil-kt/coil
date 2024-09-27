package coil3.svg

import coil3.ImageLoader
import coil3.request.Options
import coil3.size.Size
import coil3.test.utils.AbstractSvgDecoderTest
import coil3.test.utils.context
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer

class SvgDecoderTestAndroid : AbstractSvgDecoderTest(SvgDecoder.Factory()) {

    @Test
    fun scaleToDensity_true() = runTest {
        val source = FileSystem.RESOURCES.source("coil_logo.svg".toPath()).buffer()
            .asSourceResult(mimeType = "image/svg+xml")
        val result = SvgDecoder.Factory(scaleToDensity = true).create(
            result = source,
            options = Options(context, size = Size.ORIGINAL),
            imageLoader = ImageLoader(context),
        )?.decode()
        assertNotNull(result)
        assertEquals(368, result.image.width)
        assertEquals(368, result.image.height)
    }
}
