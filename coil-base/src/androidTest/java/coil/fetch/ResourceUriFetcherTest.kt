package coil.fetch

import android.content.ContentResolver
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import coil.base.test.R
import coil.bitmappool.BitmapPool
import coil.decode.DrawableDecoderService
import coil.map.ResourceUriMapper
import coil.size.PixelSize
import coil.util.createOptions
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResourceUriFetcherTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var pool: BitmapPool
    private lateinit var drawableDecoder: DrawableDecoderService
    private lateinit var fetcher: ResourceUriFetcher

    @Before
    fun before() {
        pool = BitmapPool(0)
        drawableDecoder = DrawableDecoderService(context, pool)
        fetcher = ResourceUriFetcher(context, drawableDecoder)
    }

    @Test
    fun rasterDrawable() {
        val uri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.drawable.normal}")

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
        val uri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.drawable.ic_android}")

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
        // https://android.googlesource.com/platform/packages/apps/Settings/+/master/res/drawable/regulatory_info.png
        val packageName = "com.android.settings"
        val rawUri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://$packageName/drawable/regulatory_info")
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
        // https://android.googlesource.com/platform/packages/apps/Settings/+/master/res/drawable/ic_cancel.xml
        val packageName = "com.android.settings"
        val rawUri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://$packageName/drawable/ic_cancel")
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
