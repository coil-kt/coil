@file:JvmName("FakeImageLoaderInterceptors")

package coil.test

import android.graphics.drawable.Drawable
import coil.test.FakeImageLoaderEngine.OptionalInterceptor
import coil.annotation.ExperimentalCoilApi
import coil.intercept.Interceptor
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.size.Size
import coil.ImageLoader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * An [ImageLoader] interceptor that intercepts all incoming requests before they're fetched
 * and decoded by the image loader's real image engine. This class is useful for overriding
 * an image loader's responses in tests:
 *
 * ```
 * val engine = FakeImageLoaderEngine()
 * engine.set("https://example.com/image.jpg", testDrawable)
 * engine.set({ it is String && it.endsWith("test.png") }, testDrawable)
 * engine.setFallback(ColorDrawable(Color.BLACK))
 *
 * val imageLoader = ImageLoader.Builder(context)
 *     .components { add(engine) }
 *     .build()
 * Coil.setImageLoader(imageLoader)
 * ```
 */
@ExperimentalCoilApi
class FakeImageLoaderEngine : Interceptor {

    private val _interceptors = mutableListOf<OptionalInterceptor>()
    private val _requests = MutableSharedFlow<RequestValue>()
    private val _results = MutableSharedFlow<ResultValue>()
    private var fallbackInterceptor = Interceptor { chain ->
        error("No interceptors handled this request and no fallback is set: ${chain.request.data}")
    }

    /** Returns the list of [OptionalInterceptor]s. */
    val interceptors: List<OptionalInterceptor> get() = _interceptors.toList()

    /** Returns a [Flow] that emits when a request starts. */
    val requests: Flow<RequestValue> get() = _requests.asSharedFlow()

    /** Returns a [Flow] that emits when a request completes. */
    val results: Flow<ResultValue> get() = _results.asSharedFlow()

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = RequestValue(chain.request, chain.size)
        _requests.emit(request)

        var result: ImageResult? = null
        for (interceptor in _interceptors) {
            result = interceptor.intercept(chain)
            if (result != null) break
        }
        if (result == null) {
            result = fallbackInterceptor.intercept(chain)
        }

        _results.emit(ResultValue(request, result))
        return result
    }

    fun addInterceptor(interceptor: OptionalInterceptor) {
        _interceptors += interceptor
    }

    fun addInterceptor(index: Int, interceptor: OptionalInterceptor) {
        _interceptors.add(index, interceptor)
    }

    fun removeInterceptor(interceptor: OptionalInterceptor) {
        _interceptors -= interceptor
    }

    /**
     * Set the fallback [Interceptor] that will be called if no [OptionalInterceptor]s
     * handle the request. If a fallback is not set, any requests not handled by an
     * [OptionalInterceptor] will throw an exception.
     */
    fun setFallback(interceptor: Interceptor) {
        fallbackInterceptor = interceptor
    }

    data class RequestValue(
        val request: ImageRequest,
        val size: Size,
    )

    data class ResultValue(
        val request: RequestValue,
        val result: ImageResult,
    )

    /**
     * An [Interceptor] that supports returning 'null' to delegate to the next
     * [OptionalInterceptor] in the list.
     */
    fun interface OptionalInterceptor {
        suspend fun intercept(chain: Interceptor.Chain): ImageResult?
    }
}

/**
 * Set a [Drawable] that will be returned if [data] equals an incoming [ImageRequest]'s data.
 */
fun FakeImageLoaderEngine.set(
    data: Any,
    drawable: Drawable,
) = set(
    predicate = { it == data },
    drawable = drawable,
)

/**
 * Set a [Drawable] that will be returned if [predicate] returns `true`.
 */
fun FakeImageLoaderEngine.set(
    predicate: (data: Any) -> Boolean,
    drawable: Drawable,
) = set(
    predicate = predicate,
    interceptor = { imageResultOf(drawable, it.request) },
)

/**
 * Set an [Interceptor] that will be called if [predicate] returns `true`.
 */
fun FakeImageLoaderEngine.set(
    predicate: (data: Any) -> Boolean,
    interceptor: Interceptor,
) = addInterceptor { chain ->
    if (predicate(chain.request.data)) interceptor.intercept(chain) else null
}

/**
 * Set a fallback [Drawable] that will be returned if no [OptionalInterceptor]s handle the request.
 * If a fallback is not set, any requests not handled by an [OptionalInterceptor] will throw an
 * exception.
 */
fun FakeImageLoaderEngine.setFallback(drawable: Drawable) {
    setFallback { imageResultOf(drawable, it.request) }
}
