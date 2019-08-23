package coil.decode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.test.core.app.ApplicationProvider
import coil.bitmappool.BitmapPool
import coil.bitmappool.FakeBitmapPool
import coil.size.OriginalSize
import coil.size.PixelSize
import coil.size.Size
import coil.util.createOptions
import coil.util.isSimilarTo
import kotlinx.coroutines.runBlocking
import okio.buffer
import okio.source
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BitmapFactoryDecoderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var pool: BitmapPool
    private lateinit var service: BitmapFactoryDecoder

    @Before
    fun before() {
        pool = FakeBitmapPool()
        service = BitmapFactoryDecoder(context)
    }

    @Test
    fun basicDecode() {
        val source = context.assets.open("normal.jpg").source().buffer()
        val (drawable, isSampled) = runBlocking {
            service.decode(
                pool = pool,
                source = source,
                size = PixelSize(100, 100),
                options = createOptions()
            )
        }

        val exception = assertFailsWith<IllegalStateException> { source.exhausted() }
        assertEquals("closed", exception.message)
        assertTrue(isSampled)
        assertTrue(drawable is BitmapDrawable)
        assertEquals(PixelSize(100, 125), drawable.bitmap.run { PixelSize(width, height) })
        assertEquals(drawable.bitmap.config, Bitmap.Config.ARGB_8888)
    }

    @Test
    fun malformedImageThrows() {
        assertFailsWith<IllegalStateException> {
            runBlocking {
                service.decode(
                    pool = pool,
                    source = context.assets.open("malformed.jpg").source().buffer(),
                    size = PixelSize(100, 100),
                    options = createOptions()
                )
            }
        }
    }

    @Test
    fun resultIsSampledIfGreaterThanHalfSize() {
        val (drawable, isSampled) = runBlocking {
            service.decode(
                pool = pool,
                source = context.assets.open("normal.jpg").source().buffer(),
                size = PixelSize(600, 600),
                options = createOptions()
            )
        }

        assertTrue(isSampled)
        assertTrue(drawable is BitmapDrawable)
    }

    @Test
    fun originalSizeDimensionsAreResolvedCorrectly() {
        val size = OriginalSize
        val normal = decode("normal.jpg", size)
        assertTrue(normal.width == 1080 && normal.height == 1350)
    }

    @Test
    fun exifTransformationsAreAppliedCorrectly() {
        val size = PixelSize(500, 500)
        val normal = decode("normal.jpg", size)

        for (index in 1..8) {
            val other = decode("exif/$index.jpg", size)
            assertTrue(normal.isSimilarTo(other), "Image with index $index is incorrect.")
        }
    }

    @Test
    fun largeExifMetadata() {
        val size = PixelSize(500, 500)
        val normal = decode("exif/large_metadata_normalized.jpg", size)
        val largeExifMetadata = decode("exif/large_metadata_normalized.jpg", size)
        assertTrue(normal.isSimilarTo(largeExifMetadata))
    }

    private fun decode(fileName: String, size: Size): Bitmap = runBlocking {
        val result = service.decode(
            pool = pool,
            source = context.assets.open(fileName).source().buffer(),
            size = size,
            options = createOptions()
        )
        return@runBlocking (result.drawable as BitmapDrawable).bitmap
    }
}
