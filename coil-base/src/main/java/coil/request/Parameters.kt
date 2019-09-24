package coil.request

import androidx.collection.arrayMapOf
import coil.decode.Decoder
import coil.fetch.Fetcher
import coil.util.toArrayMap

/** A map of generic values that can be used to pass custom data to [Fetcher]s and [Decoder]s. */
class Parameters private constructor(
    private val map: Map<String, Entry>
) : Map<String, Parameters.Entry> by map {

    companion object {
        @JvmField
        val EMPTY = Parameters()

        /** Create a new [Parameters] instance. */
        inline operator fun invoke(
            builder: Builder.() -> Unit = {}
        ): Parameters = Builder().apply(builder).build()
    }

    override fun equals(other: Any?) = map == other

    override fun hashCode() = map.hashCode()

    override fun toString() = map.toString()

    fun newBuilder() = Builder(this)

    /**
     * @param value The parameter's value.
     * @param cacheKey The parameter's cache key. If not null, this value will be added to a request's cache key.
     */
    data class Entry(
        val value: Any?,
        val cacheKey: String?
    )

    class Builder {

        private val map: MutableMap<String, Entry>

        constructor() {
            map = arrayMapOf()
        }

        constructor(parameters: Parameters) {
            map = parameters.map.toArrayMap()
        }

        /**
         * Set a parameter.
         *
         * @param key The parameter's key.
         * @param value The parameter's value.
         * @param cacheKey The parameter's cache key. If not null, this value will be added to a request's cache key.
         */
        @JvmOverloads
        fun set(key: String, value: Any?, cacheKey: String? = null) = apply {
            this.map[key] = Entry(value, cacheKey)
        }

        /**
         * Remove a parameter.
         */
        fun remove(key: String) = apply {
            this.map.remove(key)
        }

        fun build() = Parameters(map)
    }
}
