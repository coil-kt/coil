package coil3.decode

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Gainmap
import android.os.Build.VERSION.SDK_INT
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.BitmapImage
import coil3.Extras
import coil3.ImageLoader
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.request.bitmapConfig
import coil3.size.Size
import coil3.test.utils.context
import coil3.util.MIME_TYPE_JPEG
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import okio.source
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GainmapHardwareDecodeTest {

    @Test
    fun bitmapFactoryDecoder_hardware_preservesGainmap() = runTest {
        val factory = BitmapFactoryDecoder.Factory()
        val source = context.assets.open("fountain_night.jpg").source().buffer()
        val decoder = factory.create(
            result = SourceFetchResult(
                source = ImageSource(
                    source = source,
                    fileSystem = FakeFileSystem(),
                    metadata = AssetMetadata("fountain_night.jpg"),
                ),
                mimeType = "image/jpeg",
                dataSource = DataSource.DISK,
            ),
            options = Options(
                context = context,
                size = Size.ORIGINAL,
                extras = Extras.Builder()
                    .set(Extras.Key.bitmapConfig, Bitmap.Config.HARDWARE)
                    .build(),
            ),
            imageLoader = ImageLoader(context),
        )
        assertNotNull(decoder)
        val result = decoder.decode()
        assertNotNull(result)
        val bitmap = (result.image as BitmapImage).bitmap
        assertEquals(Bitmap.Config.HARDWARE, bitmap.config)
        if (SDK_INT >= 34) {
            assertTrue(bitmap.hasGainmap())
            val gainmap = bitmap.gainmap
            assertNotNull(gainmap)
            assertEquals(Bitmap.Config.HARDWARE, gainmap.gainmapContents.config)
        }
    }

    @Test
    fun staticImageDecoder_hardware_preservesGainmap() = runTest {
        if (SDK_INT < 29) return@runTest
        val factory = StaticImageDecoder.Factory()
        val source = context.assets.open("fountain_night.jpg").source().buffer()
        val decoder = factory.create(
            result = SourceFetchResult(
                source = ImageSource(
                    source = source,
                    fileSystem = FakeFileSystem(),
                    metadata = AssetMetadata("fountain_night.jpg"),
                ),
                mimeType = "image/jpeg",
                dataSource = DataSource.DISK,
            ),
            options = Options(
                context = context,
                size = Size.ORIGINAL,
                extras = Extras.Builder()
                    .set(Extras.Key.bitmapConfig, Bitmap.Config.HARDWARE)
                    .build(),
            ),
            imageLoader = ImageLoader(context),
        )
        assertNotNull(decoder)
        val result = decoder.decode()
        assertNotNull(result)
        val bitmap = (result.image as BitmapImage).bitmap
        assertEquals(Bitmap.Config.HARDWARE, bitmap.config)
        if (SDK_INT >= 34) {
            assertTrue(bitmap.hasGainmap())
            val gainmap = bitmap.gainmap
            assertNotNull(gainmap)
            assertEquals(Bitmap.Config.HARDWARE, gainmap.gainmapContents.config)
        }
    }

    @Test
    fun bitmapFactoryDecoder_software_preservesGainmap() = runTest {
        val factory = BitmapFactoryDecoder.Factory()
        val source = context.assets.open("fountain_night.jpg").source().buffer()
        val decoder = factory.create(
            result = SourceFetchResult(
                source = ImageSource(
                    source = source,
                    fileSystem = FakeFileSystem(),
                    metadata = AssetMetadata("fountain_night.jpg"),
                ),
                mimeType = "image/jpeg",
                dataSource = DataSource.DISK,
            ),
            options = Options(
                context = context,
                size = Size.ORIGINAL,
                extras = Extras.Builder()
                    .set(Extras.Key.bitmapConfig, Bitmap.Config.ARGB_8888)
                    .build(),
            ),
            imageLoader = ImageLoader(context),
        )
        assertNotNull(decoder)
        val result = decoder.decode()
        assertNotNull(result)
        val bitmap = (result.image as BitmapImage).bitmap
        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
        if (SDK_INT >= 34) {
            assertTrue(bitmap.hasGainmap())
            assertNotNull(bitmap.gainmap)
        }
    }

    @Test
    fun staticImageDecoder_software_preservesGainmap() = runTest {
        if (SDK_INT < 29) return@runTest
        val factory = StaticImageDecoder.Factory()
        val source = context.assets.open("fountain_night.jpg").source().buffer()
        val decoder = factory.create(
            result = SourceFetchResult(
                source = ImageSource(
                    source = source,
                    fileSystem = FakeFileSystem(),
                    metadata = AssetMetadata("fountain_night.jpg"),
                ),
                mimeType = "image/jpeg",
                dataSource = DataSource.DISK,
            ),
            options = Options(
                context = context,
                size = Size.ORIGINAL,
                extras = Extras.Builder()
                    .set(Extras.Key.bitmapConfig, Bitmap.Config.ARGB_8888)
                    .build(),
            ),
            imageLoader = ImageLoader(context),
        )
        assertNotNull(decoder)
        val result = decoder.decode()
        assertNotNull(result)
        val bitmap = (result.image as BitmapImage).bitmap
        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
        if (SDK_INT >= 34) {
            assertTrue(bitmap.hasGainmap())
            assertNotNull(bitmap.gainmap)
        }
    }

    @Test
    fun nonGainmapImage_hardwareDecodeUnaffected() = runTest {
        val factory = BitmapFactoryDecoder.Factory()
        val source = context.assets.open("normal.jpg").source().buffer()
        val decoder = factory.create(
            result = SourceFetchResult(
                source = ImageSource(
                    source = source,
                    fileSystem = FakeFileSystem(),
                    metadata = AssetMetadata("normal.jpg"),
                ),
                mimeType = "image/jpeg",
                dataSource = DataSource.DISK,
            ),
            options = Options(
                context = context,
                size = Size.ORIGINAL,
                extras = Extras.Builder()
                    .set(Extras.Key.bitmapConfig, Bitmap.Config.HARDWARE)
                    .build(),
            ),
            imageLoader = ImageLoader(context),
        )
        assertNotNull(decoder)
        val result = decoder.decode()
        assertNotNull(result)
        val bitmap = (result.image as BitmapImage).bitmap
        assertEquals(Bitmap.Config.HARDWARE, bitmap.config)
    }

    @Test
    fun nonJpegImage_png_hardwareDecodeUnaffected() = runTest {
        val factory = BitmapFactoryDecoder.Factory()
        val source = context.assets.open("normal.png").source().buffer()
        val decoder = factory.create(
            result = SourceFetchResult(
                source = ImageSource(
                    source = source,
                    fileSystem = FakeFileSystem(),
                    metadata = AssetMetadata("normal.png"),
                ),
                mimeType = "image/png",
                dataSource = DataSource.DISK,
            ),
            options = Options(
                context = context,
                size = Size.ORIGINAL,
                extras = Extras.Builder()
                    .set(Extras.Key.bitmapConfig, Bitmap.Config.HARDWARE)
                    .build(),
            ),
            imageLoader = ImageLoader(context),
        )
        assertNotNull(decoder)
        val result = decoder.decode()
        assertNotNull(result)
        val bitmap = (result.image as BitmapImage).bitmap
        assertEquals(Bitmap.Config.HARDWARE, bitmap.config)
    }

    @Test
    fun shouldWorkAroundHardwareGainmap_narrowedCorrectly() {
        assertFalse(GainmapUtils.shouldWorkAroundHardwareGainmap("image/png"))
        assertFalse(GainmapUtils.shouldWorkAroundHardwareGainmap("image/webp"))
        assertFalse(GainmapUtils.shouldWorkAroundHardwareGainmap("image/gif"))
        assertFalse(GainmapUtils.shouldWorkAroundHardwareGainmap(null))

        if (SDK_INT == 34) {
            val a8 = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
            val hw = a8.copy(Bitmap.Config.HARDWARE, false)
            val isBroken = hw == null
            a8.recycle()
            hw?.recycle()
            assertEquals(isBroken, GainmapUtils.shouldWorkAroundHardwareGainmap(MIME_TYPE_JPEG))
        } else {
            assertFalse(GainmapUtils.shouldWorkAroundHardwareGainmap(MIME_TYPE_JPEG))
        }
    }

    @Test
    fun gainmapConversion_preservesPixelLuminanceAndMetadata() {
        if (SDK_INT < 34) return

        val alphaBitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ALPHA_8)
        alphaBitmap.setPixel(0, 0, Color.argb(0, 0, 0, 0))
        alphaBitmap.setPixel(1, 0, Color.argb(64, 0, 0, 0))
        alphaBitmap.setPixel(0, 1, Color.argb(128, 0, 0, 0))
        alphaBitmap.setPixel(1, 1, Color.argb(255, 0, 0, 0))

        val converted = GainmapUtils.convertAlpha8ToArgb8888(alphaBitmap)
        assertEquals(Bitmap.Config.ARGB_8888, converted.config)

        fun checkPixel(x: Int, y: Int, expectedVal: Int) {
            val pixel = converted.getPixel(x, y)
            assertEquals(255, Color.alpha(pixel))
            assertEquals(expectedVal, Color.red(pixel))
            assertEquals(expectedVal, Color.green(pixel))
            assertEquals(expectedVal, Color.blue(pixel))
        }

        checkPixel(0, 0, 0)
        checkPixel(1, 0, 64)
        checkPixel(0, 1, 128)
        checkPixel(1, 1, 255)

        val origGainmap = Gainmap(alphaBitmap).apply {
            setRatioMin(1.0f, 1.1f, 1.2f)
            setRatioMax(3.0f, 3.1f, 3.2f)
            setGamma(1.5f, 1.6f, 1.7f)
            setEpsilonSdr(0.01f, 0.02f, 0.03f)
            setEpsilonHdr(0.04f, 0.05f, 0.06f)
            displayRatioForFullHdr = 4.0f
            minDisplayRatioForHdrTransition = 1.0f
        }

        val copiedGainmap = GainmapUtils.copyGainmap(origGainmap, converted)

        val ratioMin = copiedGainmap.ratioMin
        assertEquals(1.0f, ratioMin[0])
        assertEquals(1.1f, ratioMin[1])
        assertEquals(1.2f, ratioMin[2])

        val ratioMax = copiedGainmap.ratioMax
        assertEquals(3.0f, ratioMax[0])
        assertEquals(3.1f, ratioMax[1])
        assertEquals(3.2f, ratioMax[2])

        val gamma = copiedGainmap.gamma
        assertEquals(1.5f, gamma[0])
        assertEquals(1.6f, gamma[1])
        assertEquals(1.7f, gamma[2])

        val epsilonSdr = copiedGainmap.epsilonSdr
        assertEquals(0.01f, epsilonSdr[0])
        assertEquals(0.02f, epsilonSdr[1])
        assertEquals(0.03f, epsilonSdr[2])

        val epsilonHdr = copiedGainmap.epsilonHdr
        assertEquals(0.04f, epsilonHdr[0])
        assertEquals(0.05f, epsilonHdr[1])
        assertEquals(0.06f, epsilonHdr[2])

        assertEquals(4.0f, copiedGainmap.displayRatioForFullHdr)
        assertEquals(1.0f, copiedGainmap.minDisplayRatioForHdrTransition)

        alphaBitmap.recycle()
        converted.recycle()
    }
}
