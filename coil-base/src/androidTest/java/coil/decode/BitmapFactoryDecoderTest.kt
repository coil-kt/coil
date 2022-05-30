package coil.decode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build.VERSION.SDK_INT
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.fetch.SourceResult
import coil.request.Options
import coil.size.Dimension
import coil.size.Scale
import coil.size.Size
import coil.util.assertIsSimilarTo
import coil.util.assumeTrue
import coil.util.decodeBitmapAsset
import coil.util.isSimilarTo
import coil.util.size
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okio.buffer
import okio.source
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BitmapFactoryDecoderTest {

    private lateinit var context: Context
    private lateinit var decoderFactory: BitmapFactoryDecoder.Factory

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        decoderFactory = BitmapFactoryDecoder.Factory()
    }

    @Test
    fun basic() = runTest {
        val result = decode(
            assetName = "normal.jpg",
            size = Size(100, 100)
        )

        assertTrue(result.isSampled)
        val drawable = assertIs<BitmapDrawable>(result.drawable)
        assertEquals(Size(100, 125), drawable.bitmap.size)
        assertEquals(Bitmap.Config.ARGB_8888, drawable.bitmap.config)
    }

    @Test
    fun undefinedWidth() = runTest {
        val result = decode(
            assetName = "normal.jpg",
            size = Size(Dimension.Undefined, 100),
            scale = Scale.FIT
        )

        assertTrue(result.isSampled)
        val drawable = assertIs<BitmapDrawable>(result.drawable)
        assertEquals(Size(80, 100), drawable.bitmap.size)
        assertEquals(Bitmap.Config.ARGB_8888, drawable.bitmap.config)
    }

    @Test
    fun undefinedHeight() = runTest {
        val result = decode(
            assetName = "normal.jpg",
            size = Size(100, Dimension.Undefined),
            scale = Scale.FIT
        )

        assertTrue(result.isSampled)
        val drawable = assertIs<BitmapDrawable>(result.drawable)
        assertEquals(Size(100, 125), drawable.bitmap.size)
        assertEquals(Bitmap.Config.ARGB_8888, drawable.bitmap.config)
    }

    @Test
    fun malformedImageThrows() = runTest {
        assertFailsWith<IllegalStateException> {
            decode(
                assetName = "malformed.jpg",
                size = Size(100, 100)
            )
        }
    }

    @Test
    fun resultIsSampledIfGreaterThanHalfSize() = runTest {
        val result = decode(
            assetName = "normal.jpg",
            size = Size(600, 600)
        )

        assertTrue(result.isSampled)
        val drawable = assertIs<BitmapDrawable>(result.drawable)
        assertEquals(Size(600, 750), drawable.bitmap.size)
    }

    @Test
    fun originalSizeDimensionsAreResolvedCorrectly() = runTest {
        val size = Size.ORIGINAL
        val normal = decodeBitmap("normal.jpg", size)
        assertEquals(Size(1080, 1350), normal.size)
    }

    @Test
    fun exifTransformationsAreAppliedCorrectlyByDefault() = runTest {
        val size = Size(500, 500)
        val normal = decodeBitmap("normal.jpg", size)

        for (index in 1..8) {
            val other = decodeBitmap("exif/$index.jpg", size)
            assertTrue(normal.isSimilarTo(other), "Image with index $index is incorrect.")
        }
    }

    @Test
    fun largeExifMetadata() = runTest {
        val size = Size(500, 500)
        val expected = decodeBitmap("exif/large_metadata_normalized.jpg", size)
        val actual = decodeBitmap("exif/large_metadata.jpg", size)
        expected.assertIsSimilarTo(actual)
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/619 */
    @Test
    fun heicExifMetadata() = runTest {
        // HEIC files are not supported before API 30.
        assumeTrue(SDK_INT >= 30)

        // Ensure this completes and doesn't end up in an infinite loop.
        val normal = context.decodeBitmapAsset("exif/basic.heic")
        val actual = decodeBitmap("exif/basic.heic", Size.ORIGINAL)
        normal.assertIsSimilarTo(actual)
    }

    @Test
    fun allowInexactSize_true() = runTest {
        val result = decodeBitmap(
            assetName = "normal.jpg",
            options = Options(
                context = context,
                size = Size(1500, 1500),
                scale = Scale.FIT,
                allowInexactSize = true
            )
        )
        assertEquals(Size(1080, 1350), result.size)
    }

    @Test
    fun allowInexactSize_false() = runTest {
        val result = decodeBitmap(
            assetName = "normal.jpg",
            options = Options(
                context = context,
                size = Size(1500, 1500),
                scale = Scale.FIT,
                allowInexactSize = false
            )
        )
        assertEquals(Size(1200, 1500), result.size)
    }

    @Test
    fun allowRgb565_true() = runTest {
        val result = decodeBitmap(
            assetName = "normal.jpg",
            options = Options(
                context = context,
                size = Size(500, 500),
                scale = Scale.FILL,
                allowRgb565 = true
            )
        )
        assertEquals(Size(500, 625), result.size)
        assertEquals(Bitmap.Config.RGB_565, result.config)
    }

    @Test
    fun allowRgb565_false() = runTest {
        val result = decodeBitmap(
            assetName = "normal.jpg",
            options = Options(
                context = context,
                size = Size(500, 500),
                scale = Scale.FILL,
                allowRgb565 = false
            )
        )
        assertEquals(Size(500, 625), result.size)
        assertEquals(Bitmap.Config.ARGB_8888, result.config)
    }

    @Test
    fun premultipliedAlpha_true() = runTest {
        val result = decodeBitmap(
            assetName = "normal_alpha.png",
            options = Options(
                context = context,
                size = Size(400, 200),
                scale = Scale.FILL,
                premultipliedAlpha = true
            )
        )
        assertEquals(Size(400, 200), result.size)
        assertTrue(result.isPremultiplied)
    }

    @Test
    fun premultipliedAlpha_false() = runTest {
        val result = decodeBitmap(
            assetName = "normal_alpha.png",
            options = Options(
                context = context,
                size = Size(400, 200),
                scale = Scale.FILL,
                premultipliedAlpha = false
            )
        )
        assertEquals(Size(400, 200), result.size)
        assertFalse(result.isPremultiplied)
    }

    // https://github.com/coil-kt/coil/issues/1074
    @Test
    fun exifOrientationPolicy_ignore() = runTest {
        val decoderFactory = BitmapFactoryDecoder.Factory(
            exifOrientationPolicy = ExifOrientationPolicy.IGNORE
        )

        for (index in 1..4) {
            val result = decodeBitmap(decoderFactory, "exif/$index.jpg", Size.ORIGINAL)
            assertEquals(Size(1080, 1350), result.size)
        }

        for (index in 5..8) {
            val result = decodeBitmap(decoderFactory, "exif/$index.jpg", Size.ORIGINAL)
            assertEquals(Size(1350, 1080), result.size)
        }
    }

    @Test
    fun exifOrientationPolicy_respectOptimal() = runTest {
        val decoderFactory = BitmapFactoryDecoder.Factory(
            exifOrientationPolicy = ExifOrientationPolicy.RESPECT_OPTIMAL
        )

        for (index in 1..8) {
            val result = decodeBitmap(decoderFactory, "exif/$index.jpg", Size.ORIGINAL)
            assertEquals(Size(1080, 1350), result.size)
        }
    }

    // https://github.com/coil-kt/coil/issues/1182
    @Test
    fun exifOrientationPolicy_respectAll() = runTest {
        val decoderFactory = BitmapFactoryDecoder.Factory(
            exifOrientationPolicy = ExifOrientationPolicy.RESPECT_ALL
        )

        for (index in 1..8) {
            val result = decodeBitmap(decoderFactory, "exif/$index.png", Size.ORIGINAL)
            assertEquals(Size(108, 135), result.size)
        }
    }

    @Test
    fun lossyWebP() = runTest {
        val expected = decodeBitmap("normal.jpg", Size(450, 675))
        decodeBitmap("lossy.webp", Size(450, 675)).assertIsSimilarTo(expected)
    }

    @Test
    fun png_16bit() = runTest {
        // The emulator runs out of memory on pre-23.
        assumeTrue(SDK_INT >= 23)

        val result = decode("16_bit.png", Size(250, 250))

        assertTrue(result.isSampled)
        val drawable = assertIs<BitmapDrawable>(result.drawable)
        assertEquals(Size(250, 250), drawable.bitmap.size)

        val expectedConfig = if (SDK_INT >= 26) Bitmap.Config.RGBA_F16 else Bitmap.Config.ARGB_8888
        assertEquals(expectedConfig, drawable.bitmap.config)
    }

    @Test
    fun largeJpeg() = runTest {
        decodeBitmap("large.jpg", Size(1080, 1920))
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/368 */
    @Test
    fun largePng() = runTest {
        // Ensure that this doesn't cause an OOM exception - particularly on API 23 and below.
        decodeBitmap("large.png", Size(1080, 1920))
    }

    @Test
    fun largeWebP() = runTest {
        decodeBitmap("large.webp", Size(1080, 1920))
    }

    @Test
    fun largeHeic() = runTest {
        // HEIC files are not supported before API 30.
        assumeTrue(SDK_INT >= 30)

        decodeBitmap("large.heic", Size(1080, 1920))
    }

    private suspend fun decodeBitmap(
        assetName: String,
        size: Size,
        scale: Scale = Scale.FILL
    ): Bitmap = decodeBitmap(decoderFactory, assetName, size, scale)

    private suspend fun decodeBitmap(
        assetName: String,
        options: Options
    ): Bitmap = decodeBitmap(decoderFactory, assetName, options)

    private suspend fun decode(
        assetName: String,
        size: Size,
        scale: Scale = Scale.FILL
    ): DecodeResult = decode(decoderFactory, assetName, size, scale)

    private suspend fun decode(
        assetName: String,
        options: Options
    ): DecodeResult = decode(decoderFactory, assetName, options)

    private suspend fun decodeBitmap(
        decoderFactory: BitmapFactoryDecoder.Factory,
        assetName: String,
        size: Size,
        scale: Scale = Scale.FILL
    ): Bitmap = decodeBitmap(
        decoderFactory,
        assetName,
        Options(context = context, size = size, scale = scale)
    )

    private suspend fun decodeBitmap(
        decoderFactory: BitmapFactoryDecoder.Factory,
        assetName: String,
        options: Options
    ): Bitmap = (decode(decoderFactory, assetName, options).drawable as BitmapDrawable).bitmap

    private suspend fun decode(
        decoderFactory: BitmapFactoryDecoder.Factory,
        assetName: String,
        size: Size,
        scale: Scale = Scale.FILL
    ): DecodeResult = decode(decoderFactory, assetName, Options(context = context, size = size, scale = scale))

    private suspend fun decode(
        decoderFactory: BitmapFactoryDecoder.Factory,
        assetName: String,
        options: Options
    ): DecodeResult {
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

        return result
    }
}
