package coil.fetch

import android.content.ContentResolver.SCHEME_ANDROID_RESOURCE
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.os.Build.VERSION.SDK_INT
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.base.test.R
import coil.map.ResourceIntMapper
import coil.map.ResourceUriMapper
import coil.request.Options
import coil.size.OriginalSize
import coil.size.PixelSize
import coil.util.getDrawableCompat
import coil.util.isSimilarTo
import coil.util.withTestActivity
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ResourceUriFetcherTest {

    private lateinit var context: Context
    private lateinit var fetcherFactory: ResourceUriFetcher.Factory

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        fetcherFactory = ResourceUriFetcher.Factory()
    }

    @Test
    fun rasterDrawable() {
        val uri = "$SCHEME_ANDROID_RESOURCE://${context.packageName}/${R.drawable.normal}".toUri()
        val options = Options(context, size = PixelSize(100, 100))

        val result = runBlocking {
            fetcherFactory.create(uri, options, ImageLoader(context))?.fetch()
        }

        assertTrue(result is SourceResult)
        assertEquals("image/jpeg", result.mimeType)
        assertFalse(result.source.source().exhausted())
    }

    @Test
    fun vectorDrawable() {
        val uri = "$SCHEME_ANDROID_RESOURCE://${context.packageName}/${R.drawable.ic_android}".toUri()
        val options = Options(context, size = PixelSize(100, 100))

        val result = runBlocking {
            fetcherFactory.create(uri, options, ImageLoader(context))?.fetch()
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
        val options = Options(context, size = PixelSize(100, 100))
        val uri = assertNotNull(ResourceUriMapper().map(rawUri, options))

        val result = runBlocking {
            fetcherFactory.create(uri, options, ImageLoader(context))?.fetch()
        }

        assertTrue(result is SourceResult)
        assertEquals("image/png", result.mimeType)
        assertFalse(result.source.source().exhausted())
    }

    @Test
    fun externalPackageVectorDrawable() {
        // com.android.settings/drawable/ic_cancel was added in API 23.
        assumeTrue(SDK_INT >= 23)

        // https://android.googlesource.com/platform/packages/apps/Settings/+/master/res/drawable/ic_cancel.xml
        val rawUri = "$SCHEME_ANDROID_RESOURCE://com.android.settings/drawable/ic_cancel".toUri()
        val options = Options(context, size = PixelSize(100, 100))
        val uri = assertNotNull(ResourceUriMapper().map(rawUri, options))

        val result = runBlocking {
            fetcherFactory.create(uri, options, ImageLoader(context))?.fetch()
        }

        assertTrue(result is DrawableResult)
        assertTrue(result.drawable is BitmapDrawable)
        assertTrue(result.isSampled)
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/469 */
    @Test
    fun colorAttributeIsApplied() = withTestActivity { activity ->
        val imageLoader = ImageLoader(context) // Intentionally use the application context.
        val options = Options(context = activity, size = OriginalSize)
        val result = runBlocking {
            val uri = assertNotNull(ResourceIntMapper().map(R.drawable.ic_tinted_vector, options))
            fetcherFactory.create(uri, options, imageLoader)?.fetch()
        }
        val expected = activity.getDrawableCompat(R.drawable.ic_tinted_vector).toBitmap()
        val actual = (result as DrawableResult).drawable.toBitmap()
        assertTrue(actual.isSimilarTo(expected))
    }
}
