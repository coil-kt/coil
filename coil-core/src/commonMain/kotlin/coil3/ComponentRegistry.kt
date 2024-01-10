package coil3

import coil3.annotation.ExperimentalCoilApi
import coil3.decode.Decoder
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.intercept.Interceptor
import coil3.key.Keyer
import coil3.map.Mapper
import coil3.request.Options
import coil3.util.flatMapIndices
import coil3.util.forEachIndices
import coil3.util.toImmutableList
import kotlin.jvm.JvmOverloads
import kotlin.reflect.KClass

/**
 * Registry for all the components that an [ImageLoader] uses to fulfil image requests.
 *
 * Use this class to register support for custom [Interceptor]s, [Mapper]s, [Keyer]s,
 * [Fetcher]s, and [Decoder]s.
 */
class ComponentRegistry private constructor(
    val interceptors: List<Interceptor>,
    val mappers: List<Pair<Mapper<out Any, out Any>, KClass<out Any>>>,
    val keyers: List<Pair<Keyer<out Any>, KClass<out Any>>>,
    private var lazyFetcherFactories: List<() -> List<Pair<Fetcher.Factory<out Any>, KClass<out Any>>>>,
    private var lazyDecoderFactories: List<() -> List<Decoder.Factory>>,
) {

    constructor() : this(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())

    val fetcherFactories: List<Pair<Fetcher.Factory<out Any>, KClass<out Any>>> by lazy {
        lazyFetcherFactories.flatMapIndices { it() }.also { lazyFetcherFactories = emptyList() }
    }

    val decoderFactories: List<Decoder.Factory> by lazy {
        lazyDecoderFactories.flatMapIndices { it() }.also { lazyDecoderFactories = emptyList() }
    }

    /**
     * Convert [data] using the registered [mappers].
     *
     * @return The mapped data.
     */
    @Suppress("UNCHECKED_CAST")
    fun map(data: Any, options: Options): Any {
        var mappedData = data
        mappers.forEachIndices { (mapper, type) ->
            if (type.isInstance(mappedData)) {
                (mapper as Mapper<Any, *>).map(mappedData, options)?.let { mappedData = it }
            }
        }
        return mappedData
    }

    /**
     * Convert [data] to a string key using the registered [keyers].
     *
     * @return The cache key, or 'null' if [data] should not be cached.
     */
    @Suppress("UNCHECKED_CAST")
    fun key(data: Any, options: Options): String? {
        keyers.forEachIndices { (keyer, type) ->
            if (type.isInstance(data)) {
                (keyer as Keyer<Any>).key(data, options)?.let { return it }
            }
        }
        return null
    }

    /**
     * Create a new [Fetcher] using the registered [fetcherFactories].
     *
     * @return A [Pair] where the first element is the new [Fetcher] and the
     *  second element is the index of the factory in [fetcherFactories] that created it.
     *  Returns 'null' if a [Fetcher] cannot be created for [data].
     */
    @JvmOverloads
    @Suppress("UNCHECKED_CAST")
    fun newFetcher(
        data: Any,
        options: Options,
        imageLoader: ImageLoader,
        startIndex: Int = 0,
    ): Pair<Fetcher, Int>? {
        for (index in startIndex until fetcherFactories.size) {
            val (factory, type) = fetcherFactories[index]
            if (type.isInstance(data)) {
                val fetcher = (factory as Fetcher.Factory<Any>).create(data, options, imageLoader)
                if (fetcher != null) return fetcher to index
            }
        }
        return null
    }

    /**
     * Create a new [Decoder] using the registered [decoderFactories].
     *
     * @return A [Pair] where the first element is the new [Decoder] and the
     *  second element is the index of the factory in [decoderFactories] that created it.
     *  Returns 'null' if a [Decoder] cannot be created for [result].
     */
    @JvmOverloads
    fun newDecoder(
        result: SourceFetchResult,
        options: Options,
        imageLoader: ImageLoader,
        startIndex: Int = 0,
    ): Pair<Decoder, Int>? {
        for (index in startIndex until decoderFactories.size) {
            val factory = decoderFactories[index]
            val decoder = factory.create(result, options, imageLoader)
            if (decoder != null) return decoder to index
        }
        return null
    }

    fun newBuilder() = Builder(this)

    class Builder {

        internal val interceptors: MutableList<Interceptor>
        internal val mappers: MutableList<Pair<Mapper<out Any, *>, KClass<out Any>>>
        internal val keyers: MutableList<Pair<Keyer<out Any>, KClass<out Any>>>
        internal val lazyFetcherFactories: MutableList<() -> List<Pair<Fetcher.Factory<out Any>, KClass<out Any>>>>
        internal val lazyDecoderFactories: MutableList<() -> List<Decoder.Factory>>

        constructor() {
            interceptors = mutableListOf()
            mappers = mutableListOf()
            keyers = mutableListOf()
            lazyFetcherFactories = mutableListOf()
            lazyDecoderFactories = mutableListOf()
        }

        constructor(registry: ComponentRegistry) {
            interceptors = registry.interceptors.toMutableList()
            mappers = registry.mappers.toMutableList()
            keyers = registry.keyers.toMutableList()
            lazyFetcherFactories = registry.fetcherFactories.mapTo(mutableListOf()) { { listOf(it) } }
            lazyDecoderFactories = registry.decoderFactories.mapTo(mutableListOf()) { { listOf(it) } }
        }

        /** Append an [Interceptor] to the end of the list. */
        fun add(interceptor: Interceptor) = apply {
            interceptors += interceptor
        }

        /** Register a [Mapper]. */
        inline fun <reified T : Any> add(mapper: Mapper<T, *>) = add(mapper, T::class)

        /** Register a [Mapper]. */
        fun <T : Any> add(mapper: Mapper<T, *>, type: KClass<T>) = apply {
            mappers += mapper to type
        }

        /** Register a [Keyer]. */
        inline fun <reified T : Any> add(keyer: Keyer<T>) = add(keyer, T::class)

        /** Register a [Keyer]. */
        fun <T : Any> add(keyer: Keyer<T>, type: KClass<T>) = apply {
            keyers += keyer to type
        }

        /** Register a [Fetcher.Factory]. */
        inline fun <reified T : Any> add(factory: Fetcher.Factory<T>) = add(factory, T::class)

        /** Register a [Fetcher.Factory]. */
        fun <T : Any> add(factory: Fetcher.Factory<T>, type: KClass<T>) = apply {
            lazyFetcherFactories += { listOf(factory to type) }
        }

        /** Register a factory of [Fetcher.Factory]s. */
        @ExperimentalCoilApi
        fun addFetcherFactories(factory: () -> List<Pair<Fetcher.Factory<out Any>, KClass<out Any>>>) = apply {
            lazyFetcherFactories += factory
        }

        /** Register a [Decoder.Factory]. */
        fun add(factory: Decoder.Factory) = apply {
            lazyDecoderFactories += { listOf(factory) }
        }

        /** Register a factory of [Decoder.Factory]s. */
        @ExperimentalCoilApi
        fun addDecoderFactories(factory: () -> List<Decoder.Factory>) = apply {
            lazyDecoderFactories += factory
        }

        fun build(): ComponentRegistry {
            return ComponentRegistry(
                interceptors = interceptors.toImmutableList(),
                mappers = mappers.toImmutableList(),
                keyers = keyers.toImmutableList(),
                lazyFetcherFactories = lazyFetcherFactories.toImmutableList(),
                lazyDecoderFactories = lazyDecoderFactories.toImmutableList(),
            )
        }
    }
}
