@file:Suppress("UNCHECKED_CAST", "unused")

package coil

import coil.decode.Decoder
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.intercept.Interceptor
import coil.key.Keyer
import coil.map.Mapper
import coil.request.Options
import coil.util.asImmutable
import coil.util.forEachIndices

/**
 * Registry for all the components that an [ImageLoader] uses to fulfil image requests.
 *
 * Use this class to register support for custom [Interceptor]s, [Mapper]s, [Keyer]s, [Fetcher]s, and [Decoder]s.
 */
class ComponentRegistry private constructor(
    val interceptors: List<Interceptor>,
    val mappers: List<Pair<Mapper<out Any, *>, Class<out Any>>>,
    val keyers: List<Pair<Keyer<out Any>, Class<out Any>>>,
    val fetcherFactories: List<Pair<Fetcher.Factory<out Any>, Class<out Any>>>,
    val decoderFactories: List<Decoder.Factory>
) {

    constructor() : this(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())

    /**
     * Convert [data] using the registered [mappers].
     */
    fun map(data: Any, options: Options): Any {
        var mappedData = data
        mappers.forEachIndices { (mapper, type) ->
            if (type.isAssignableFrom(mappedData::class.java)) {
                (mapper as Mapper<Any, *>).map(mappedData, options)?.let { mappedData = it }
            }
        }
        return mappedData
    }

    /**
     * Convert [data] to a string key using the registered [keyers].
     */
    fun key(data: Any, options: Options): String? {
        keyers.forEachIndices { (keyer, type) ->
            if (type.isAssignableFrom(data::class.java)) {
                (keyer as Keyer<Any>).key(data, options)?.let { return it }
            }
        }
        return null
    }

    /**
     * Create a new [Fetcher] using the registered [fetcherFactories].
     */
    @JvmOverloads
    fun newFetcher(
        data: Any,
        options: Options,
        imageLoader: ImageLoader,
        startIndex: Int = 0
    ): Pair<Fetcher, Int>? {
        for (index in startIndex until fetcherFactories.size) {
            val (factory, type) = fetcherFactories[index]
            if (type.isAssignableFrom(data::class.java)) {
                val fetcher = (factory as Fetcher.Factory<Any>).create(data, options, imageLoader)
                if (fetcher != null) return fetcher to index
            }
        }
        return null
    }

    /**
     * Create a new [Decoder] using the registered [decoderFactories].
     */
    @JvmOverloads
    fun newDecoder(
        result: SourceResult,
        options: Options,
        imageLoader: ImageLoader,
        startIndex: Int = 0
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
        private val mappers: MutableList<Pair<Mapper<out Any, *>, Class<out Any>>>
        private val keyers: MutableList<Pair<Keyer<out Any>, Class<out Any>>>
        private val fetcherFactories: MutableList<Pair<Fetcher.Factory<out Any>, Class<out Any>>>
        private val decoderFactories: MutableList<Decoder.Factory>

        constructor() {
            interceptors = mutableListOf()
            mappers = mutableListOf()
            keyers = mutableListOf()
            fetcherFactories = mutableListOf()
            decoderFactories = mutableListOf()
        }

        constructor(registry: ComponentRegistry) {
            interceptors = registry.interceptors.toMutableList()
            mappers = registry.mappers.toMutableList()
            keyers = registry.keyers.toMutableList()
            fetcherFactories = registry.fetcherFactories.toMutableList()
            decoderFactories = registry.decoderFactories.toMutableList()
        }

        /** Prepend an [Interceptor] to the beginning of the list. */
        fun addFirst(interceptor: Interceptor) = apply {
            interceptors.add(0, interceptor)
        }

        /** Append an [Interceptor] to the end of the list. */
        fun add(interceptor: Interceptor) = apply {
            interceptors += interceptor
        }

        /** Prepend a [Mapper] to the beginning of the list. */
        inline fun <reified T : Any> addFirst(mapper: Mapper<T, *>) = addFirst(mapper, T::class.java)

        /** Append a [Mapper] to the end of the list. */
        inline fun <reified T : Any> add(mapper: Mapper<T, *>) = add(mapper, T::class.java)

        /** Prepend a [Keyer] to the beginning of the list. */
        inline fun <reified T : Any> addFirst(keyer: Keyer<T>) = addFirst(keyer, T::class.java)

        /** Append a [Keyer] to the end of the list. */
        inline fun <reified T : Any> add(keyer: Keyer<T>) = add(keyer, T::class.java)

        /** Prepend a [Fetcher.Factory] to the beginning of the list. */
        inline fun <reified T : Any> addFirst(factory: Fetcher.Factory<T>) = addFirst(factory, T::class.java)

        /** Append a [Fetcher.Factory] to the end of the list. */
        inline fun <reified T : Any> add(factory: Fetcher.Factory<T>) = add(factory, T::class.java)

        /** Prepend a [Decoder.Factory] to the beginning of the list. */
        fun addFirst(factory: Decoder.Factory) = apply {
            decoderFactories.add(0, factory)
        }

        /** Append a [Decoder.Factory] to the end of the list. */
        fun add(factory: Decoder.Factory) = apply {
            decoderFactories += factory
        }

        @PublishedApi
        internal fun <T : Any> addFirst(mapper: Mapper<T, *>, type: Class<T>) = apply {
            mappers.add(0, mapper to type)
        }

        @PublishedApi
        internal fun <T : Any> add(mapper: Mapper<T, *>, type: Class<T>) = apply {
            mappers += mapper to type
        }

        @PublishedApi
        internal fun <T : Any> addFirst(keyer: Keyer<T>, type: Class<T>) = apply {
            keyers.add(0, keyer to type)
        }

        @PublishedApi
        internal fun <T : Any> add(keyer: Keyer<T>, type: Class<T>) = apply {
            keyers += keyer to type
        }

        @PublishedApi
        internal fun <T : Any> addFirst(factory: Fetcher.Factory<T>, type: Class<T>) = apply {
            fetcherFactories += factory to type
        }

        @PublishedApi
        internal fun <T : Any> add(factory: Fetcher.Factory<T>, type: Class<T>) = apply {
            fetcherFactories += factory to type
        }

        fun build(): ComponentRegistry {
            return ComponentRegistry(
                interceptors.toList().asImmutable(),
                mappers.toList().asImmutable(),
                keyers.toList().asImmutable(),
                fetcherFactories.toList().asImmutable(),
                decoderFactories.toList().asImmutable()
            )
        }
    }
}
