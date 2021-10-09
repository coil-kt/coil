package coil

import android.content.ContentResolver.SCHEME_ANDROID_RESOURCE
import android.content.ContentResolver.SCHEME_CONTENT
import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.activityScenarioRule
import coil.base.test.R
import coil.decode.DataSource
import coil.fetch.AssetUriFetcher.Companion.ASSET_FILE_PATH_ROOT
import coil.memory.MemoryCache
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.NullRequestDataException
import coil.request.SuccessResult
import coil.size.PixelSize
import coil.size.Precision
import coil.util.TestActivity
import coil.util.activity
import coil.util.createMockWebServer
import coil.util.decodeBitmapAsset
import coil.util.getDrawableCompat
import coil.util.isMainThread
import coil.util.runBlockingTest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import okio.buffer
import okio.sink
import okio.source
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RealImageLoaderTest {

    private lateinit var context: Context
    private lateinit var server: MockWebServer
    private lateinit var memoryCache: MemoryCache
    private lateinit var imageLoader: ImageLoader

    @get:Rule
    val activityRule = activityScenarioRule<TestActivity>()

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        server = createMockWebServer(context, IMAGE_NAME, IMAGE_NAME)
        memoryCache = MemoryCache.Builder(context).maxSizeBytes(Int.MAX_VALUE).build()
        imageLoader = ImageLoader.Builder(context)
            .memoryCache(memoryCache)
            .diskCache(null)
            .build()
        activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
    }

    @After
    fun after() {
        server.shutdown()
        imageLoader.shutdown()
    }

    // region Test all the supported data types.

    @Test
    fun string() {
        val data = server.url(IMAGE_NAME).toString()
        testEnqueue(data)
        testExecute(data)
    }

    @Test
    fun httpUri() {
        val data = server.url(IMAGE_NAME).toString().toUri()
        testEnqueue(data)
        testExecute(data)
    }

    @Test
    fun httpUrl() {
        val data = server.url(IMAGE_NAME)
        testEnqueue(data)
        testExecute(data)
    }

    @Test
    fun resourceInt() {
        val data = R.drawable.normal
        testEnqueue(data)
        testExecute(data)
    }

    @Test
    fun resourceIntVector() {
        val data = R.drawable.ic_android
        testEnqueue(data, PixelSize(100, 100))
        testExecute(data, PixelSize(100, 100))
    }

    @Test
    fun resourceUriInt() {
        val data = "$SCHEME_ANDROID_RESOURCE://${context.packageName}/${R.drawable.normal}".toUri()
        testEnqueue(data)
        testExecute(data)
    }

    @Test
    fun resourceUriIntVector() {
        val data = "$SCHEME_ANDROID_RESOURCE://${context.packageName}/${R.drawable.ic_android}".toUri()
        testEnqueue(data, PixelSize(100, 100))
        testExecute(data, PixelSize(100, 100))
    }

    @Test
    fun resourceUriString() {
        val data = "$SCHEME_ANDROID_RESOURCE://${context.packageName}/drawable/normal".toUri()
        testEnqueue(data)
        testExecute(data)
    }

    @Test
    fun resourceUriStringVector() {
        val data = "$SCHEME_ANDROID_RESOURCE://${context.packageName}/drawable/ic_android".toUri()
        testEnqueue(data, PixelSize(100, 100))
        testExecute(data, PixelSize(100, 100))
    }

    @Test
    fun file() {
        val data = copyNormalImageAssetToCacheDir()
        testEnqueue(data)
        testExecute(data)
    }

    @Test
    fun fileUri() {
        val data = copyNormalImageAssetToCacheDir().toUri()
        testEnqueue(data)
        testExecute(data)
    }

    @Test
    fun assetUri() {
        val data = "$SCHEME_FILE:///$ASSET_FILE_PATH_ROOT/exif/large_metadata.jpg".toUri()
        testEnqueue(data, PixelSize(75, 100))
        testExecute(data, PixelSize(75, 100))
    }

    @Test
    fun contentUri() {
        val data = "$SCHEME_CONTENT://coil/$IMAGE_NAME".toUri()
        testEnqueue(data)
        testExecute(data)
    }

    @Test
    fun drawable() {
        val data = context.getDrawableCompat(R.drawable.normal)
        val expectedSize = PixelSize(1080, 1350)
        testEnqueue(data, expectedSize)
        testExecute(data, expectedSize)
    }

    @Test
    fun bitmap() {
        val data = (context.getDrawableCompat(R.drawable.normal) as BitmapDrawable).bitmap
        val expectedSize = PixelSize(1080, 1350)
        testEnqueue(data, expectedSize)
        testExecute(data, expectedSize)
    }

    @Test
    fun byteBuffer() {
        val data = ByteBuffer.wrap(context.resources.openRawResource(R.drawable.normal).readBytes())
        testEnqueue(data)
        testExecute(data)
    }

    // endregion

    @Test
    fun unsupportedDataThrows() {
        val data = Any()
        assertFailsWith<IllegalStateException> { testEnqueue(data) }
        assertFailsWith<IllegalStateException> { testExecute(data) }
    }

    @Test
    fun nullRequestDataShowsFallbackDrawable() {
        val error = ColorDrawable(Color.BLUE)
        val fallback = ColorDrawable(Color.BLACK)

        runBlocking {
            suspendCancellableCoroutine<Unit> { continuation ->
                var hasCalledTargetOnError = false

                val request = ImageRequest.Builder(context)
                    .data(null)
                    .size(100, 100)
                    .error(error)
                    .fallback(fallback)
                    .target(
                        onStart = { throw IllegalStateException() },
                        onError = { drawable ->
                            check(drawable === fallback)
                            hasCalledTargetOnError = true
                        },
                        onSuccess = { throw IllegalStateException() }
                    )
                    .listener(
                        onStart = { throw IllegalStateException() },
                        onSuccess = { _, _ -> throw IllegalStateException() },
                        onCancel = { throw IllegalStateException() },
                        onError = { _, result ->
                            if (hasCalledTargetOnError && result.throwable is NullRequestDataException) {
                                continuation.resume(Unit)
                            } else {
                                continuation.resumeWithException(result.throwable)
                            }
                        }
                    )
                    .build()
                imageLoader.enqueue(request)
            }
        }
    }

    @Test
    fun loadedImageIsPresentInMemoryCache() {
        val result = runBlocking {
            val request = ImageRequest.Builder(context)
                .data(server.url(IMAGE_NAME))
                .size(100, 100)
                .build()
            imageLoader.execute(request)
        }

        assertTrue(result is SuccessResult)
        val bitmap = (result.drawable as BitmapDrawable).bitmap
        assertNotNull(bitmap)
        assertEquals(bitmap, imageLoader.memoryCache!![result.memoryCacheKey!!]?.bitmap)
    }

    @Test
    fun placeholderKeyReturnsCorrectMemoryCacheEntry() {
        val key = MemoryCache.Key("fake_key")
        val fileName = IMAGE_NAME
        val bitmap = decodeAssetAndAddToMemoryCache(key, fileName)

        runBlocking {
            suspendCancellableCoroutine<Unit> { continuation ->
                val request = ImageRequest.Builder(context)
                    .memoryCacheKey(key)
                    .placeholderMemoryCacheKey(key)
                    .data("$SCHEME_FILE:///$ASSET_FILE_PATH_ROOT/$fileName")
                    .size(100, 100)
                    .precision(Precision.INEXACT)
                    .allowHardware(true)
                    .dispatcher(Dispatchers.Main.immediate)
                    .target(
                        onStart = {
                            // The drawable in the memory cache should be returned here.
                            assertEquals(bitmap, (it as BitmapDrawable).bitmap)
                        },
                        onSuccess = {
                            // The same drawable should be returned since the drawable is valid for this request.
                            assertEquals(bitmap, (it as BitmapDrawable).bitmap)
                        }
                    )
                    .listener(
                        onSuccess = { _, _ -> continuation.resume(Unit) },
                        onError = { _, result -> continuation.resumeWithException(result.throwable) },
                        onCancel = { continuation.cancel() }
                    )
                    .build()
                imageLoader.enqueue(request)
            }
        }
    }

    @Test
    fun cachedValueIsResolvedSynchronously() = runBlockingTest {
        val key = MemoryCache.Key("fake_key")
        val fileName = IMAGE_NAME
        decodeAssetAndAddToMemoryCache(key, fileName)

        var isSuccessful = false
        val request = ImageRequest.Builder(context)
            .data("$SCHEME_FILE:///$ASSET_FILE_PATH_ROOT/$fileName")
            .size(100, 100)
            .precision(Precision.INEXACT)
            .memoryCacheKey(key)
            .target { isSuccessful = true }
            .build()
        imageLoader.enqueue(request).dispose()

        // isSuccessful should be synchronously set to true.
        assertTrue(isSuccessful)
    }

    @Test
    fun newBuilderSharesResources() {
        val imageLoader1 = ImageLoader(context)
        val imageLoader2 = imageLoader1.newBuilder().build()

        assertSame(imageLoader1.defaults, imageLoader2.defaults)
        assertSame(
            (imageLoader1 as RealImageLoader).componentRegistry,
            (imageLoader2 as RealImageLoader).componentRegistry
        )
        assertSame(imageLoader1.memoryCache, imageLoader2.memoryCache)
        assertSame(imageLoader1.diskCache, imageLoader2.diskCache)
    }

    @Test
    fun customMemoryCacheKey() {
        val imageLoader = ImageLoader(context)
        val key = MemoryCache.Key("fake_key")

        val result = runBlocking {
            val request = ImageRequest.Builder(context)
                .data(server.url(IMAGE_NAME))
                .memoryCacheKey(key)
                .build()
            imageLoader.execute(request) as SuccessResult
        }

        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(key, result.memoryCacheKey)
        assertSame(imageLoader.memoryCache!![key]!!.bitmap, result.drawable.toBitmap())
    }

    @Test
    fun customDiskCacheKey() {
        val imageLoader = ImageLoader(context)
        val key = "fake_key"

        val result = runBlocking {
            val request = ImageRequest.Builder(context)
                .data(server.url(IMAGE_NAME))
                .diskCacheKey(key)
                .build()
            imageLoader.execute(request) as SuccessResult
        }

        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(key, result.diskCacheKey)
        imageLoader.diskCache!![key]!!.use { assertNotNull(it) }
    }

    @Test
    fun callFactoryIsInitializedLazily() {
        var isInitialized = false
        val imageLoader = ImageLoader.Builder(context)
            .callFactory {
                assertFalse(isMainThread())
                check(!isInitialized)
                isInitialized = true
                OkHttpClient()
            }
            .build()

        assertFalse(isInitialized)

        runBlocking {
            val request = ImageRequest.Builder(context)
                .data(server.url(IMAGE_NAME))
                .build()
            imageLoader.execute(request) as SuccessResult
        }

        assertTrue(isInitialized)
    }

    @Test
    fun diskCacheIsInitializedLazily() {
        var isInitialized = false
        val imageLoader = ImageLoader.Builder(context)
            .diskCache {
                assertFalse(isMainThread())
                check(!isInitialized)
                isInitialized = true
                null
            }
            .build()

        assertFalse(isInitialized)

        runBlocking {
            val request = ImageRequest.Builder(context)
                .data(server.url(IMAGE_NAME))
                .build()
            imageLoader.execute(request) as SuccessResult
        }

        assertTrue(isInitialized)
    }

    private fun testEnqueue(data: Any, expectedSize: PixelSize = PixelSize(80, 100)) {
        val imageView = activityRule.scenario.activity.imageView
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER

        assertNull(imageView.drawable)

        runBlocking {
            suspendCancellableCoroutine<Unit> { continuation ->
                val request = ImageRequest.Builder(context)
                    .data(data)
                    .target(imageView)
                    .size(100, 100)
                    .listener(
                        onSuccess = { _, _ -> continuation.resume(Unit) },
                        onError = { _, result -> continuation.resumeWithException(result.throwable) },
                        onCancel = { continuation.resumeWithException(CancellationException()) }
                    )
                    .build()
                imageLoader.enqueue(request)
            }
        }

        val drawable = imageView.drawable
        assertTrue(drawable is BitmapDrawable)
        assertEquals(expectedSize, drawable.bitmap.size)
    }

    private fun testExecute(data: Any, expectedSize: PixelSize = PixelSize(80, 100)) {
        val result = runBlocking {
            val request = ImageRequest.Builder(context)
                .data(data)
                .size(100, 100)
                .build()
            imageLoader.execute(request)
        }

        if (result is ErrorResult) {
            throw result.throwable
        }

        assertTrue(result is SuccessResult)
        val drawable = result.drawable
        assertTrue(drawable is BitmapDrawable)
        assertEquals(expectedSize, drawable.bitmap.size)
    }

    private fun copyNormalImageAssetToCacheDir(): File {
        val file = File(context.cacheDir, IMAGE_NAME)
        val source = context.assets.open(IMAGE_NAME).source()
        val sink = file.sink().buffer()
        source.use { sink.use { sink.writeAll(source) } }
        return file
    }

    @Suppress("SameParameterValue")
    private fun decodeAssetAndAddToMemoryCache(key: MemoryCache.Key, fileName: String): Bitmap {
        val bitmap = context.decodeBitmapAsset(fileName)
        memoryCache[key] = MemoryCache.Value(bitmap)
        return bitmap
    }

    companion object {
        private const val IMAGE_NAME = "normal.jpg"
    }
}
