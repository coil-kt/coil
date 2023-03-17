package coil

import android.graphics.drawable.Drawable
import coil.annotation.ExperimentalCoilApi
import coil.decode.DataSource
import coil.intercept.Interceptor
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.SuccessResult
import coil.size.Size
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@ExperimentalCoilApi
class FakeImageLoaderInterceptor : Interceptor {

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

    fun interface OptionalInterceptor {
        suspend fun intercept(chain: Interceptor.Chain): ImageResult?
    }
}

fun FakeImageLoaderInterceptor.set(data: Any, drawable: Drawable) = set(
    predicate = { it == data },
    drawable = drawable,
)

fun FakeImageLoaderInterceptor.set(predicate: (Any) -> Boolean, drawable: Drawable) = set(
    predicate = predicate,
    interceptor = { imageResultOf(drawable, it.request) },
)

fun FakeImageLoaderInterceptor.set(
    predicate: (Any) -> Boolean,
    interceptor: Interceptor,
) {
    addInterceptor { if (predicate(it)) interceptor.intercept(it) else null }
}

fun FakeImageLoaderInterceptor.setFallback(drawable: Drawable) {
    setFallback { imageResultOf(drawable, it.request) }
}

private fun imageResultOf(drawable: Drawable, request: ImageRequest) = SuccessResult(
    drawable = drawable,
    request = request,
    dataSource = DataSource.MEMORY,
)
