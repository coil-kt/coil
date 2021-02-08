@file:Suppress("SameParameterValue")

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
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.activityScenarioRule
import coil.base.test.R
import coil.bitmap.BitmapPool
import coil.bitmap.RealBitmapReferenceCounter
import coil.decode.BitmapFactoryDecoder
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.decode.Options
import coil.fetch.AssetUriFetcher.Companion.ASSET_FILE_PATH_ROOT
import coil.memory.MemoryCache
import coil.memory.RealMemoryCache
import coil.memory.RealWeakMemoryCache
import coil.memory.StrongMemoryCache
import coil.request.CachePolicy
import coil.request.DefaultRequestOptions
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.NullRequestDataException
import coil.request.SuccessResult
import coil.size.PixelSize
import coil.size.Precision
import coil.size.Size
import coil.util.ImageLoaderOptions
import coil.util.TestActivity
import coil.util.Utils
import coil.util.activity
import coil.util.createMockWebServer
import coil.util.decodeBitmapAsset
import coil.util.getDrawableCompat
import coil.util.runBlockingTest
import coil.util.size
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import okio.BufferedSource
import okio.buffer
import okio.sink
import okio.source
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RealImageLoaderTest {

    private lateinit var context: Context
    private lateinit var server: MockWebServer
    private lateinit var strongMemoryCache: StrongMemoryCache
    private lateinit var imageLoader: RealImageLoader

    @get:Rule
    val activityRule = activityScenarioRule<TestActivity>()

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        server = createMockWebServer(context, IMAGE_NAME, IMAGE_NAME)
        val bitmapPool = BitmapPool(Int.MAX_VALUE)
        val weakMemoryCache = RealWeakMemoryCache(null)
        val referenceCounter = RealBitmapReferenceCounter(weakMemoryCache, bitmapPool, null)
        strongMemoryCache = StrongMemoryCache(weakMemoryCache, referenceCounter, Int.MAX_VALUE, null)
        val memoryCache = RealMemoryCache(strongMemoryCache, weakMemoryCache, referenceCounter, bitmapPool)
        imageLoader = RealImageLoader(
            context = context,
            defaults = DefaultRequestOptions(),
            bitmapPool = bitmapPool,
            memoryCache = memoryCache,
            callFactory = OkHttpClient(),
            eventListenerFactory = EventListener.Factory.NONE,
            componentRegistry = ComponentRegistry(),
            options = ImageLoaderOptions(),
            logger = null
        )
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
        testExecute(data, PixelSize(100, 133))
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

    // endregion

    @Test
    fun unsupportedDataThrows() {
        val data = Any()
        assertFailsWith<IllegalStateException> { testEnqueue(data) }
        assertFailsWith<IllegalStateException> { testExecute(data) }
    }

    @Test
    fun memoryCacheDisabled_preloadDoesNotDecode() {
        val imageLoader = ImageLoader.Builder(context)
            .componentRegistry {
                add(object : Decoder {
                    override fun handles(source: BufferedSource, mimeType: String?) = true

                    override suspend fun decode(
                        pool: BitmapPool,
                        source: BufferedSource,
                        size: Size,
                        options: Options
                    ) = throw IllegalStateException("Decode should not be called.")
                })
            }
            .build()

        val url = server.url(IMAGE_NAME)
        val cacheFolder = Utils.getDefaultCacheDirectory(context).apply {
            deleteRecursively()
            mkdirs()
        }

        assertTrue(cacheFolder.listFiles().isNullOrEmpty())

        runBlocking {
            suspendCancellableCoroutine<Unit> { continuation ->
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .listener(
                        onSuccess = { _, _ -> continuation.resume(Unit) },
                        onError = { _, throwable -> continuation.resumeWithException(throwable) },
                        onCancel = { continuation.resumeWithException(CancellationException()) }
                    )
                    .build()
                imageLoader.enqueue(request)
            }
        }

        val cacheFile = cacheFolder.listFiles().orEmpty().find { it.name.contains(Cache.key(url)) && it.length() == IMAGE_SIZE }
        assertNotNull(cacheFile, "Did not find the image file in the disk cache.")
    }

    @Test
    fun memoryCacheDisabled_getDoesDecode() {
        var numDecodes = 0
        val imageLoader = ImageLoader.Builder(context)
            .componentRegistry {
                add(object : Decoder {
                    private val delegate = BitmapFactoryDecoder(context)

                    override fun handles(source: BufferedSource, mimeType: String?) = true

                    override suspend fun decode(
                        pool: BitmapPool,
                        source: BufferedSource,
                        size: Size,
                        options: Options
                    ): DecodeResult {
                        numDecodes++
                        return delegate.decode(pool, source, size, options)
                    }
                })
            }
            .build()

        val url = server.url(IMAGE_NAME)
        val cacheFolder = Utils.getDefaultCacheDirectory(context).apply {
            deleteRecursively()
            mkdirs()
        }

        assertTrue(cacheFolder.listFiles().isNullOrEmpty())

        runBlocking {
            val request = ImageRequest.Builder(context)
                .data(url)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .build()
            imageLoader.execute(request)
        }

        val cacheFile = cacheFolder.listFiles().orEmpty().find { it.name.contains(Cache.key(url)) && it.length() == IMAGE_SIZE }
        assertNotNull(cacheFile, "Did not find the image file in the disk cache.")
        assertEquals(1, numDecodes)
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
                        onError = { _, throwable ->
                            if (hasCalledTargetOnError && throwable is NullRequestDataException) {
                                continuation.resume(Unit)
                            } else {
                                continuation.resumeWithException(throwable)
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
        assertEquals(bitmap, imageLoader.memoryCache[result.metadata.memoryCacheKey!!])
    }

    @Test
    fun placeholderKeyReturnsCorrectMemoryCacheEntry() {
        val key = MemoryCache.Key("fake_key")
        val fileName = "normal.jpg"
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
                        onError = { _, throwable -> continuation.resumeWithException(throwable) },
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
        val fileName = "normal.jpg"
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
    fun newBuilderSharesMemoryCache() {
        val key = MemoryCache.Key("fake_key")
        val imageLoader1 = ImageLoader(context)
        val imageLoader2 = imageLoader1.newBuilder().build()

        assertSame(imageLoader1.memoryCache, imageLoader2.memoryCache)
        assertNull(imageLoader1.memoryCache[key])
        assertNull(imageLoader2.memoryCache[key])

        val bitmap = createBitmap(100, 100)
        imageLoader1.memoryCache[key] = bitmap

        assertSame(bitmap, imageLoader2.memoryCache[key])
    }

    @Test
    fun newBuilderSharesBitmapPool() {
        val imageLoader1 = ImageLoader.Builder(context).bitmapPoolPercentage(0.5).build()
        val imageLoader2 = imageLoader1.newBuilder().build()

        assertSame(imageLoader1.bitmapPool, imageLoader2.bitmapPool)
        assertNull(imageLoader1.bitmapPool.getOrNull(100, 100, Bitmap.Config.ARGB_8888))
        assertNull(imageLoader2.bitmapPool.getOrNull(100, 100, Bitmap.Config.ARGB_8888))

        val bitmap = createBitmap(100, 100)
        imageLoader1.bitmapPool.put(bitmap)

        assertSame(bitmap, imageLoader2.bitmapPool.getOrNull(100, 100, Bitmap.Config.ARGB_8888))
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
                        onError = { _, throwable -> continuation.resumeWithException(throwable) },
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

    private fun testExecute(data: Any, expectedSize: PixelSize = PixelSize(100, 125)) {
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

    private fun decodeAssetAndAddToMemoryCache(key: MemoryCache.Key, fileName: String): Bitmap {
        val bitmap = context.decodeBitmapAsset(fileName)
        strongMemoryCache.set(key, bitmap, false)
        return bitmap
    }

    companion object {
        private const val IMAGE_NAME = "normal.jpg"
        private const val IMAGE_SIZE = 443291L
    }
}
