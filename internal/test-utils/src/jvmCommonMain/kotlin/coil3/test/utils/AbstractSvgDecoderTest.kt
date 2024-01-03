package coil3.test.utils

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.size.Dimension
import coil3.size.Scale
import coil3.size.Size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import okio.BufferedSource
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer

abstract class AbstractSvgDecoderTest(
    private val decoderFactory: Decoder.Factory,
) {

    @Test
    fun handlesSvgMimeType() {
        val result = FileSystem.RESOURCES.source("coil_logo.svg".toPath()).buffer()
            .asSourceResult(mimeType = "image/svg+xml")
        assertNotNull(decoderFactory.create(result, Options(context), ImageLoader(context)))
    }

    @Test
    fun doesNotHandlePngMimeType() {
        val result = FileSystem.RESOURCES.source("coil_logo.png".toPath()).buffer()
            .asSourceResult(mimeType = "image/png")
        assertNull(decoderFactory.create(result, Options(context), ImageLoader(context)))
    }

    @Test
    fun doesNotHandleGeneralXmlFile() {
        val source = FileSystem.RESOURCES.source("document.xml".toPath()).buffer()
        val result = source.asSourceResult()
        assertNull(decoderFactory.create(result, Options(context), ImageLoader(context)))
        assertFalse(source.exhausted())
        assertEquals(8192, source.buffer.size) // should buffer exactly 1 segment
    }

    @Test
    fun handlesSvgSource() {
        val options = Options(context)
        val imageLoader = ImageLoader(context)
        var source = FileSystem.RESOURCES.source("coil_logo.svg".toPath()).buffer()
        assertNotNull(decoderFactory.create(source.asSourceResult(), options, imageLoader))

        source = FileSystem.RESOURCES.source("coil_logo.png".toPath()).buffer()
        assertNull(decoderFactory.create(source.asSourceResult(), options, imageLoader))

        source = FileSystem.RESOURCES.source("instacart_logo.svg".toPath()).buffer()
        assertNotNull(decoderFactory.create(source.asSourceResult(), options, imageLoader))

        source = FileSystem.RESOURCES.source("instacart_logo.png".toPath()).buffer()
        assertNull(decoderFactory.create(source.asSourceResult(), options, imageLoader))
    }

    @Test
    fun basic() = runTest {
        val source = FileSystem.RESOURCES.source("coil_logo.svg".toPath()).buffer()
        val options = Options(
            context = context,
            size = Size(400, 250), // coil_logo.svg's intrinsic dimensions are 200x200.
            scale = Scale.FIT,
        )
        val result = assertNotNull(
            decoderFactory.create(
                result = source.asSourceResult(),
                options = options,
                imageLoader = ImageLoader(context),
            )?.decode(),
        )

        assertTrue(result.isSampled)

        val expected = decodeBitmapResource("coil_logo.png")
        result.image.asCoilBitmap().assertIsSimilarTo(expected)
    }

    @Test
    fun noViewBox() = runTest {
        val source = FileSystem.RESOURCES.source("instacart_logo.svg".toPath()).buffer()
        val options = Options(
            context = context,
            size = Size(600, 96),
            scale = Scale.FILL,
        )
        val result = assertNotNull(
            decoderFactory.create(
                result = source.asSourceResult(),
                options = options,
                imageLoader = ImageLoader(context),
            )?.decode(),
        )

        assertTrue(result.isSampled)

        val expected = decodeBitmapResource("instacart_logo.png")
        result.image.asCoilBitmap().assertIsSimilarTo(expected)
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/1246 */
    @Test
    fun oneDimensionIsUndefined() = runTest {
        val source = FileSystem.RESOURCES.source("coil_logo.svg".toPath()).buffer()
        val options = Options(
            context = context,
            // coil_logo.svg's intrinsic dimensions are 200x200.
            size = Size(Dimension.Undefined, 250),
            scale = Scale.FIT,
        )
        val result = assertNotNull(
            decoderFactory.create(
                result = source.asSourceResult(),
                options = options,
                imageLoader = ImageLoader(context),
            )?.decode(),
        )

        assertTrue(result.isSampled)

        val expected = decodeBitmapResource("coil_logo.png")
        result.image.asCoilBitmap().assertIsSimilarTo(expected)
    }

    @Test
    fun resultImageIsShareable() = runTest {
        val source = FileSystem.RESOURCES.source("coil_logo.svg".toPath()).buffer()
            .asSourceResult(mimeType = "image/svg+xml")
        val result = decoderFactory.create(
            result = source,
            options = Options(context),
            imageLoader = ImageLoader(context),
        )?.decode()
        assertNotNull(result)
        assertTrue(result.image.shareable)
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
