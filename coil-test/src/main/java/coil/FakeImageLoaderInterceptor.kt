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
    private var defaultResultFactory: (Interceptor.Chain) -> ImageResult = {
        error("No interceptors handled this request and no default is set: ${it.request.data}")
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
            result = defaultResultFactory(chain)
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

    fun setDefault(resultFactory: (Interceptor.Chain) -> ImageResult) {
        defaultResultFactory = resultFactory
    }

    class RequestValue(
        val request: ImageRequest,
        val size: Size,
    )

    class ResultValue(
        val request: RequestValue,
        val result: ImageResult,
    )

    fun interface OptionalInterceptor {
        suspend fun intercept(chain: Interceptor.Chain): ImageResult?
    }
}

fun FakeImageLoaderInterceptor.set(data: Any, drawable: Drawable) = set(
    dataPredicate = { it == data },
    drawable = drawable,
)

fun FakeImageLoaderInterceptor.set(dataPredicate: (Any) -> Boolean, drawable: Drawable) = set(
    dataPredicate = dataPredicate,
    resultFactory = { imageResultOf(drawable, it.request) },
)

fun FakeImageLoaderInterceptor.set(
    dataPredicate: (Any) -> Boolean,
    resultFactory: (Interceptor.Chain) -> ImageResult,
) {
    addInterceptor { if (dataPredicate(it)) resultFactory(it) else null }
}

fun FakeImageLoaderInterceptor.setDefault(drawable: Drawable) {
    setDefault { imageResultOf(drawable, it.request) }
}

private fun imageResultOf(drawable: Drawable, request: ImageRequest) = SuccessResult(
    drawable = drawable,
    request = request,
    dataSource = DataSource.MEMORY,
)
