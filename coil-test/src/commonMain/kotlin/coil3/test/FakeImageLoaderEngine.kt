@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package coil3.test

import coil3.Image
import coil3.ImageLoader
import coil3.annotation.Data
import coil3.intercept.Interceptor
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.size.Size
import coil3.test.FakeImageLoaderEngine.RequestTransformer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Create a new [FakeImageLoaderEngine] that returns [image] for all requests.
 */
fun FakeImageLoaderEngine(image: Image): FakeImageLoaderEngine {
    return FakeImageLoaderEngine.Builder()
        .default(image)
        .build()
}

/**
 * An [ImageLoader] interceptor that intercepts all incoming requests before they're fetched
 * and decoded by the image loader's real image engine. This class is useful for overriding
 * an image loader's responses in tests:
 *
 * ```
 * val engine = FakeImageLoaderEngine.Builder()
 *     .intercept("https://www.example.com/image.jpg", FakeImage())
 *     .intercept({ it is String && it.endsWith("test.png") }, FakeImage())
 *     .default(FakeImage(color = 0x0000FF))
 *     .build()
 * val imageLoader = ImageLoader.Builder(context)
 *     .components { add(engine) }
 *     .build()
 * ```
 */
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

    @Data
    class RequestValue(
        val request: ImageRequest,
        val size: Size,
    )

    @Data
    class ResultValue(
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
                error(
                    "No interceptors handled this request and no fallback is " +
                        "set: ${chain.request.data}",
                )
            }
            requestTransformer = defaultRequestTransformer()
        }

        constructor(engine: FakeImageLoaderEngine) {
            interceptors = engine.interceptors.toMutableList()
            defaultInterceptor = engine.defaultInterceptor
            requestTransformer = engine.requestTransformer
        }

        /**
         * Add an interceptor that will return [Image] if [data] is equal to an incoming
         * [ImageRequest]'s data.
         */
        fun intercept(
            data: Any,
            image: Image,
        ) = intercept(
            predicate = { it == data },
            image = image,
        )

        /**
         * Add an interceptor that will return [Image] if [predicate] returns `true`.
         */
        fun intercept(
            predicate: (data: Any) -> Boolean,
            image: Image,
        ) = intercept(
            predicate = predicate,
            interceptor = { imageResultOf(image, it.request) },
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
         * Set a default [Image] that will be returned if no [OptionalInterceptor]s handle the
         * request. If a default is not set, any requests not handled by an [OptionalInterceptor]
         * will throw an exception.
         */
        fun default(image: Image) = default { chain ->
            imageResultOf(image, chain.request)
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
                interceptors = interceptors.toList(),
                defaultInterceptor = defaultInterceptor,
                requestTransformer = requestTransformer,
            )
        }
    }
}

internal expect fun defaultRequestTransformer(): RequestTransformer
