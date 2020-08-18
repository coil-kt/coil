@file:OptIn(ExperimentalCoilApi::class)
@file:Suppress("DEPRECATION", "unused")

package coil

import coil.annotation.ExperimentalCoilApi
import coil.decode.Decoder
import coil.fetch.Fetcher
import coil.intercept.Interceptor
import coil.map.Mapper
import coil.util.MultiList
import coil.util.MultiMutableList

/**
 * Registry for all the components that an [ImageLoader] uses to fulfil image requests.
 *
 * Use this class to register support for custom [Interceptor]s, [Mapper]s, [Fetcher]s, and [Decoder]s.
 */
class ComponentRegistry private constructor(
    internal val interceptors: List<Interceptor>,
    internal val mappers: MultiList<Mapper<out Any, *>, Class<out Any>>,
    internal val fetchers: MultiList<Fetcher<out Any>, Class<out Any>>,
    internal val decoders: List<Decoder>
) {

    constructor() : this(emptyList(), emptyList(), emptyList(), emptyList())

    fun newBuilder() = Builder(this)

    class Builder {

        private val interceptors: MutableList<Interceptor>
        private val mappers: MultiMutableList<Mapper<out Any, *>, Class<out Any>>
        private val fetchers: MultiMutableList<Fetcher<out Any>, Class<out Any>>
        private val decoders: MutableList<Decoder>

        constructor() {
            interceptors = mutableListOf()
            mappers = mutableListOf()
            fetchers = mutableListOf()
            decoders = mutableListOf()
        }

        constructor(registry: ComponentRegistry) {
            interceptors = registry.interceptors.toMutableList()
            mappers = registry.mappers.toMutableList()
            fetchers = registry.fetchers.toMutableList()
            decoders = registry.decoders.toMutableList()
        }

        /** Register an [Interceptor]. */
        fun add(interceptor: Interceptor) = apply {
            interceptors += interceptor
        }

        /** Register a [Mapper]. */
        inline fun <reified T : Any> add(mapper: Mapper<T, *>) = add(mapper, T::class.java)

        @PublishedApi
        internal fun <T : Any> add(mapper: Mapper<T, *>, type: Class<T>) = apply {
            mappers += mapper to type
        }

        /** Register a [Fetcher]. */
        inline fun <reified T : Any> add(fetcher: Fetcher<T>) = add(fetcher, T::class.java)

        @PublishedApi
        internal fun <T : Any> add(fetcher: Fetcher<T>, type: Class<T>) = apply {
            fetchers += fetcher to type
        }

        /** Register a [Decoder]. */
        fun add(decoder: Decoder) = apply {
            decoders += decoder
        }

        fun build(): ComponentRegistry {
            return ComponentRegistry(
                interceptors.toList(),
                mappers.toList(),
                fetchers.toList(),
                decoders.toList()
            )
        }
    }
}
