@file:Suppress("EXPERIMENTAL_API_USAGE")

package coil.fetch

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import coil.bitmappool.BitmapPool
import coil.bitmappool.FakeBitmapPool
import coil.base.test.R
import coil.size.PixelSize
import coil.util.createOptions
import kotlinx.coroutines.runBlocking
import okio.buffer
import okio.sink
import okio.source
import org.hamcrest.CoreMatchers.startsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UriFetcherTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var loader: UriFetcher
    private lateinit var pool: BitmapPool

    @Rule
    @JvmField
    var thrown: ExpectedException = ExpectedException.none()

    @Before
    fun before() {
        loader = UriFetcher(context)
        pool = FakeBitmapPool()
    }

    @Test
    fun basicFileFetch() {
        // Copy the asset to filesDir.
        val source = context.assets.open("normal.jpg").source().buffer()
        val file = File(context.filesDir.absolutePath + File.separator + "normal.jpg")
        val sink = file.sink().buffer()
        sink.writeAll(source)

        val uri = file.toUri()
        assertTrue(loader.handles(uri))

        val result = runBlocking {
            loader.fetch(pool, uri, PixelSize(100, 100), createOptions())
        }

        assertTrue(result is SourceResult)
        assertFalse(result.source.exhausted())
    }

    @Test
    fun basicAssetFetch() {
        val uri = Uri.parse("file:///android_asset/normal.jpg")
        assertTrue(loader.handles(uri))

        val result = runBlocking {
            loader.fetch(pool, uri, PixelSize(100, 100), createOptions())
        }

        assertTrue(result is SourceResult)
        assertFalse(result.source.exhausted())
    }

    @Test
    fun basicAndroidResourceVectorFetchSamePackage() {
        val resourceId = R.drawable.ic_android
        val uri = Uri.parse("android.resource://${context.packageName}/$resourceId")
        assertTrue(loader.handles(uri))

        val result = runBlocking {
            loader.fetch(pool, uri, PixelSize(100, 100), createOptions())
        }

        assertTrue(result is SourceResult)
        assertFalse(result.source.exhausted())
    }

    @Test
    fun basicAndroidResourceVectorFetchDifferentPackage() {
        val uri = Uri.parse("android.resource://com.android.messaging/drawable/abc_ic_ab_back_material")
        assertTrue(loader.handles(uri))

        val result = runBlocking {
            loader.fetch(pool, uri, PixelSize(100, 100), createOptions())
        }

        assertTrue(result is SourceResult)
        assertFalse(result.source.exhausted())
    }

    @Test
    fun basicExtractPath() {
        val uri = Uri.parse("file:///android_asset/something.jpg")
        val result = loader.extractAssetPath(uri)
        assertEquals("something.jpg", result)
    }

    @Test
    fun nestedDirectoriesExtractPath() {
        val uri = Uri.parse("file:///android_asset/img/foo/bar/test.jpg")
        val result = loader.extractAssetPath(uri)
        assertEquals("img/foo/bar/test.jpg", result)
    }

    @Test
    fun emptyExtractPath() {
        val uri = Uri.parse("file:///android_asset/")
        val result = loader.extractAssetPath(uri)
        assertEquals(null, result)
    }

    @Test
    fun nonAssetUriExtractPath() {
        val uri = Uri.parse("file:///fake/file/path")
        val result = loader.extractAssetPath(uri)
        assertEquals(null, result)
    }

    @Test
    fun fileCacheKeyWithLastModified() {
        val file = File(context.filesDir.absolutePath + File.separator + "file.jpg")
        val uri = file.toUri()

        // Copy the asset to filesDir.
        val source = context.assets.open("normal.jpg").source().buffer()
        val sink = file.sink().buffer()
        sink.writeAll(source)

        file.setLastModified(1234L)
        val firstKey = loader.key(uri)

        file.setLastModified(4321L)
        val secondKey = loader.key(uri)

        assertNotEquals(secondKey, firstKey)
    }

    @Test
    fun assetCacheKeyWithLastModified() {
        val uri = Uri.parse("file:///android_asset/normal.jpg")
        assertEquals("$uri:0", loader.key(uri))
    }

    @Test
    fun nonfileCacheKeyEqualsUri() {
        val uri = Uri.parse("content://fake/content/path")
        assertEquals(uri.toString(), loader.key(uri))
    }

    @Test
    fun findResourceIdFromUri() {
        val resourceId = 12345
        val uri = Uri.parse("android.resource://com.android.messaging/$resourceId")
        val result = loader.findResourceIdFromUri(uri)
        assertEquals(resourceId, result)
    }

    @Test
    fun notResourceIdFromUri() {
        val uri = Uri.parse("android.resource://com.android.messaging/")
        thrown.expect(IllegalArgumentException::class.java)
        thrown.expectMessage(startsWith("Failed to find resource id"))
        loader.findResourceIdFromUri(uri)
    }

    @Test
    fun findContextForSamePackage() {
        val resourceId = 12345
        val uri = Uri.parse("android.resource://${context.packageName}/$resourceId")
        val expectedContext = context
        val targetPackage = uri.authority
        targetPackage?.let {
            val result = loader.findContextForPackage(context, uri, it)
            assertEquals(expectedContext, result)
        }
    }

    @Test
    fun findContextForDifferentPackage() {
        val resourceId = 12345
        val uri = Uri.parse("android.resource://com.android.messaging/$resourceId")
        val expectedContext = context
        val targetPackage = uri.authority
        targetPackage?.let {
            val result = loader.findContextForPackage(context, uri, it)
            assertNotNull(result)
            assertNotEquals(expectedContext, result)
        }
    }

    @Test
    fun unknownContextForDifferentPackage() {
        val resourceId = 12345
        val uri = Uri.parse("android.resource://com.test.beta/$resourceId")
        val targetPackage = uri.authority
        targetPackage?.let {
            thrown.expect(PackageManager.NameNotFoundException::class.java)
            thrown.expectMessage(startsWith("Failed to find target package on device for"))
            loader.findContextForPackage(context, uri, it)
        }
    }

    @Test
    fun findResourceIdFromTypeAndNameResourceUriSamePackage() {
        val uri = Uri.parse("android.resource://${context.packageName}/drawable/ic_android")
        val targetPackage = uri.authority
        targetPackage?.let {
            val targetContext = loader.findContextForPackage(context, uri, it)
            val result = loader.findResourceIdFromTypeAndNameResourceUri(targetContext, uri)
            assertNotNull(result)
        }
    }

    @Test
    fun findResourceIdFromTypeAndNameResourceUriDifferentPackage() {
        val uri = Uri.parse("android.resource://com.android.messaging/drawable/abc_ic_ab_back_material")
        val targetPackage = uri.authority
        targetPackage?.let {
            val targetContext = loader.findContextForPackage(context, uri, it)
            val result = loader.findResourceIdFromTypeAndNameResourceUri(targetContext, uri)
            assertNotNull(result)
        }
    }

    @Test
    fun unknownResourceIdFromTypeAndNameResourceUriSamePackage() {
        val uri = Uri.parse("android.resource://${context.packageName}/drawable/not_exist")
        val targetPackage = uri.authority
        targetPackage?.let {
            val targetContext = loader.findContextForPackage(context, uri, it)
            thrown.expect(IllegalArgumentException::class.java)
            thrown.expectMessage(startsWith("Failed to find name resource"))
            loader.findResourceIdFromTypeAndNameResourceUri(targetContext, uri)
        }
    }

    @Test
    fun unknownResourceIdFromTypeAndNameResourceUriDifferentPackage() {
        val uri = Uri.parse("android.resource://com.android.messaging/mipmap/not_exist")
        val targetPackage = uri.authority
        targetPackage?.let {
            val targetContext = loader.findContextForPackage(context, uri, it)
            thrown.expect(IllegalArgumentException::class.java)
            thrown.expectMessage(startsWith("Failed to find name resource"))
            loader.findResourceIdFromTypeAndNameResourceUri(targetContext, uri)
        }
    }

    @Test
    fun basicExtractResourceId() {
        val resourceId = R.drawable.ic_android
        val uri = Uri.parse("android.resource://${context.packageName}/$resourceId")
        val result = loader.extractResourceId(context, uri)
        assertEquals(resourceId, result)
    }

    @Test
    fun notResourceIdFromTypeAndNameResourceUri() {
        val uri = Uri.parse("android.resource://com.android.messaging/")
        thrown.expect(IllegalArgumentException::class.java)
        thrown.expectMessage(startsWith("Failed to find resource or unrecognized Uri format"))
        loader.findResourceIdFromTypeAndNameResourceUri(context, uri)
    }

    @Test
    fun emptyExtractResourceId() {
        val uri = Uri.parse("android.resource://coil.sample/")
        thrown.expect(IllegalArgumentException::class.java)
        thrown.expectMessage(startsWith("Failed to find resource id for"))
        loader.extractResourceId(context, uri)
    }

    @Test
    fun nonAndroidResourceUriExtractResourceId() {
        val uri = Uri.parse("android.resource://fake/file/path")
        thrown.expect(IllegalArgumentException::class.java)
        thrown.expectMessage(startsWith("Failed to find name resource id"))
        loader.extractResourceId(context, uri)
    }
}
