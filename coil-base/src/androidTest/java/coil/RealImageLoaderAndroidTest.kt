package coil

import android.content.ContentResolver.SCHEME_ANDROID_RESOURCE
import android.content.ContentResolver.SCHEME_CONTENT
import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.activityScenarioRule
import coil.base.test.R
import coil.decode.DataSource
import coil.decode.DecodeUtils
import coil.memory.MemoryCache
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.NullRequestDataException
import coil.request.SuccessResult
import coil.request.Tags
import coil.size.Precision
import coil.size.Scale
import coil.size.Size
import coil.util.ASSET_FILE_PATH_ROOT
import coil.util.TestActivity
import coil.util.activity
import coil.util.createMockWebServer
import coil.util.decodeBitmapAsset
import coil.util.enqueueImage
import coil.util.getDrawableCompat
import coil.util.isMainThread
import coil.util.runTestAsync
import coil.util.runTestMain
import coil.util.size
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
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
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RealImageLoaderAndroidTest {

    private lateinit var context: Context
    private lateinit var server: MockWebServer
    private lateinit var memoryCache: MemoryCache
    private lateinit var imageLoader: ImageLoader

    @get:Rule
    val activityRule = activityScenarioRule<TestActivity>()

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        server = createMockWebServer()
        memoryCache = MemoryCache.Builder(context)
            .maxSizeBytes(Int.MAX_VALUE)
            .build()
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
    fun string() = runTest {
        val data = server.url(IMAGE).toString()
        server.enqueueImage(IMAGE)
        testEnqueue(data)
        testExecute(data)
    }

    @Test
    fun httpUri() = runTest {
        val data = server.url(IMAGE).toString().toUri()
        server.enqueueImage(IMAGE)
        testEnqueue(data)
        testExecute(data)
    }

    @Test
    fun httpUrl() = runTest {
        val data = server.url(IMAGE)
        server.enqueueImage(IMAGE)
        testEnqueue(data)
        testExecute(data)
    }

    @Test
    fun resourceInt() = runTest {
        val data = R.drawable.normal
        testEnqueue(data)
        testExecute(data)
    }

    @Test
    fun resourceIntVector() = runTest {
        val data = R.drawable.ic_android
        testEnqueue(data, Size(100, 100))
        testExecute(data, Size(100, 100))
    }

    @Test
    fun resourceUriInt() = runTest {
        val data = "$SCHEME_ANDROID_RESOURCE://${context.packageName}/${R.drawable.normal}".toUri()
        testEnqueue(data)
        testExecute(data)
    }

    @Test
    fun resourceUriIntVector() = runTest {
        val data = "$SCHEME_ANDROID_RESOURCE://${context.packageName}/${R.drawable.ic_android}".toUri()
        testEnqueue(data, Size(100, 100))
        testExecute(data, Size(100, 100))
    }

    @Test
    fun resourceUriString() = runTest {
        val data = "$SCHEME_ANDROID_RESOURCE://${context.packageName}/drawable/normal".toUri()
        testEnqueue(data)
        testExecute(data)
    }

    @Test
    fun resourceUriStringVector() = runTest {
        val data = "$SCHEME_ANDROID_RESOURCE://${context.packageName}/drawable/ic_android".toUri()
        testEnqueue(data, Size(100, 100))
        testExecute(data, Size(100, 100))
    }

    @Test
    fun file() = runTest {
        val data = copyNormalImageAssetToCacheDir()
        testEnqueue(data)
        testExecute(data)
    }

    @Test
    fun fileUri() = runTest {
        val data = copyNormalImageAssetToCacheDir().toUri()
        testEnqueue(data)
        testExecute(data)
    }

    @Test
    fun assetUri() = runTest {
        val data = "$SCHEME_FILE:///$ASSET_FILE_PATH_ROOT/exif/large_metadata.jpg".toUri()
        testEnqueue(data, Size(75, 100))
        testExecute(data, Size(75, 100))
    }

    @Test
    fun contentUri() = runTest {
        val data = "$SCHEME_CONTENT://coil/$IMAGE".toUri()
        testEnqueue(data)
        testExecute(data)
    }

    @Test
    fun drawable() = runTest {
        val data = context.getDrawableCompat(R.drawable.normal)
        val expectedSize = Size(1080, 1350)
        testEnqueue(data, expectedSize)
        testExecute(data, expectedSize)
    }

    @Test
    fun bitmap() = runTest {
        val data = (context.getDrawableCompat(R.drawable.normal) as BitmapDrawable).bitmap
        val expectedSize = Size(1080, 1350)
        testEnqueue(data, expectedSize)
        testExecute(data, expectedSize)
    }

    @Test
    fun byteBuffer() = runTest {
        val data = ByteBuffer.wrap(context.resources.openRawResource(R.drawable.normal).readBytes())
        testEnqueue(data)
        testExecute(data)
    }

    @Test
    fun byteArray() = runTest {
        val data = context.resources.openRawResource(R.drawable.normal).readBytes()
        testEnqueue(data)
        testExecute(data)
    }

    // endregion

    @Test
    fun unsupportedDataThrows() = runTest {
        val data = Any()
        assertFailsWith<IllegalStateException> { testEnqueue(data) }
        assertFailsWith<IllegalStateException> { testExecute(data) }
    }

    @Test
    fun nullRequestDataShowsFallbackDrawable() = runTest {
        val error = ColorDrawable(Color.BLUE)
        val fallback = ColorDrawable(Color.BLACK)

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

    @Test
    fun loadedImageIsPresentInMemoryCache() = runTest {
        server.enqueueImage(IMAGE)
        val request = ImageRequest.Builder(context)
            .data(server.url(IMAGE))
            .size(100, 100)
            .build()
        val result = imageLoader.execute(request)

        assertTrue(result is SuccessResult)
        val bitmap = (result.drawable as BitmapDrawable).bitmap
        assertNotNull(bitmap)
        assertEquals(bitmap, imageLoader.memoryCache!![result.memoryCacheKey!!]?.bitmap)
    }

    @Test
    fun placeholderKeyReturnsCorrectMemoryCacheEntry() = runTest {
        val key = MemoryCache.Key("fake_key")
        val fileName = IMAGE
        val bitmap = decodeAssetAndAddToMemoryCache(key, fileName)

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

    @Test
    fun cachedValueIsResolvedSynchronously() = runTestMain {
        val key = MemoryCache.Key("fake_key")
        val fileName = IMAGE
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
    fun customMemoryCacheKey() = runTest {
        val imageLoader = ImageLoader(context)
        val key = MemoryCache.Key("fake_key")

        server.enqueueImage(IMAGE)
        val request = ImageRequest.Builder(context)
            .data(server.url(IMAGE))
            .memoryCacheKey(key)
            .build()
        val result = imageLoader.execute(request) as SuccessResult

        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(key, result.memoryCacheKey)
        assertSame(imageLoader.memoryCache!![key]!!.bitmap, result.drawable.toBitmap())
    }

    @Test
    fun customDiskCacheKey() = runTest {
        val imageLoader = ImageLoader(context)
        val key = "fake_key"

        server.enqueueImage(IMAGE)
        val request = ImageRequest.Builder(context)
            .data(server.url(IMAGE))
            .diskCacheKey(key)
            .build()
        val result = imageLoader.execute(request) as SuccessResult

        assertEquals(DataSource.NETWORK, result.dataSource)
        assertEquals(key, result.diskCacheKey)
        imageLoader.diskCache!![key]!!.use { assertNotNull(it) }
    }

    @Test
    fun callFactoryIsInitializedLazily() = runTest {
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

        server.enqueueImage(IMAGE)
        val request = ImageRequest.Builder(context)
            .data(server.url(IMAGE))
            .build()
        val result = imageLoader.execute(request)

        assertIs<SuccessResult>(result)
        assertTrue(isInitialized)
    }

    @Test
    fun memoryCacheIsInitializedLazily() = runTest {
        var isInitialized = false
        val imageLoader = ImageLoader.Builder(context)
            .memoryCache {
                check(!isInitialized)
                isInitialized = true
                null
            }
            .build()

        assertFalse(isInitialized)

        server.enqueueImage(IMAGE)
        val request = ImageRequest.Builder(context)
            .data(server.url(IMAGE))
            .build()
        val result = imageLoader.execute(request)

        assertIs<SuccessResult>(result)
        assertTrue(isInitialized)
    }

    @Test
    fun diskCacheIsInitializedLazily() = runTest {
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

        server.enqueueImage(IMAGE)
        val request = ImageRequest.Builder(context)
            .data(server.url(IMAGE))
            .build()
        val result = imageLoader.execute(request)

        assertIs<SuccessResult>(result)
        assertTrue(isInitialized)
    }

    @Test
    fun noMemoryCacheReturnsNoMemoryCacheKey() = runTest {
        val imageLoader = ImageLoader.Builder(context)
            .memoryCache(null)
            .build()

        server.enqueueImage(IMAGE)
        val request = ImageRequest.Builder(context)
            .data(server.url(IMAGE))
            .build()
        val result = imageLoader.execute(request)

        assertIs<SuccessResult>(result)
        assertNull(result.memoryCacheKey)
    }

    @Test
    fun noDiskCacheReturnsNoDiskCacheKey() = runTest {
        val imageLoader = ImageLoader.Builder(context)
            .diskCache(null)
            .build()

        server.enqueueImage(IMAGE)
        val request = ImageRequest.Builder(context)
            .data(server.url(IMAGE))
            .build()
        val result = imageLoader.execute(request)

        assertIs<SuccessResult>(result)
        assertNull(result.diskCacheKey)
    }

    @Test
    fun requestTagsArePassedToOkHttpInterceptor() = runTestAsync {
        val tags = Tags.from(mapOf(
            Map::class.java to emptyMap<String, String>(),
            String::class.java to "test"
        ))
        val callFactory = OkHttpClient.Builder()
            .addInterceptor { chain ->
                tags.asMap().forEach { (key, value) ->
                    assertEquals(value, chain.request().tag(key))
                }
                chain.proceed(chain.request())
            }
            .build()
        val imageLoader = ImageLoader.Builder(context).callFactory(callFactory).build()

        server.enqueueImage(IMAGE)
        val request = ImageRequest.Builder(context)
            .data(server.url(IMAGE).toString())
            .tags(tags)
            .build()
        val result = imageLoader.execute(request)
        if (result is ErrorResult) throw result.throwable
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/1201 */
    @Test
    fun veryLargeImage() = runTest {
        val request = ImageRequest.Builder(context)
            .data(R.drawable.very_large)
            .build()
        val result = imageLoader.execute(request)
        if (result is ErrorResult) throw result.throwable

        assertIs<SuccessResult>(result)
        val drawable = assertIs<BitmapDrawable>(result.drawable)
        val maxDimension = context.resources.displayMetrics.run { max(widthPixels, heightPixels) }
        val multiplier = DecodeUtils.computeSizeMultiplier(
            srcWidth = 9052,
            srcHeight = 4965,
            dstWidth = maxDimension,
            dstHeight = maxDimension,
            scale = Scale.FIT
        )
        val expectedWidth = (multiplier * 9052).roundToInt()
        val expectedHeight = (multiplier * 4965).roundToInt()
        assertTrue(drawable.bitmap.width in expectedWidth - 1..expectedWidth + 1)
        assertTrue(drawable.bitmap.height in expectedHeight - 1..expectedHeight + 1)
    }

    @Test
    fun imageViewWrapWidth() = runTest {
        val imageView = activityRule.scenario.activity.imageView
        withContext(Dispatchers.Main.immediate) {
            imageView.updateLayoutParams {
                width = ViewGroup.LayoutParams.WRAP_CONTENT
                height = ViewGroup.LayoutParams.MATCH_PARENT
            }
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            imageView.requestLayout()
            awaitFrame()
        }

        val request = ImageRequest.Builder(context)
            .data(R.drawable.very_large)
            .target(imageView)
            .build()
        val result = imageLoader.execute(request)
        if (result is ErrorResult) throw result.throwable

        assertIs<SuccessResult>(result)
        val drawable = assertIs<BitmapDrawable>(result.drawable)
        val multiplier = DecodeUtils.computeSizeMultiplier(
            srcWidth = 9052,
            srcHeight = 4965,
            dstWidth = 9052,
            dstHeight = imageView.height,
            scale = Scale.FIT
        )
        val expectedWidth = (multiplier * 9052).roundToInt()
        val expectedHeight = (multiplier * 4965).roundToInt()
        assertTrue(drawable.bitmap.width in expectedWidth - 1..expectedWidth + 1)
        assertTrue(drawable.bitmap.height in expectedHeight - 1..expectedHeight + 1)
    }

    @Test
    fun imageViewWrapHeight() = runTest {
        val imageView = activityRule.scenario.activity.imageView
        withContext(Dispatchers.Main.immediate) {
            imageView.updateLayoutParams {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            imageView.requestLayout()
            awaitFrame()
        }

        val request = ImageRequest.Builder(context)
            .data(R.drawable.very_large)
            .target(imageView)
            .build()
        val result = imageLoader.execute(request)
        if (result is ErrorResult) throw result.throwable

        assertIs<SuccessResult>(result)
        val drawable = assertIs<BitmapDrawable>(result.drawable)
        val multiplier = DecodeUtils.computeSizeMultiplier(
            srcWidth = 9052,
            srcHeight = 4965,
            dstWidth = imageView.width,
            dstHeight = 4965,
            scale = Scale.FIT
        )
        val expectedWidth = (multiplier * 9052).roundToInt()
        val expectedHeight = (multiplier * 4965).roundToInt()
        assertTrue(drawable.bitmap.width in expectedWidth - 1..expectedWidth + 1)
        assertTrue(drawable.bitmap.height in expectedHeight - 1..expectedHeight + 1)
    }

    private suspend fun testEnqueue(data: Any, expectedSize: Size = Size(80, 100)) {
        val imageView = activityRule.scenario.activity.imageView
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER

        assertNull(imageView.drawable)

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

        val drawable = assertIs<BitmapDrawable>(imageView.drawable)
        assertEquals(expectedSize, drawable.bitmap.size)
    }

    private suspend fun testExecute(data: Any, expectedSize: Size = Size(80, 100)) {
        val request = ImageRequest.Builder(context)
            .data(data)
            .size(100, 100)
            .build()
        val result = imageLoader.execute(request)

        if (result is ErrorResult) {
            throw result.throwable
        }

        assertIs<SuccessResult>(result)
        val drawable = assertIs<BitmapDrawable>(result.drawable)
        assertEquals(expectedSize, drawable.bitmap.size)
    }

    private fun copyNormalImageAssetToCacheDir(): File {
        val file = File(context.cacheDir, IMAGE)
        val source = context.assets.open(IMAGE).source()
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
        private const val IMAGE = "normal.jpg"
    }
}
