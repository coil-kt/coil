package coil3.decode

import coil3.BitmapImage
import coil3.ImageLoader
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.size.Dimension
import coil3.size.Scale
import coil3.size.Size
import coil3.test.utils.assertIsSimilarTo
import coil3.test.utils.context
import coil3.test.utils.decodeBitmapAsset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import okio.BufferedSource
import okio.FileSystem
import okio.buffer
import okio.source
import org.junit.Test

class SvgDecoderTest {

    private val decoderFactory = SvgDecoder.Factory()

    @Test
    fun handlesSvgMimeType() {
        val result = context.assets.open("coil_logo.svg").source().buffer()
            .asSourceResult(mimeType = "image/svg+xml")
        assertNotNull(decoderFactory.create(result, Options(context), ImageLoader(context)))
    }

    @Test
    fun doesNotHandlePngMimeType() {
        val result = context.assets.open("coil_logo.png").source().buffer()
            .asSourceResult(mimeType = "image/png")
        assertNull(decoderFactory.create(result, Options(context), ImageLoader(context)))
    }

    @Test
    fun doesNotHandleGeneralXmlFile() {
        val source = context.assets.open("document.xml").source().buffer()
        val result = source.asSourceResult()
        assertNull(decoderFactory.create(result, Options(context), ImageLoader(context)))
        assertFalse(source.exhausted())
        assertEquals(8192, source.buffer.size) // should buffer exactly 1 segment
    }

    @Test
    fun handlesSvgSource() {
        val options = Options(context)
        val imageLoader = ImageLoader(context)
        var source = context.assets.open("coil_logo.svg").source().buffer()
        assertNotNull(decoderFactory.create(source.asSourceResult(), options, imageLoader))

        source = context.assets.open("coil_logo.png").source().buffer()
        assertNull(decoderFactory.create(source.asSourceResult(), options, imageLoader))

        source = context.assets.open("instacart_logo.svg").source().buffer()
        assertNotNull(decoderFactory.create(source.asSourceResult(), options, imageLoader))

        source = context.assets.open("instacart_logo.png").source().buffer()
        assertNull(decoderFactory.create(source.asSourceResult(), options, imageLoader))
    }

    @Test
    fun basic() = runTest {
        val source = context.assets.open("coil_logo.svg").source().buffer()
        val options = Options(
            context = context,
            size = Size(400, 250), // coil_logo.svg's intrinsic dimensions are 200x200.
            scale = Scale.FIT
        )
        val result = assertNotNull(
            decoderFactory.create(
                result = source.asSourceResult(),
                options = options,
                imageLoader = ImageLoader(context)
            )?.decode()
        )

        assertTrue(result.isSampled)
        val image = assertIs<BitmapImage>(result.image)

        val expected = context.decodeBitmapAsset("coil_logo.png")
        image.bitmap.assertIsSimilarTo(expected)
    }

    @Test
    fun noViewBox() = runTest {
        val source = context.assets.open("instacart_logo.svg").source().buffer()
        val options = Options(
            context = context,
            size = Size(600, 96),
            scale = Scale.FILL
        )
        val result = assertNotNull(
            decoderFactory.create(
                result = source.asSourceResult(),
                options = options,
                imageLoader = ImageLoader(context)
            )?.decode()
        )

        assertTrue(result.isSampled)
        val image = assertIs<BitmapImage>(result.image)

        val expected = context.decodeBitmapAsset("instacart_logo.png")
        image.bitmap.assertIsSimilarTo(expected)
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/1246 */
    @Test
    fun oneDimensionIsUndefined() = runTest {
        val source = context.assets.open("coil_logo.svg").source().buffer()
        val options = Options(
            context = context,
            // coil_logo.svg's intrinsic dimensions are 200x200.
            size = Size(Dimension.Undefined, 250),
            scale = Scale.FIT
        )
        val result = assertNotNull(
            decoderFactory.create(
                result = source.asSourceResult(),
                options = options,
                imageLoader = ImageLoader(context)
            )?.decode()
        )

        assertTrue(result.isSampled)
        val image = assertIs<BitmapImage>(result.image)

        val expected = context.decodeBitmapAsset("coil_logo.png")
        image.bitmap.assertIsSimilarTo(expected)
    }

    private fun BufferedSource.asSourceResult(
        mimeType: String? = null,
        dataSource: DataSource = DataSource.DISK,
    ) = SourceFetchResult(
        source = ImageSource(this, FileSystem.SYSTEM),
        mimeType = mimeType,
        dataSource = dataSource,
    )
}
