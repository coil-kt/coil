package coil

import android.content.ContentResolver.SCHEME_ANDROID_RESOURCE
import android.content.ContentResolver.SCHEME_CONTENT
import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import coil.annotation.ExperimentalCoilApi
import coil.base.test.R
import coil.bitmappool.BitmapPool
import coil.decode.BitmapFactoryDecoder
import coil.decode.DataSource
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.decode.Options
import coil.fetch.AssetUriFetcher.Companion.ASSET_FILE_PATH_ROOT
import coil.fetch.DrawableResult
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.GetRequest
import coil.request.LoadRequest
import coil.request.NullRequestDataException
import coil.request.SuccessResult
import coil.size.PixelSize
import coil.size.Size
import coil.transform.CircleCropTransformation
import coil.util.Utils
import coil.util.createGetRequest
import coil.util.createMockWebServer
import coil.util.createOptions
import coil.util.getDrawableCompat
import coil.util.size
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Cache
import okhttp3.mockwebserver.MockWebServer
import okio.BufferedSource
import okio.buffer
import okio.sink
import okio.source
import org.junit.After
import org.junit.Before
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

/**
 * Integration tests for [RealImageLoader].
 */
@OptIn(ExperimentalCoilApi::class)
class RealImageLoaderIntegrationTest {

    companion object {
        private const val IMAGE_NAME = "normal.jpg"
        private const val IMAGE_SIZE = 443291L
    }

    private lateinit var context: Context
    private lateinit var server: MockWebServer
    private lateinit var imageLoader: RealImageLoader

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        server = createMockWebServer(context, IMAGE_NAME, IMAGE_NAME)
        imageLoader = ImageLoader(context) as RealImageLoader
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
        testLoad(data)
        testGet(data)
    }

    @Test
    fun httpUri() {
        val data = server.url(IMAGE_NAME).toString().toUri()
        testLoad(data)
        testGet(data)
    }

    @Test
    fun httpUrl() {
        val data = server.url(IMAGE_NAME)
        testLoad(data)
        testGet(data)
    }

    @Test
    fun resourceInt() {
        val data = R.drawable.normal
        testLoad(data)
        testGet(data)
    }

    @Test
    fun resourceIntVector() {
        val data = R.drawable.ic_android
        testLoad(data, PixelSize(100, 100))
        testGet(data, PixelSize(100, 100))
    }

    @Test
    fun resourceUriInt() {
        val data = "$SCHEME_ANDROID_RESOURCE://${context.packageName}/${R.drawable.normal}".toUri()
        testLoad(data)
        testGet(data)
    }

    @Test
    fun resourceUriIntVector() {
        val data = "$SCHEME_ANDROID_RESOURCE://${context.packageName}/${R.drawable.ic_android}".toUri()
        testLoad(data, PixelSize(100, 100))
        testGet(data, PixelSize(100, 100))
    }

    @Test
    fun resourceUriString() {
        val data = "$SCHEME_ANDROID_RESOURCE://${context.packageName}/drawable/normal".toUri()
        testLoad(data)
        testGet(data)
    }

    @Test
    fun resourceUriStringVector() {
        val data = "$SCHEME_ANDROID_RESOURCE://${context.packageName}/drawable/ic_android".toUri()
        testLoad(data, PixelSize(100, 100))
        testGet(data, PixelSize(100, 100))
    }

    @Test
    fun file() {
        val data = copyNormalImageAssetToCacheDir()
        testLoad(data)
        testGet(data)
    }

    @Test
    fun fileUri() {
        val data = copyNormalImageAssetToCacheDir().toUri()
        testLoad(data)
        testGet(data)
    }

    @Test
    fun assetUri() {
        val data = "$SCHEME_FILE:///$ASSET_FILE_PATH_ROOT/exif/large_metadata.jpg".toUri()
        testLoad(data, PixelSize(75, 100))
        testGet(data, PixelSize(100, 133))
    }

    @Test
    fun contentUri() {
        val data = "$SCHEME_CONTENT://coil/$IMAGE_NAME".toUri()
        testLoad(data)
        testGet(data)
    }

    @Test
    fun drawable() {
        val data = context.getDrawableCompat(R.drawable.normal)
        val expectedSize = PixelSize(1080, 1350)
        testLoad(data, expectedSize)
        testGet(data, expectedSize)
    }

    @Test
    fun bitmap() {
        val data = (context.getDrawableCompat(R.drawable.normal) as BitmapDrawable).bitmap
        val expectedSize = PixelSize(1080, 1350)
        testLoad(data, expectedSize)
        testGet(data, expectedSize)
    }

    // endregion

    @Test
    fun unsupportedDataThrows() {
        val data = Any()
        assertFailsWith<IllegalStateException> { testLoad(data) }
        assertFailsWith<IllegalStateException> { testGet(data) }
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
                val request = LoadRequest.Builder(context)
                    .data(url)
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .listener(
                        onSuccess = { _, _ -> continuation.resume(Unit) },
                        onError = { _, throwable -> continuation.resumeWithException(throwable) },
                        onCancel = { continuation.resumeWithException(CancellationException()) }
                    )
                    .build()
                imageLoader.execute(request)
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
            val request = GetRequest.Builder(context)
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
    fun applyTransformations_transformationsConvertDrawableToBitmap() {
        val drawable = ColorDrawable(Color.BLACK)
        val size = PixelSize(100, 100)
        val result = runBlocking {
            imageLoader.applyTransformations(
                scope = this,
                result = DrawableResult(
                    drawable = drawable,
                    isSampled = false,
                    dataSource = DataSource.MEMORY
                ),
                request = createGetRequest(context) { transformations(CircleCropTransformation()) },
                size = size,
                options = createOptions(),
                eventListener = EventListener.NONE
            )
        }

        val resultDrawable = result.drawable
        assertTrue(resultDrawable is BitmapDrawable)
        assertEquals(resultDrawable.bitmap.size, size)
    }

    @Test
    fun applyTransformations_emptyTransformationsDoesNotConvertDrawable() {
        val drawable = ColorDrawable(Color.BLACK)
        val size = PixelSize(100, 100)
        val result = runBlocking {
            imageLoader.applyTransformations(
                scope = this,
                result = DrawableResult(
                    drawable = drawable,
                    isSampled = false,
                    dataSource = DataSource.MEMORY
                ),
                request = createGetRequest(context) { transformations(emptyList()) },
                size = size,
                options = createOptions(),
                eventListener = EventListener.NONE
            )
        }

        assertSame(drawable, result.drawable)
    }

    @Test
    fun nullRequestDataShowsFallbackDrawable() {
        val error = ColorDrawable(Color.BLUE)
        val fallback = ColorDrawable(Color.BLACK)

        runBlocking {
            suspendCancellableCoroutine<Unit> { continuation ->
                var hasCalledTargetOnError = false

                val request = LoadRequest.Builder(context)
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
                imageLoader.execute(request)
            }
        }
    }

    private fun testLoad(data: Any, expectedSize: PixelSize = PixelSize(80, 100)) {
        val imageView = ImageView(context)
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER

        assertNull(imageView.drawable)

        runBlocking {
            suspendCancellableCoroutine<Unit> { continuation ->
                val request = LoadRequest.Builder(context)
                    .data(data)
                    .target(imageView)
                    .size(100, 100)
                    .listener(
                        onSuccess = { _, _ -> continuation.resume(Unit) },
                        onError = { _, throwable -> continuation.resumeWithException(throwable) },
                        onCancel = { continuation.resumeWithException(CancellationException()) }
                    )
                    .build()
                imageLoader.execute(request)
            }
        }

        val drawable = imageView.drawable
        assertTrue(drawable is BitmapDrawable)
        assertEquals(expectedSize, drawable.bitmap.size)
    }

    private fun testGet(data: Any, expectedSize: PixelSize = PixelSize(100, 125)) {
        val result = runBlocking {
            val request = GetRequest.Builder(context)
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
}
