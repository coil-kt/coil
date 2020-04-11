package coil.decode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build.VERSION.SDK_INT
import androidx.core.graphics.createBitmap
import androidx.test.core.app.ApplicationProvider
import coil.bitmappool.BitmapPool
import coil.size.OriginalSize
import coil.size.PixelSize
import coil.size.Scale
import coil.size.Size
import coil.util.createOptions
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
import kotlin.test.assertTrue

class BitmapFactoryDecoderTest {

    private lateinit var context: Context
    private lateinit var pool: BitmapPool
    private lateinit var service: BitmapFactoryDecoder

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        pool = BitmapPool(Long.MAX_VALUE)
        service = BitmapFactoryDecoder(context)
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
        val normal = decodeBitmap("exif/large_metadata_normalized.jpg", size)
        val actual = decodeBitmap("exif/large_metadata_normalized.jpg", size)
        assertTrue(normal.isSimilarTo(actual))
    }

    @Test
    fun allowInexactSize_True() {
        val result = decodeBitmap(
            assetName = "normal.jpg",
            size = PixelSize(1500, 1500),
            options = createOptions(scale = Scale.FIT, allowInexactSize = true)
        )
        assertEquals(PixelSize(1080, 1350), result.size)
    }

    @Test
    fun allowInexactSize_False() {
        val result = decodeBitmap(
            assetName = "normal.jpg",
            size = PixelSize(1500, 1500),
            options = createOptions(scale = Scale.FIT, allowInexactSize = false)
        )
        assertEquals(PixelSize(1200, 1500), result.size)
    }

    @Test
    fun allowRgb565_True() {
        val result = decodeBitmap(
            assetName = "normal.jpg",
            size = PixelSize(500, 500),
            options = createOptions(allowRgb565 = true)
        )
        assertEquals(PixelSize(500, 625), result.size)
        assertEquals(Bitmap.Config.RGB_565, result.config)
    }

    @Test
    fun allowRgb565_False() {
        val result = decodeBitmap(
            assetName = "normal.jpg",
            size = PixelSize(500, 500),
            options = createOptions(allowRgb565 = false)
        )
        assertEquals(PixelSize(500, 625), result.size)
        assertEquals(Bitmap.Config.ARGB_8888, result.config)
    }

    @Test
    fun pooledBitmap_exactSize() {
        val pooledBitmap = createBitmap(1080, 1350, Bitmap.Config.ARGB_8888)
        pool.put(pooledBitmap)

        val result = decodeBitmap(
            assetName = "normal.jpg",
            size = PixelSize(1080, 1350),
            options = createOptions(
                config = Bitmap.Config.ARGB_8888,
                scale = Scale.FIT,
                allowInexactSize = false
            )
        )
        assertEquals(PixelSize(1080, 1350), result.size)
        assertEquals(pooledBitmap, result)
    }

    @Test
    fun pooledBitmap_inexactSize() {
        val pooledBitmap = createBitmap(1080, 1350, Bitmap.Config.ARGB_8888)
        pool.put(pooledBitmap)

        val result = decodeBitmap(
            assetName = "normal.jpg",
            size = PixelSize(500, 500),
            options = createOptions(
                config = Bitmap.Config.ARGB_8888,
                scale = Scale.FILL,
                allowInexactSize = false
            )
        )
        assertEquals(PixelSize(500, 625), result.size)
        // The bitmap should not be re-used on pre-API 19.
        assertEquals(pooledBitmap === result, SDK_INT >= 19)
    }

    @Test
    fun png_16bit() {
        // The emulator runs out of memory while decoding 16_bit.png on pre-21.
        assumeTrue(SDK_INT >= 21)

        val (drawable, isSampled) = decode(
            assetName = "16_bit.png",
            size = PixelSize(250, 250),
            options = createOptions()
        )

        assertTrue(isSampled)
        assertTrue(drawable is BitmapDrawable)
        assertEquals(PixelSize(250, 250), drawable.bitmap.size)

        val expectedConfig = if (SDK_INT >= 26) Bitmap.Config.RGBA_F16 else Bitmap.Config.ARGB_8888
        assertEquals(expectedConfig, drawable.bitmap.config)
    }

    private fun decode(
        assetName: String,
        size: Size,
        options: Options = createOptions()
    ): DecodeResult = runBlocking {
        val source = context.assets.open(assetName).source().buffer()
        val result = service.decode(
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
        options: Options = createOptions()
    ): Bitmap = (decode(assetName, size, options).drawable as BitmapDrawable).bitmap
}
