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
    internal val mappers: MultiList<Class<out Any>, Mapper<out Any, *>>,
    internal val measuredMappers: MultiList<Class<out Any>, MeasuredMapper<out Any, *>>,
    internal val fetchers: MultiList<Class<out Any>, Fetcher<out Any>>,
    internal val decoders: List<Decoder>
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
        val result = mappers.find { (type, mapper) ->
            type.isAssignableFrom(data::class.java) && (mapper as Mapper<Any, *>).handles(data)
        }
        return result?.second as Mapper<T, *>?
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getMeasuredMapper(data: T): MeasuredMapper<T, *>? {
        val result = measuredMappers.find { (type, mapper) ->
            type.isAssignableFrom(data::class.java) && (mapper as MeasuredMapper<Any, *>).handles(data)
        }
        return result?.second as MeasuredMapper<T, *>?
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> requireFetcher(data: T): Fetcher<T> {
        val result = fetchers.find { (type, fetcher) ->
            type.isAssignableFrom(data::class.java) && (fetcher as Fetcher<Any>).handles(data)
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

        private val mappers: MultiMutableList<Class<out Any>, Mapper<out Any, *>>
        private val measuredMappers: MultiMutableList<Class<out Any>, MeasuredMapper<out Any, *>>
        private val fetchers: MultiMutableList<Class<out Any>, Fetcher<out Any>>
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

        /** Register a [Mapper]. */
        inline fun <reified T : Any> add(mapper: Mapper<T, *>) = add(T::class.java, mapper)

        @PublishedApi
        internal fun <T : Any> add(type: Class<T>, mapper: Mapper<T, *>) = apply {
            mappers += type to mapper
        }

        /** Register a [MeasuredMapper]. */
        inline fun <reified T : Any> add(measuredMapper: MeasuredMapper<T, *>) = add(T::class.java, measuredMapper)

        @PublishedApi
        internal fun <T : Any> add(type: Class<T>, measuredMapper: MeasuredMapper<T, *>) = apply {
            measuredMappers += type to measuredMapper
        }

        /** Register a [Fetcher]. */
        inline fun <reified T : Any> add(fetcher: Fetcher<T>) = add(T::class.java, fetcher)

        @PublishedApi
        internal fun <T : Any> add(type: Class<T>, fetcher: Fetcher<T>) = apply {
            fetchers += type to fetcher
        }

        /** Register a [Decoder]. */
        fun add(decoder: Decoder) = apply {
            decoders += decoder
        }

        fun build(): ComponentRegistry {
            return ComponentRegistry(
                mappers.toList(),
                measuredMappers.toList(),
                fetchers.toList(),
                decoders.toList()
            )
        }
    }
}
