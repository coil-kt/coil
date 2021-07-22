package coil.decode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build.VERSION.SDK_INT
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.fetch.SourceResult
import coil.request.Options
import coil.size
import coil.size.OriginalSize
import coil.size.PixelSize
import coil.size.Scale
import coil.size.Size
import coil.util.decodeBitmapAsset
import coil.util.isSimilarTo
import kotlinx.coroutines.runBlocking
import okio.buffer
import okio.source
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BitmapFactoryDecoderTest {

    private lateinit var context: Context
    private lateinit var decoderFactory: BitmapFactoryDecoder.Factory

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        decoderFactory = BitmapFactoryDecoder.Factory()
    }

    @Test
    fun basic() {
        val (drawable, isSampled) = decode(
            assetName = "normal.jpg",
            size = PixelSize(100, 100)
        )

        assertTrue(isSampled)
        assertTrue(drawable is BitmapDrawable)
        assertEquals(PixelSize(100, 125), drawable.bitmap.size)
        assertEquals(Bitmap.Config.ARGB_8888, drawable.bitmap.config)
    }

    @Test
    fun malformedImageThrows() {
        assertFailsWith<IllegalStateException> {
            decode(
                assetName = "malformed.jpg",
                size = PixelSize(100, 100)
            )
        }
    }

    @Test
    fun resultIsSampledIfGreaterThanHalfSize() {
        val (drawable, isSampled) = decode(
            assetName = "normal.jpg",
            size = PixelSize(600, 600)
        )

        assertTrue(isSampled)
        assertTrue(drawable is BitmapDrawable)
        assertEquals(PixelSize(600, 750), drawable.bitmap.size)
    }

    @Test
    fun originalSizeDimensionsAreResolvedCorrectly() {
        val size = OriginalSize
        val normal = decodeBitmap("normal.jpg", size)
        assertEquals(PixelSize(1080, 1350), normal.size)
    }

    @Test
    fun exifTransformationsAreAppliedCorrectly() {
        val size = PixelSize(500, 500)
        val normal = decodeBitmap("normal.jpg", size)

        for (index in 1..8) {
            val other = decodeBitmap("exif/$index.jpg", size)
            assertTrue(normal.isSimilarTo(other), "Image with index $index is incorrect.")
        }
    }

    @Test
    fun largeExifMetadata() {
        val size = PixelSize(500, 500)
        val expected = decodeBitmap("exif/large_metadata_normalized.jpg", size)
        val actual = decodeBitmap("exif/large_metadata.jpg", size)
        assertTrue(expected.isSimilarTo(actual))
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/619 */
    @Test
    fun heicExifMetadata() {
        // HEIC files are not supported before API 30.
        assumeTrue(SDK_INT >= 30)

        // Ensure this completes and doesn't end up in an infinite loop.
        val normal = context.decodeBitmapAsset("exif/basic.heic")
        val actual = decodeBitmap("exif/basic.heic", OriginalSize)
        assertTrue(normal.isSimilarTo(actual))
    }

    @Test
    fun allowInexactSize_true() {
        val result = decodeBitmap(
            assetName = "normal.jpg",
            options = Options(
                context = context,
                size = PixelSize(1500, 1500),
                scale = Scale.FIT,
                allowInexactSize = true
            )
        )
        assertEquals(PixelSize(1080, 1350), result.size)
    }

    @Test
    fun allowInexactSize_false() {
        val result = decodeBitmap(
            assetName = "normal.jpg",
            options = Options(
                context = context,
                size = PixelSize(1500, 1500),
                scale = Scale.FIT,
                allowInexactSize = false
            )
        )
        assertEquals(PixelSize(1200, 1500), result.size)
    }

    @Test
    fun allowRgb565_true() {
        val result = decodeBitmap(
            assetName = "normal.jpg",
            options = Options(
                context = context,
                size = PixelSize(500, 500),
                scale = Scale.FILL,
                allowRgb565 = true
            )
        )
        assertEquals(PixelSize(500, 625), result.size)
        assertEquals(Bitmap.Config.RGB_565, result.config)
    }

    @Test
    fun allowRgb565_false() {
        val result = decodeBitmap(
            assetName = "normal.jpg",
            options = Options(
                context = context,
                size = PixelSize(500, 500),
                scale = Scale.FILL,
                allowRgb565 = false
            )
        )
        assertEquals(PixelSize(500, 625), result.size)
        assertEquals(Bitmap.Config.ARGB_8888, result.config)
    }

    @Test
    fun premultipliedAlpha_true() {
        val result = decodeBitmap(
            assetName = "normal_alpha.png",
            options = Options(
                context = context,
                size = PixelSize(400, 200),
                scale = Scale.FILL,
                premultipliedAlpha = true
            )
        )
        assertEquals(PixelSize(400, 200), result.size)
        assertTrue(result.isPremultiplied)
    }

    @Test
    fun premultipliedAlpha_false() {
        val result = decodeBitmap(
            assetName = "normal_alpha.png",
            options = Options(
                context = context,
                size = PixelSize(400, 200),
                scale = Scale.FILL,
                premultipliedAlpha = false
            )
        )
        assertEquals(PixelSize(400, 200), result.size)
        assertFalse(result.isPremultiplied)
    }

    @Test
    fun lossyWebP() {
        val expectedBitmap = decodeBitmap("normal.jpg", PixelSize(450, 675))
        assertTrue(decodeBitmap("lossy.webp", PixelSize(450, 675)).isSimilarTo(expectedBitmap))
    }

    @Test
    fun png_16bit() {
        // The emulator runs out of memory on pre-23.
        assumeTrue(SDK_INT >= 23)

        val (drawable, isSampled) = decode("16_bit.png", PixelSize(250, 250))

        assertTrue(isSampled)
        assertTrue(drawable is BitmapDrawable)
        assertEquals(PixelSize(250, 250), drawable.bitmap.size)

        val expectedConfig = if (SDK_INT >= 26) Bitmap.Config.RGBA_F16 else Bitmap.Config.ARGB_8888
        assertEquals(expectedConfig, drawable.bitmap.config)
    }

    @Test
    fun largeJpeg() {
        decodeBitmap("large.jpg", PixelSize(1080, 1920))
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/368 */
    @Test
    fun largePng() {
        // Ensure that this doesn't cause an OOM exception - particularly on API 23 and below.
        decodeBitmap("large.png", PixelSize(1080, 1920))
    }

    @Test
    fun largeWebP() {
        decodeBitmap("large.webp", PixelSize(1080, 1920))
    }

    @Test
    fun largeHeic() {
        // HEIC files are not supported before API 30.
        assumeTrue(SDK_INT >= 30)

        decodeBitmap("large.heic", PixelSize(1080, 1920))
    }

    private fun decodeBitmap(assetName: String, size: Size): Bitmap =
        decodeBitmap(assetName, Options(context = context, size = size, scale = Scale.FILL))

    private fun decodeBitmap(assetName: String, options: Options): Bitmap =
        (decode(assetName, options).drawable as BitmapDrawable).bitmap

    private fun decode(assetName: String, size: Size): DecodeResult =
        decode(assetName, Options(context = context, size = size, scale = Scale.FILL))

    private fun decode(assetName: String, options: Options): DecodeResult = runBlocking {
        val source = context.assets.open(assetName).source().buffer()
        val decoder = decoderFactory.create(
            result = SourceResult(
                source = ImageSource(source, context),
                mimeType = null,
                dataSource = DataSource.DISK
            ),
            options = options,
            imageLoader = ImageLoader(context)
        )
        val result = checkNotNull(decoder.decode())

        // Assert that the source has been closed.
        val exception = assertFailsWith<IllegalStateException> { source.exhausted() }
        assertEquals("closed", exception.message)

        return@runBlocking result
    }
}
