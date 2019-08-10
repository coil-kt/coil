@file:Suppress("unused")

package coil

import coil.annotation.BuilderMarker
import coil.decode.Decoder
import coil.fetch.Fetcher
import coil.map.Mapper
import coil.map.MeasuredMapper
import coil.util.MultiList
import coil.util.MultiMutableList
import okio.BufferedSource

/**
 * Registry for all the components that an [ImageLoader] uses to fulfil image requests.
 *
 * Use this class to register support for custom [Mapper]s, [MeasuredMapper]s, [Fetcher]s, and [Decoder]s.
 */
class ComponentRegistry private constructor(
    private val mappers: MultiList<Class<*>, Mapper<*, *>>,
    private val measuredMappers: MultiList<Class<*>, MeasuredMapper<*, *>>,
    private val fetchers: MultiList<Class<*>, Fetcher<*>>,
    private val decoders: List<Decoder>
) {

    companion object {
        /**
         * Create a new [ComponentRegistry] instance.
         *
         * Example:
         * ```
         * val registry = ComponentRegistry {
         *     add(GifDecoder())
         * }
         * ```
         */
        inline operator fun invoke(
            builder: Builder.() -> Unit = {}
        ): ComponentRegistry = Builder().apply(builder).build()

        /** Create a new [ComponentRegistry] instance. */
        inline operator fun invoke(
            registry: ComponentRegistry,
            builder: Builder.() -> Unit = {}
        ): ComponentRegistry = Builder(registry).apply(builder).build()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getMapper(data: T): Mapper<T, *>? {
        val result = mappers.find { (type, converter) ->
            type.isAssignableFrom(data::class.java) && (converter as Mapper<Any, *>).handles(data)
        }
        return result?.second as Mapper<T, *>?
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getMeasuredMapper(data: T): MeasuredMapper<T, *>? {
        val result = measuredMappers.find { (type, converter) ->
            type.isAssignableFrom(data::class.java) && (converter as MeasuredMapper<Any, *>).handles(data)
        }
        return result?.second as MeasuredMapper<T, *>?
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> requireFetcher(data: T): Fetcher<T> {
        val result = fetchers.find { (type, loader) ->
            type.isAssignableFrom(data::class.java) && (loader as Fetcher<Any>).handles(data)
        }
        checkNotNull(result) { "Unable to fetch data. No fetcher supports: $data" }
        return result.second as Fetcher<T>
    }

    fun <T : Any> requireDecoder(
        data: T,
        source: BufferedSource,
        mimeType: String?
    ): Decoder {
        val decoder = decoders.find { it.handles(source, mimeType) }
        return checkNotNull(decoder) { "Unable to decode data. No decoder supports: $data" }
    }

    fun newBuilder(): Builder = Builder(this)

    @BuilderMarker
    class Builder {

        private val mappers: MultiMutableList<Class<*>, Mapper<*, *>>
        private val measuredMappers: MultiMutableList<Class<*>, MeasuredMapper<*, *>>
        private val fetchers: MultiMutableList<Class<*>, Fetcher<*>>
        private val decoders: MutableList<Decoder>

        constructor() {
            mappers = mutableListOf()
            measuredMappers = mutableListOf()
            fetchers = mutableListOf()
            decoders = mutableListOf()
        }

        constructor(registry: ComponentRegistry) {
            mappers = registry.mappers.toMutableList()
            measuredMappers = registry.measuredMappers.toMutableList()
            fetchers = registry.fetchers.toMutableList()
            decoders = registry.decoders.toMutableList()
        }

        /** Add a custom [Mapper]. */
        inline fun <reified T : Any> add(mapper: Mapper<T, *>) = add(T::class.java, mapper)

        @PublishedApi
        internal fun <T : Any> add(type: Class<T>, mapper: Mapper<T, *>) = apply {
            mappers += type to mapper
        }

        /** Add a custom [MeasuredMapper]. */
        inline fun <reified T : Any> add(measuredMapper: MeasuredMapper<T, *>) = add(T::class.java, measuredMapper)

        @PublishedApi
        internal fun <T : Any> add(type: Class<T>, measuredMapper: MeasuredMapper<T, *>) = apply {
            measuredMappers += type to measuredMapper
        }

        /** Add a custom [Fetcher]. */
        inline fun <reified T : Any> add(fetcher: Fetcher<T>) = add(T::class.java, fetcher)

        @PublishedApi
        internal fun <T : Any> add(type: Class<T>, fetcher: Fetcher<T>) = apply {
            fetchers += type to fetcher
        }

        /** Add a custom [Decoder]. */
        fun add(decoder: Decoder) = apply {
            decoders += decoder
        }

        fun build(): ComponentRegistry {
            return ComponentRegistry(
                mappers,
                measuredMappers,
                fetchers,
                decoders
            )
        }
    }
}
