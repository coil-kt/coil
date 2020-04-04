package coil

import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import androidx.test.core.app.ApplicationProvider
import coil.annotation.ExperimentalCoilApi
import coil.bitmappool.BitmapPool
import coil.fetch.AssetUriFetcher.Companion.ASSET_FILE_PATH_ROOT
import coil.memory.BitmapReferenceCounter
import coil.memory.MemoryCache
import coil.memory.RealWeakMemoryCache
import coil.request.LoadRequest
import coil.size.Precision
import coil.util.decodeBitmapAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * Basic tests for [RealImageLoader] that don't touch Android's graphics pipeline ([BitmapFactory], [ImageDecoder], etc.).
 */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoilApi::class)
class RealImageLoaderBasicTest {

    private lateinit var context: Context
    private lateinit var memoryCache: MemoryCache
    private lateinit var imageLoader: ImageLoader

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        val bitmapPool = BitmapPool(Long.MAX_VALUE)
        val weakMemoryCache = RealWeakMemoryCache()
        val referenceCounter = BitmapReferenceCounter(weakMemoryCache, bitmapPool, null)
        memoryCache = MemoryCache(weakMemoryCache, referenceCounter, Int.MAX_VALUE, null)
        imageLoader = RealImageLoader(
            context,
            DefaultRequestOptions(),
            bitmapPool,
            referenceCounter,
            memoryCache,
            weakMemoryCache,
            OkHttpClient(),
            EventListener.Factory.NONE,
            ComponentRegistry(),
            null
        )
    }

    @After
    fun after() {
        imageLoader.shutdown()
    }

    @Test
    fun `cachedHardwareBitmap - disallowHardware`() {
        val key = "fake_key"
        val fileName = "normal.jpg"
        val bitmap = decodeAssetAndAddToMemoryCache(key, fileName)

        runBlocking {
            var error: Throwable? = null
            val request = LoadRequest.Builder(context)
                .key(key)
                .data("$SCHEME_FILE:///$ASSET_FILE_PATH_ROOT/$fileName")
                .size(100, 100)
                .precision(Precision.INEXACT)
                .allowHardware(false)
                .dispatcher(Dispatchers.Main.immediate)
                .target(
                    onStart = {
                        // The hardware bitmap should not be returned as a placeholder.
                        assertNull(it)
                    },
                    onSuccess = {
                        // The hardware bitmap should not be returned as the result.
                        assertNotEquals(bitmap, (it as BitmapDrawable).bitmap)
                    }
                )
                .listener(
                    onError = { _, throwable -> error = throwable }
                )
                .build()
            imageLoader.execute(request).await()

            // Rethrow any errors that occurred while loading.
            error?.let { throw it }
        }
    }

    @Test
    fun `cachedHardwareBitmap - allowHardware`() {
        val key = "fake_key"
        val fileName = "normal.jpg"
        val bitmap = decodeAssetAndAddToMemoryCache(key, fileName)

        runBlocking {
            var error: Throwable? = null
            val request = LoadRequest.Builder(context)
                .key(key)
                .data("$SCHEME_FILE:///$ASSET_FILE_PATH_ROOT/$fileName")
                .size(100, 100)
                .precision(Precision.INEXACT)
                .allowHardware(true)
                .dispatcher(Dispatchers.Main.immediate)
                .target(
                    onStart = {
                        assertEquals(bitmap, (it as BitmapDrawable).bitmap)
                    },
                    onSuccess = {
                        assertEquals(bitmap, (it as BitmapDrawable).bitmap)
                    }
                )
                .listener(
                    onError = { _, throwable -> error = throwable }
                )
                .build()
            imageLoader.execute(request).await()

            // Rethrow any errors that occurred while loading.
            error?.let { throw it }
        }
    }

    @Suppress("SameParameterValue")
    private fun decodeAssetAndAddToMemoryCache(key: String, fileName: String): Bitmap {
        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.HARDWARE }
        val bitmap = context.decodeBitmapAsset(fileName, options)
        assertEquals(Bitmap.Config.HARDWARE, bitmap.config)
        memoryCache.set(key, bitmap, false)
        return bitmap
    }
}
