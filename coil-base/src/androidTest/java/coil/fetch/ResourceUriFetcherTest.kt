package coil.fetch

import android.content.ContentResolver.SCHEME_ANDROID_RESOURCE
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.os.Build.VERSION.SDK_INT
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import coil.base.test.R
import coil.bitmap.BitmapPool
import coil.decode.DrawableDecoderService
import coil.map.ResourceUriMapper
import coil.size.PixelSize
import coil.util.createOptions
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResourceUriFetcherTest {

    private lateinit var context: Context
    private lateinit var pool: BitmapPool
    private lateinit var drawableDecoder: DrawableDecoderService
    private lateinit var fetcher: ResourceUriFetcher

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        pool = BitmapPool(0)
        drawableDecoder = DrawableDecoderService(pool)
        fetcher = ResourceUriFetcher(context, drawableDecoder)
    }

    @Test
    fun rasterDrawable() {
        val uri = "$SCHEME_ANDROID_RESOURCE://${context.packageName}/${R.drawable.normal}".toUri()

        assertTrue(fetcher.handles(uri))

        val result = runBlocking {
            fetcher.fetch(pool, uri, PixelSize(100, 100), createOptions())
        }

        assertTrue(result is SourceResult)
        assertEquals("image/jpeg", result.mimeType)
        assertFalse(result.source.exhausted())
    }

    @Test
    fun vectorDrawable() {
        val uri = "$SCHEME_ANDROID_RESOURCE://${context.packageName}/${R.drawable.ic_android}".toUri()

        assertTrue(fetcher.handles(uri))

        val result = runBlocking {
            fetcher.fetch(pool, uri, PixelSize(100, 100), createOptions())
        }

        assertTrue(result is DrawableResult)
        assertTrue(result.drawable is BitmapDrawable)
        assertTrue(result.isSampled)
    }

    @Test
    fun externalPackageRasterDrawable() {
        // https://android.googlesource.com/platform/packages/apps/Settings/+/master/res/drawable-xhdpi
        val resource = if (SDK_INT >= 23) "msg_bubble_incoming" else "ic_power_system"
        val rawUri = "$SCHEME_ANDROID_RESOURCE://com.android.settings/drawable/$resource".toUri()
        val uri = ResourceUriMapper(context).map(rawUri)

        assertTrue(fetcher.handles(uri))

        val result = runBlocking {
            fetcher.fetch(pool, uri, PixelSize(100, 100), createOptions())
        }

        assertTrue(result is SourceResult)
        assertEquals("image/png", result.mimeType)
        assertFalse(result.source.exhausted())
    }

    @Test
    fun externalPackageVectorDrawable() {
        // com.android.settings/drawable/ic_cancel was added in API 23.
        assumeTrue(SDK_INT >= 23)

        // https://android.googlesource.com/platform/packages/apps/Settings/+/master/res/drawable/ic_cancel.xml
        val rawUri = "$SCHEME_ANDROID_RESOURCE://com.android.settings/drawable/ic_cancel".toUri()
        val uri = ResourceUriMapper(context).map(rawUri)

        assertTrue(fetcher.handles(uri))

        val result = runBlocking {
            fetcher.fetch(pool, uri, PixelSize(100, 100), createOptions())
        }

        assertTrue(result is DrawableResult)
        assertTrue(result.drawable is BitmapDrawable)
        assertTrue(result.isSampled)
    }
}
