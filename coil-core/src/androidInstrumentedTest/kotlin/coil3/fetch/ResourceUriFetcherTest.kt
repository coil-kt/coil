package coil3.fetch

import android.content.ContentResolver.SCHEME_ANDROID_RESOURCE
import android.os.Build.VERSION.SDK_INT
import androidx.core.graphics.drawable.toBitmap
import coil3.BitmapImage
import coil3.ImageLoader
import coil3.core.test.R
import coil3.map.ResourceIntMapper
import coil3.map.ResourceUriMapper
import coil3.request.Options
import coil3.size.Size
import coil3.test.utils.ViewTestActivity
import coil3.test.utils.assertIsSimilarTo
import coil3.test.utils.assumeTrue
import coil3.test.utils.context
import coil3.test.utils.launchActivity
import coil3.toUri
import coil3.util.getDrawableCompat
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ResourceUriFetcherTest {

    private val fetcherFactory = ResourceUriFetcher.Factory()

    @Test
    fun rasterDrawable() = runTest {
        val uri = "$SCHEME_ANDROID_RESOURCE://${context.packageName}/${R.drawable.normal}".toUri()
        val options = Options(context, size = Size(100, 100))
        val result = fetcherFactory.create(uri, options, ImageLoader(context))?.fetch()

        assertIs<SourceFetchResult>(result)
        assertEquals("image/jpeg", result.mimeType)
        assertFalse(result.source.source().exhausted())
    }

    @Test
    fun vectorDrawable() = runTest {
        val uri = "$SCHEME_ANDROID_RESOURCE://${context.packageName}/${R.drawable.ic_android}".toUri()
        val options = Options(context, size = Size(100, 100))
        val result = fetcherFactory.create(uri, options, ImageLoader(context))?.fetch()

        assertIs<ImageFetchResult>(result)
        assertIs<BitmapImage>(result.image)
        assertTrue(result.isSampled)
    }

    @Test
    fun externalPackageRasterDrawable() = runTest {
        // https://android.googlesource.com/platform/packages/apps/Settings/+/master/res/drawable-xhdpi
        val resource = if (SDK_INT >= 23) "msg_bubble_incoming" else "ic_power_system"
        val rawUri = "$SCHEME_ANDROID_RESOURCE://com.android.settings/drawable/$resource".toUri()
        val options = Options(context, size = Size(100, 100))
        val uri = assertNotNull(ResourceUriMapper().map(rawUri, options))
        val result = fetcherFactory.create(uri, options, ImageLoader(context))?.fetch()

        assertIs<SourceFetchResult>(result)
        assertEquals("image/png", result.mimeType)
        assertFalse(result.source.source().exhausted())
    }

    @Test
    fun externalPackageVectorDrawable() = runTest {
        // com.android.settings/drawable/ic_cancel was added in API 23.
        assumeTrue(SDK_INT >= 23)

        // https://android.googlesource.com/platform/packages/apps/Settings/+/master/res/drawable/ic_cancel.xml
        val rawUri = "$SCHEME_ANDROID_RESOURCE://com.android.settings/drawable/ic_cancel".toUri()
        val options = Options(context, size = Size(100, 100))
        val uri = assertNotNull(ResourceUriMapper().map(rawUri, options))
        val result = fetcherFactory.create(uri, options, ImageLoader(context))?.fetch()

        assertIs<ImageFetchResult>(result)
        assertIs<BitmapImage>(result.image)
        assertTrue(result.isSampled)
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/469 */
    @Test
    fun colorAttributeIsApplied() = launchActivity { activity: ViewTestActivity ->
        // Intentionally use the application context.
        val imageLoader = ImageLoader(context.applicationContext)
        val options = Options(context = activity, size = Size.ORIGINAL)
        val result = runBlocking {
            val uri = assertNotNull(ResourceIntMapper().map(R.drawable.ic_tinted_vector, options))
            fetcherFactory.create(uri, options, imageLoader)?.fetch()
        }
        val expected = activity.getDrawableCompat(R.drawable.ic_tinted_vector).toBitmap()
        val actual = ((result as ImageFetchResult).image as BitmapImage).bitmap
        actual.assertIsSimilarTo(expected)
    }
}
