@file:JvmName("FakeImageLoaderEngines")

package coil.test

import android.graphics.drawable.Drawable
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.intercept.Interceptor
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.size.Size
import coil.test.FakeImageLoaderEngine.RequestTransformer
import coil.transition.Transition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * An [ImageLoader] interceptor that intercepts all incoming requests before they're fetched
 * and decoded by the image loader's real image engine. This class is useful for overriding
 * an image loader's responses in tests:
 *
 * ```
 * val engine = FakeImageLoaderEngine.Builder()
 *     .intercept("https://www.example.com/image.jpg", ColorDrawable(Color.RED))
 *     .intercept({ it is String && it.endsWith("test.png") }, ColorDrawable(Color.GREEN))
 *     .default(ColorDrawable(Color.BLUE))
 *     .build()
 * val imageLoader = ImageLoader.Builder(context)
 *     .components { add(engine) }
 *     .build()
 * ```
 */
@ExperimentalCoilApi
class FakeImageLoaderEngine private constructor(
    val interceptors: List<OptionalInterceptor>,
    val defaultInterceptor: Interceptor,
    val requestTransformer: RequestTransformer,
) : Interceptor {

    private val _requests = MutableSharedFlow<RequestValue>()
    private val _results = MutableSharedFlow<ResultValue>()

    /** Returns a [Flow] that emits when a request starts. */
    val requests: Flow<RequestValue> get() = _requests.asSharedFlow()

    /** Returns a [Flow] that emits when a request completes. */
    val results: Flow<ResultValue> get() = _results.asSharedFlow()

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = requestTransformer.transform(chain.request)
        val requestValue = RequestValue(request, chain.size)
        _requests.emit(requestValue)

        var result: ImageResult? = null
        for (interceptor in interceptors) {
            val newChain = chain.withRequest(request)
            result = interceptor.intercept(newChain)
            if (result != null) break
        }
        if (result == null) {
            result = defaultInterceptor.intercept(chain)
        }

        _results.emit(ResultValue(requestValue, result))
        return result
    }

    /**
     * Create a new [FakeImageLoaderEngine.Builder] with the same configuration.
     */
    fun newBuilder() = Builder(this)

    data class RequestValue(
        val request: ImageRequest,
        val size: Size,
    )

    data class ResultValue(
        val request: RequestValue,
        val result: ImageResult,
    )

    /**
     * An [Interceptor] that can either:
     *
     * - Return an [ImageResult] so no subsequent interceptors are called.
     * - Return `null` to delegate to the next [OptionalInterceptor] in the list.
     * - Optionally, call [Interceptor.Chain.proceed] to call through to the [ImageLoader]'s
     *   real interceptor chain. Typically, this will map, fetch, and decode the data using the
     *   image loader's real image engine.
     */
    fun interface OptionalInterceptor {
        suspend fun intercept(chain: Interceptor.Chain): ImageResult?
    }

    /**
     * A callback to support modifying an [ImageRequest] before it's handled by
     * the [OptionalInterceptor]s.
     */
    fun interface RequestTransformer {
        suspend fun transform(request: ImageRequest): ImageRequest
    }

    class Builder {

        private val interceptors: MutableList<OptionalInterceptor>
        private var defaultInterceptor: Interceptor
        private var requestTransformer: RequestTransformer

        constructor() {
            interceptors = mutableListOf()
            defaultInterceptor = Interceptor { chain ->
                error("No interceptors handled this request and no fallback is set: ${chain.request.data}")
            }
            requestTransformer = RequestTransformer { request ->
                request.newBuilder().transitionFactory(Transition.Factory.NONE).build()
            }
        }

        constructor(engine: FakeImageLoaderEngine) {
            interceptors = engine.interceptors.toMutableList()
            defaultInterceptor = engine.defaultInterceptor
            requestTransformer = engine.requestTransformer
        }

        /**
         * Add an interceptor that will return [Drawable] if [data] is equal to an incoming
         * [ImageRequest]'s data.
         */
        fun intercept(
            data: Any,
            drawable: Drawable,
        ) = intercept(
            predicate = { it == data },
            drawable = drawable,
        )

        /**
         * Add an interceptor that will return [Drawable] if [predicate] returns `true`.
         */
        fun intercept(
            predicate: (data: Any) -> Boolean,
            drawable: Drawable,
        ) = intercept(
            predicate = predicate,
            interceptor = { imageResultOf(drawable, it.request) },
        )

        /**
         * Add an interceptor that will call [interceptor] if [predicate] returns `true`.
         */
        fun intercept(
            predicate: (data: Any) -> Boolean,
            interceptor: OptionalInterceptor,
        ) = addInterceptor { chain ->
            if (predicate(chain.request.data)) interceptor.intercept(chain) else null
        }

        /**
         * Add an [OptionalInterceptor].
         */
        fun addInterceptor(interceptor: OptionalInterceptor) = apply {
            interceptors += interceptor
        }

        /**
         * Remove [interceptor] from the list of [OptionalInterceptor]s.
         */
        fun removeInterceptor(interceptor: OptionalInterceptor) = apply {
            interceptors -= interceptor
        }

        /**
         * Remove all [OptionalInterceptor]s.
         */
        fun clearInterceptors() = apply {
            interceptors.clear()
        }

        /**
         * Set a default [Drawable] that will be returned if no [OptionalInterceptor]s handle the
         * request. If a default is not set, any requests not handled by an [OptionalInterceptor]
         * will throw an exception.
         */
        fun default(drawable: Drawable) = default { chain ->
            imageResultOf(drawable, chain.request)
        }

        /**
         * Set the default [Interceptor] that will be called if no [OptionalInterceptor]s
         * handle the request. If a default is not set, any requests not handled by an
         * [OptionalInterceptor] will throw an exception.
         */
        fun default(interceptor: Interceptor) = apply {
            defaultInterceptor = interceptor
        }

        /**
         * Set a callback to modify an incoming [ImageRequest] before it's handled by
         * the [interceptors].
         *
         * By default, [FakeImageLoaderEngine] uses this callback to clear an [ImageRequest]'s
         * transition and setting this callback replaces that behaviour.
         */
        fun requestTransformer(transformer: RequestTransformer) = apply {
            requestTransformer = transformer
        }

        /**
         * Create a new [FakeImageLoaderEngine] instance.
         */
        fun build(): FakeImageLoaderEngine {
            return FakeImageLoaderEngine(
                interceptors = interceptors.toImmutableList(),
                defaultInterceptor = defaultInterceptor,
                requestTransformer = requestTransformer,
            )
        }
    }
}

/**
 * Create a new [FakeImageLoaderEngine] that returns [Drawable] for all requests.
 */
@JvmName("create")
fun FakeImageLoaderEngine(drawable: Drawable): FakeImageLoaderEngine {
    return FakeImageLoaderEngine.Builder()
        .default(drawable)
        .build()
}
