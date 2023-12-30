package coil3

import android.content.ContentResolver.SCHEME_ANDROID_RESOURCE
import android.content.ContentResolver.SCHEME_CONTENT
import android.content.ContentResolver.SCHEME_FILE
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.rules.activityScenarioRule
import coil3.core.test.R
import coil3.decode.DecodeUtils
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.NullRequestDataException
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.target
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
import coil3.test.utils.ViewTestActivity
import coil3.test.utils.activity
import coil3.test.utils.context
import coil3.test.utils.decodeBitmapAsset
import coil3.test.utils.runTestMain
import coil3.test.utils.size
import coil3.util.ASSET_FILE_PATH_ROOT
import coil3.util.getDrawableCompat
import coil3.util.toDrawable
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import okio.sink
import okio.source
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RealImageLoaderAndroidTest {

    private lateinit var fileSystem: FakeFileSystem
    private lateinit var memoryCache: MemoryCache
    private lateinit var diskCache: DiskCache
    private lateinit var imageLoader: ImageLoader

    @get:Rule
    val activityRule = activityScenarioRule<ViewTestActivity>()

    @Before
    fun before() {
        fileSystem = FakeFileSystem()
        memoryCache = MemoryCache.Builder()
            .maxSizeBytes(Long.MAX_VALUE)
            .build()
        diskCache = DiskCache.Builder()
            .directory(fileSystem.workingDirectory)
            .fileSystem(fileSystem)
            .maxSizeBytes(Long.MAX_VALUE)
            .build()
        imageLoader = ImageLoader.Builder(context)
            .memoryCache(memoryCache)
            .diskCache(diskCache)
            .fileSystem(fileSystem)
            .build()
        activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
    }

    @After
    fun after() {
        imageLoader.shutdown()
        diskCache.shutdown()
        fileSystem.checkNoOpenFiles()
    }

    // region Test all the supported data types.

//    @Test
//    fun string() = runTest {
//        val data = server.url(IMAGE).toString()
//        server.enqueueImage(IMAGE)
//        testEnqueue(data)
//        testExecute(data)
//    }

//    @Test
//    fun httpUri() = runTest {
//        val data = server.url(IMAGE).toString().toUri()
//        server.enqueueImage(IMAGE)
//        testEnqueue(data)
//        testExecute(data)
//    }

//    @Test
//    fun httpUrl() = runTest {
//        val data = server.url(IMAGE)
//        server.enqueueImage(IMAGE)
//        testEnqueue(data)
//        testExecute(data)
//    }

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
        val error = ColorDrawable(Color.BLUE).asCoilImage()
        val fallback = ColorDrawable(Color.BLACK).asCoilImage()

        suspendCancellableCoroutine { continuation ->
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

//    @Test
//    fun loadedImageIsPresentInMemoryCache() = runTest {
//        server.enqueueImage(IMAGE)
//        val request = ImageRequest.Builder(context)
//            .data(server.url(IMAGE))
//            .size(100, 100)
//            .build()
//        val result = imageLoader.execute(request)
//
//        assertTrue(result is SuccessResult)
//        val bitmap = (result.image as BitmapDrawable).bitmap
//        assertNotNull(bitmap)
//        assertEquals(bitmap, imageLoader.memoryCache!![result.memoryCacheKey!!]?.bitmap)
//    }

    @Test
    fun placeholderKeyReturnsCorrectMemoryCacheEntry() = runTest {
        val key = MemoryCache.Key("fake_key")
        val fileName = IMAGE
        val bitmap = decodeAssetAndAddToMemoryCache(key, fileName)

        suspendCancellableCoroutine { continuation ->
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
                        assertEquals(bitmap, (it as BitmapImage).bitmap)
                    },
                    onSuccess = {
                        // The same drawable should be returned since the drawable is valid for this request.
                        assertEquals(bitmap, (it as BitmapImage).bitmap)
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

        assertEquals(imageLoader1.defaults, imageLoader2.defaults)
        assertEquals(
            (imageLoader1 as RealImageLoader).options.componentRegistry,
            (imageLoader2 as RealImageLoader).options.componentRegistry,
        )
        assertSame(imageLoader1.memoryCache, imageLoader2.memoryCache)
        assertSame(imageLoader1.diskCache, imageLoader2.diskCache)
    }

//    @Test
//    fun customMemoryCacheKey() = runTest {
//        val imageLoader = ImageLoader(context)
//        val key = MemoryCache.Key("fake_key")
//
//        server.enqueueImage(IMAGE)
//        val request = ImageRequest.Builder(context)
//            .data(server.url(IMAGE))
//            .memoryCacheKey(key)
//            .build()
//        val result = imageLoader.execute(request) as SuccessResult
//
//        assertEquals(DataSource.NETWORK, result.dataSource)
//        assertEquals(key, result.memoryCacheKey)
//        assertSame(imageLoader.memoryCache!![key]!!.bitmap, result.image.toBitmap())
//    }

//    @Test
//    fun customDiskCacheKey() = runTest {
//        val imageLoader = ImageLoader(context)
//        val key = "fake_key"
//
//        server.enqueueImage(IMAGE)
//        val request = ImageRequest.Builder(context)
//            .data(server.url(IMAGE))
//            .diskCacheKey(key)
//            .build()
//        val result = imageLoader.execute(request) as SuccessResult
//
//        assertEquals(DataSource.NETWORK, result.dataSource)
//        assertEquals(key, result.diskCacheKey)
//        imageLoader.diskCache!!.openSnapshot(key)!!.use { assertNotNull(it) }
//    }

//    @Test
//    fun callFactoryIsInitializedLazily() = runTest {
//        var isInitialized = false
//        val imageLoader = ImageLoader.Builder(context)
//            .callFactory {
//                assertFalse(isMainThread())
//                check(!isInitialized)
//                isInitialized = true
//                OkHttpClient()
//            }
//            .build()
//
//        assertFalse(isInitialized)
//
//        server.enqueueImage(IMAGE)
//        val request = ImageRequest.Builder(context)
//            .data(server.url(IMAGE))
//            .build()
//        val result = imageLoader.execute(request)
//
//        assertIs<SuccessResult>(result)
//        assertTrue(isInitialized)
//    }

//    @Test
//    fun memoryCacheIsInitializedLazily() = runTest {
//        var isInitialized = false
//        val imageLoader = ImageLoader.Builder(context)
//            .memoryCache {
//                check(!isInitialized)
//                isInitialized = true
//                null
//            }
//            .build()
//
//        assertFalse(isInitialized)
//
//        server.enqueueImage(IMAGE)
//        val request = ImageRequest.Builder(context)
//            .data(server.url(IMAGE))
//            .build()
//        val result = imageLoader.execute(request)
//
//        assertIs<SuccessResult>(result)
//        assertTrue(isInitialized)
//    }

//    @Test
//    fun diskCacheIsInitializedLazily() = runTest {
//        var isInitialized = false
//        val imageLoader = ImageLoader.Builder(context)
//            .diskCache {
//                assertFalse(isMainThread())
//                check(!isInitialized)
//                isInitialized = true
//                null
//            }
//            .build()
//
//        assertFalse(isInitialized)
//
//        server.enqueueImage(IMAGE)
//        val request = ImageRequest.Builder(context)
//            .data(server.url(IMAGE))
//            .build()
//        val result = imageLoader.execute(request)
//
//        assertIs<SuccessResult>(result)
//        assertTrue(isInitialized)
//    }

//    @Test
//    fun noMemoryCacheReturnsNoMemoryCacheKey() = runTest {
//        val imageLoader = ImageLoader.Builder(context)
//            .memoryCache(null)
//            .build()
//
//        server.enqueueImage(IMAGE)
//        val request = ImageRequest.Builder(context)
//            .data(server.url(IMAGE))
//            .build()
//        val result = imageLoader.execute(request)
//
//        assertIs<SuccessResult>(result)
//        assertNull(result.memoryCacheKey)
//    }

//    @Test
//    fun noDiskCacheReturnsNoDiskCacheKey() = runTest {
//        val imageLoader = ImageLoader.Builder(context)
//            .diskCache(null)
//            .build()
//
//        server.enqueueImage(IMAGE)
//        val request = ImageRequest.Builder(context)
//            .data(server.url(IMAGE))
//            .build()
//        val result = imageLoader.execute(request)
//
//        assertIs<SuccessResult>(result)
//        assertNull(result.diskCacheKey)
//    }

    /** Regression test: https://github.com/coil-kt/coil/issues/1201 */
    @Test
    fun veryLargeImage() = runTest {
        val request = ImageRequest.Builder(context)
            .data(R.drawable.very_large)
            .build()
        val result = imageLoader.execute(request)
        if (result is ErrorResult) throw result.throwable

        assertIs<SuccessResult>(result)
        val image = assertIs<BitmapImage>(result.image)
        val maxDimension = context.resources.displayMetrics.run { maxOf(widthPixels, heightPixels) }
        val multiplier = DecodeUtils.computeSizeMultiplier(
            srcWidth = 9052,
            srcHeight = 4965,
            dstWidth = maxDimension,
            dstHeight = maxDimension,
            scale = Scale.FIT
        )
        val expectedWidth = (multiplier * 9052).roundToInt()
        val expectedHeight = (multiplier * 4965).roundToInt()
        assertTrue(image.bitmap.width in expectedWidth - 1..expectedWidth + 1)
        assertTrue(image.bitmap.height in expectedHeight - 1..expectedHeight + 1)
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
        val image = assertIs<BitmapImage>(result.image)
        val multiplier = DecodeUtils.computeSizeMultiplier(
            srcWidth = 9052,
            srcHeight = 4965,
            dstWidth = 9052,
            dstHeight = imageView.height,
            scale = Scale.FIT
        )
        val expectedWidth = (multiplier * 9052).roundToInt()
        val expectedHeight = (multiplier * 4965).roundToInt()
        assertTrue(image.bitmap.width in expectedWidth - 1..expectedWidth + 1)
        assertTrue(image.bitmap.height in expectedHeight - 1..expectedHeight + 1)
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
        val image = assertIs<BitmapImage>(result.image)
        val multiplier = DecodeUtils.computeSizeMultiplier(
            srcWidth = 9052,
            srcHeight = 4965,
            dstWidth = imageView.width,
            dstHeight = 4965,
            scale = Scale.FIT
        )
        val expectedWidth = (multiplier * 9052).roundToInt()
        val expectedHeight = (multiplier * 4965).roundToInt()
        assertTrue(image.bitmap.width in expectedWidth - 1..expectedWidth + 1)
        assertTrue(image.bitmap.height in expectedHeight - 1..expectedHeight + 1)
    }

    private suspend fun testEnqueue(data: Any, expectedSize: Size = Size(80, 100)) {
        val imageView = activityRule.scenario.activity.imageView
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER

        assertNull(imageView.drawable)

        suspendCancellableCoroutine { continuation ->
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
        val image = assertIs<BitmapImage>(result.image)
        assertEquals(expectedSize, image.bitmap.size)
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
        memoryCache[key] = MemoryCache.Value(bitmap.toDrawable(context).asCoilImage())
        return bitmap
    }

    companion object {
        private const val IMAGE = "normal.jpg"
    }
}
