package coil.decode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build.VERSION.SDK_INT
import androidx.core.graphics.createBitmap
import androidx.test.core.app.ApplicationProvider
import coil.bitmap.BitmapPool
import coil.size.OriginalSize
import coil.size.PixelSize
import coil.size.Scale
import coil.size.Size
import coil.util.decodeBitmapAsset
import coil.util.isSimilarTo
import coil.util.size
import kotlinx.coroutines.runBlocking
import okio.buffer
import okio.source
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BitmapFactoryDecoderTest {

    private lateinit var context: Context
    private lateinit var pool: BitmapPool
    private lateinit var decoder: BitmapFactoryDecoder

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        pool = BitmapPool(Int.MAX_VALUE)
        decoder = BitmapFactoryDecoder(context)
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
            size = PixelSize(1500, 1500),
            options = Options(
                context = context,
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
            size = PixelSize(1500, 1500),
            options = Options(
                context = context,
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
            size = PixelSize(500, 500),
            options = Options(
                context = context,
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
            size = PixelSize(500, 500),
            options = Options(
                context = context,
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
            size = PixelSize(400, 200),
            options = Options(
                context = context,
                scale = Scale.FILL,
                premultipliedAlpha = true
            )
        )
        assertEquals(PixelSize(400, 200), result.size)
        if (SDK_INT >= 19) {
            assertTrue(result.isPremultiplied)
        }
    }

    @Test
    fun premultipliedAlpha_false() {
        val result = decodeBitmap(
            assetName = "normal_alpha.png",
            size = PixelSize(400, 200),
            options = Options(
                context = context,
                scale = Scale.FILL,
                premultipliedAlpha = false
            )
        )
        assertEquals(PixelSize(400, 200), result.size)
        if (SDK_INT >= 19) {
            assertFalse(result.isPremultiplied)
        }
    }

    @Test
    fun pooledBitmap_exactSize() {
        val pooledBitmap = createBitmap(1080, 1350, Bitmap.Config.ARGB_8888)
        pool.put(pooledBitmap)

        val result = decodeBitmap(
            assetName = "normal.jpg",
            size = PixelSize(1080, 1350),
            options = Options(
                context = context,
                config = Bitmap.Config.ARGB_8888,
                scale = Scale.FIT,
                allowInexactSize = false
            )
        )
        assertEquals(PixelSize(1080, 1350), result.size)

        // BitmapFactoryDecoder creates immutable bitmaps instead of using pooled bitmaps on API 24+.
        if (SDK_INT >= 24) {
            assertNotSame(pooledBitmap, result)
            assertFalse(result.isMutable)
        } else {
            assertSame(pooledBitmap, result)
            assertTrue(result.isMutable)
        }
    }

    @Test
    fun pooledBitmap_inexactSize() {
        val pooledBitmap = createBitmap(900, 850, Bitmap.Config.ARGB_8888)
        pool.put(pooledBitmap)

        val result = decodeBitmap(
            assetName = "normal.jpg",
            size = PixelSize(500, 500),
            options = Options(
                context = context,
                config = Bitmap.Config.ARGB_8888,
                scale = Scale.FILL,
                allowInexactSize = false
            )
        )
        assertEquals(PixelSize(500, 625), result.size)

        // BitmapFactoryDecoder creates immutable bitmaps instead of using pooled bitmaps on API 24+.
        when {
            SDK_INT >= 24 -> {
                assertNotSame(pooledBitmap, result)
                assertFalse(result.isMutable)
            }
            SDK_INT >= 19 -> {
                assertSame(pooledBitmap, result)
                assertTrue(result.isMutable)
            }
            else -> {
                assertNotSame(pooledBitmap, result)
                assertTrue(result.isMutable)
            }
        }
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
        // The emulator runs out of memory on pre-19.
        assumeTrue(SDK_INT >= 19)

        decodeBitmap("large.jpg", PixelSize(1080, 1920))
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/368 */
    @Test
    fun largePng() {
        // The emulator runs out of memory on pre-19.
        assumeTrue(SDK_INT >= 19)

        // Ensure that this doesn't cause an OOM exception - particularly on API 23 and below.
        decodeBitmap("large.png", PixelSize(1080, 1920))
    }

    @Test
    fun largeWebP() {
        // The emulator runs out of memory on pre-19.
        assumeTrue(SDK_INT >= 19)

        decodeBitmap("large.webp", PixelSize(1080, 1920))
    }

    @Test
    fun largeHeic() {
        // HEIC files are not supported before API 30.
        assumeTrue(SDK_INT >= 30)

        decodeBitmap("large.heic", PixelSize(1080, 1920))
    }

    private fun decode(
        assetName: String,
        size: Size,
        options: Options = Options(context, scale = Scale.FILL)
    ): DecodeResult = runBlocking {
        val source = context.assets.open(assetName).source().buffer()
        val result = decoder.decode(
            pool = pool,
            source = source,
            size = size,
            options = options
        )

        // Assert that the source has been closed.
        val exception = assertFailsWith<IllegalStateException> { source.exhausted() }
        assertEquals("closed", exception.message)

        return@runBlocking result
    }

    private fun decodeBitmap(
        assetName: String,
        size: Size,
        options: Options = Options(context, scale = Scale.FILL)
    ): Bitmap = (decode(assetName, size, options).drawable as BitmapDrawable).bitmap
}
