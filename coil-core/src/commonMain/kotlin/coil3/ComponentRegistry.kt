package coil3

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
        private val interceptors: MutableList<Interceptor>
        private val mappers: MutableList<Pair<Mapper<out Any, *>, KClass<out Any>>>
        private val keyers: MutableList<Pair<Keyer<out Any>, KClass<out Any>>>
        private val lazyFetcherFactories: MutableList<() -> List<Pair<Fetcher.Factory<out Any>, KClass<out Any>>>>
        private val lazyDecoderFactories: MutableList<() -> List<Decoder.Factory>>

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

        /** Add an [Interceptor] at [index] in the list. */
        fun add(index: Int, interceptor: Interceptor) = apply {
            interceptors.add(index, interceptor)
        }

        /** Append a [Mapper] to the end of the list. */
        inline fun <reified T : Any> add(mapper: Mapper<T, *>) = add(mapper, T::class)

        /** Append a [Mapper] to the end of the list. */
        fun <T : Any> add(mapper: Mapper<T, *>, type: KClass<T>) = apply {
            mappers += mapper to type
        }

        /** Add a [Mapper] at [index] in the list. */
        inline fun <reified T : Any> add(index: Int, mapper: Mapper<T, *>) = add(index, mapper, T::class)

        /** Add a [Mapper] at [index] in the list. */
        fun <T : Any> add(index: Int, mapper: Mapper<T, *>, type: KClass<T>) = apply {
            mappers.add(index, mapper to type)
        }

        /** Append a [Keyer] to the end of the list. */
        inline fun <reified T : Any> add(keyer: Keyer<T>) = add(keyer, T::class)

        /** Append a [Keyer] to the end of the list. */
        fun <T : Any> add(keyer: Keyer<T>, type: KClass<T>) = apply {
            keyers += keyer to type
        }

        /** Add a [Keyer] at [index] in the list. */
        inline fun <reified T : Any> add(index: Int, keyer: Keyer<T>) = add(index, keyer, T::class)

        /** Add a [Keyer] at [index] in the list. */
        fun <T : Any> add(index: Int, keyer: Keyer<T>, type: KClass<T>) = apply {
            keyers.add(index, keyer to type)
        }

        /** Append a [Fetcher.Factory] to the end of the list. */
        inline fun <reified T : Any> add(factory: Fetcher.Factory<T>) = add(factory, T::class)

        /** Append a [Fetcher.Factory] to the end of the list. */
        fun <T : Any> add(factory: Fetcher.Factory<T>, type: KClass<T>) =
            addFetcherFactories { listOf(factory to type) }

        /** Add a [Fetcher.Factory] at [index] in the list. */
        inline fun <reified T : Any> add(index: Int, factory: Fetcher.Factory<T>) = add(index, factory, T::class)

        /** Add a [Fetcher.Factory] at [index] in the list. */
        fun <T : Any> add(index: Int, factory: Fetcher.Factory<T>, type: KClass<T>) =
            addFetcherFactories(index) { listOf(factory to type) }

        /** Append a factory of [Fetcher.Factory]s to the end of the list. */
        fun addFetcherFactories(factory: () -> List<Pair<Fetcher.Factory<out Any>, KClass<out Any>>>) = apply {
            lazyFetcherFactories += factory
        }

        /** Add a factory of [Fetcher.Factory]s at [index] in the list. */
        fun addFetcherFactories(index: Int, factory: () -> List<Pair<Fetcher.Factory<out Any>, KClass<out Any>>>) = apply {
            lazyFetcherFactories.add(index, factory)
        }

        /** Append a [Decoder.Factory] to the end of the list. */
        fun add(factory: Decoder.Factory) =
            addDecoderFactories { listOf(factory) }

        /** Add a [Decoder.Factory] at [index] in the list. */
        fun add(index: Int, factory: Decoder.Factory) =
            addDecoderFactories(index) { listOf(factory) }

        /** Append a factory of [Decoder.Factory]s to the end of the list. */
        fun addDecoderFactories(factory: () -> List<Decoder.Factory>) = apply {
            lazyDecoderFactories += factory
        }

        /** Add a factory of [Decoder.Factory]s at [index] in the list. */
        fun addDecoderFactories(index: Int, factory: () -> List<Decoder.Factory>) = apply {
            lazyDecoderFactories.add(index, factory)
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
