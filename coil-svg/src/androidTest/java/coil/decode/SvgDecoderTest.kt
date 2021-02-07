package coil.decode

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.test.core.app.ApplicationProvider
import coil.bitmap.BitmapPool
import coil.size.PixelSize
import coil.size.Scale
import coil.util.decodeBitmapAsset
import coil.util.isSimilarTo
import kotlinx.coroutines.runBlocking
import okio.buffer
import okio.source
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SvgDecoderTest {

    private lateinit var context: Context
    private lateinit var pool: BitmapPool
    private lateinit var decoder: SvgDecoder

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        pool = BitmapPool(0)
        decoder = SvgDecoder(context)
    }

    @Test
    fun handlesMimeType() {
        var source = context.assets.open("coil_logo.svg").source().buffer()
        assertTrue(decoder.handles(source, "image/svg+xml"))

        source = context.assets.open("coil_logo_250.png").source().buffer()
        assertFalse(decoder.handles(source, "image/png"))
    }

    @Test
    fun doesNotExhaustSource() {
        val source = context.assets.open("document.xml").source().buffer()
        assertFalse(decoder.handles(source, null))
        assertFalse(source.exhausted())
        assertEquals(8192, source.buffer.size) // should buffer exactly 1 segment
    }

    @Test
    fun handlesSource() {
        var source = context.assets.open("coil_logo.svg").source().buffer()
        assertTrue(decoder.handles(source, null))

        source = context.assets.open("coil_logo_250.png").source().buffer()
        assertFalse(decoder.handles(source, null))

        source = context.assets.open("instacart_logo.svg").source().buffer()
        assertTrue(decoder.handles(source, null))

        source = context.assets.open("instacart_logo_326.png").source().buffer()
        assertFalse(decoder.handles(source, null))
    }

    @Test
    fun basic() {
        val source = context.assets.open("coil_logo.svg").source().buffer()
        val (drawable, isSampled) = runBlocking {
            decoder.decode(
                pool = pool,
                source = source,
                size = PixelSize(400, 250), // coil_logo.svg's intrinsic dimensions are 200x200.
                options = Options(context, scale = Scale.FIT)
            )
        }

        assertTrue(isSampled)
        assertTrue(drawable is BitmapDrawable)

        val expected = context.decodeBitmapAsset("coil_logo_250.png")
        assertTrue(drawable.bitmap.isSimilarTo(expected))
    }

    @Test
    fun noViewBox() {
        val source = context.assets.open("instacart_logo.svg").source().buffer()
        val (drawable, isSampled) = runBlocking {
            decoder.decode(
                pool = pool,
                source = source,
                size = PixelSize(326, 50),
                options = Options(context, scale = Scale.FILL)
            )
        }

        assertTrue(isSampled)
        assertTrue(drawable is BitmapDrawable)

        val expected = context.decodeBitmapAsset("instacart_logo_326.png")
        assertTrue(drawable.bitmap.isSimilarTo(expected))
    }
}
