package coil.decode

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.fetch.SourceResult
import coil.request.Options
import coil.size.Dimension
import coil.size.Scale
import coil.size.Size
import coil.util.assertIsSimilarTo
import coil.util.decodeBitmapAsset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okio.Buffer
import okio.BufferedSource
import okio.buffer
import okio.source
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SvgDecoderTest {

    private lateinit var context: Context
    private lateinit var decoderFactory: SvgDecoder.Factory

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        decoderFactory = SvgDecoder.Factory()
    }

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

    /** Regression test: https://github.com/coil-kt/coil/issues/1154 */
    @Test
    fun isSvg_newLine() {
        val text = "<svg\n" +
            "   xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
            "   xmlns:cc=\"http://creativecommons.org/ns#\"\n" +
            "   xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
            "   xmlns:svg=\"http://www.w3.org/2000/svg\"\n" +
            "   xmlns=\"http://www.w3.org/2000/svg\"\n" +
            "   xmlns:inkscape=\"http://www.inkscape.org/namespaces/inkscape\"/>"
        val buffer = Buffer().apply { writeUtf8(text) }

        assertTrue(DecodeUtils.isSvg(buffer))
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
        val drawable = assertIs<BitmapDrawable>(result.drawable)

        val expected = context.decodeBitmapAsset("coil_logo.png")
        drawable.bitmap.assertIsSimilarTo(expected)
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
        val drawable = assertIs<BitmapDrawable>(result.drawable)

        val expected = context.decodeBitmapAsset("instacart_logo.png")
        drawable.bitmap.assertIsSimilarTo(expected)
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/1246 */
    @Test
    fun oneDimensionIsUndefined() = runTest {
        val source = context.assets.open("coil_logo.svg").source().buffer()
        val options = Options(
            context = context,
            size = Size(Dimension.Undefined, 250), // coil_logo.svg's intrinsic dimensions are 200x200.
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
        val drawable = assertIs<BitmapDrawable>(result.drawable)

        val expected = context.decodeBitmapAsset("coil_logo.png")
        drawable.bitmap.assertIsSimilarTo(expected)
    }

    private fun BufferedSource.asSourceResult(
        mimeType: String? = null,
        dataSource: DataSource = DataSource.DISK
    ) = SourceResult(
        source = ImageSource(this, context),
        mimeType = mimeType,
        dataSource = dataSource
    )
}
