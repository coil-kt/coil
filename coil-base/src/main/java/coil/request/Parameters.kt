@file:Suppress("unused")

package coil.request

import coil.decode.Decoder
import coil.fetch.Fetcher
import coil.util.sortedMapOf
import java.util.SortedMap

/** A map of generic values that can be used to pass custom data to [Fetcher]s and [Decoder]s. */
class Parameters private constructor(
    private val map: SortedMap<String, Entry>
) : Iterable<Pair<String, Parameters.Entry>> {

    constructor() : this(sortedMapOf())

    companion object {
        @JvmField
        val EMPTY = Parameters()

        /** Create a new [Parameters] instance. */
        @Deprecated(
            message = "Use Parameters.Builder to create new instances.",
            replaceWith = ReplaceWith("Parameters.Builder().apply(builder).build()")
        )
        inline operator fun invoke(
            builder: Builder.() -> Unit = {}
        ): Parameters = Builder().apply(builder).build()
    }

    /** Returns the value associated with [key] or null if [key] has no mapping. */
    fun value(key: String): Any? = map[key]?.value

    /** Returns the cache key associated with [key] or null if [key] has no mapping. */
    fun cacheKey(key: String): String? = map[key]?.cacheKey

    /** Returns the entry associated with [key] or null if [key] has no mapping. */
    fun entry(key: String): Entry? = map[key]

    /** Returns the number of parameters in this object. */
    fun count(): Int = map.count()

    /** Returns true if this object has no parameters. */
    fun isEmpty(): Boolean = map.isEmpty()

    /** Returns an [Iterator] over the entries in the [Parameters]. Iteration order is deterministic. */
    override operator fun iterator(): Iterator<Pair<String, Entry>> {
        return map.map { (key, value) -> key to value }.iterator()
    }

    override fun equals(other: Any?) = map == other

    override fun hashCode() = map.hashCode()

    override fun toString() = map.toString()

    fun newBuilder() = Builder(this)

    data class Entry(
        val value: Any?,
        val cacheKey: String?
    )

    class Builder {

        private val map: SortedMap<String, Entry>

        constructor() {
            map = sortedMapOf()
        }

        constructor(parameters: Parameters) {
            map = parameters.map.toSortedMap()
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
         *
         * @param key The parameter's key.
         */
        fun remove(key: String) = apply {
            this.map.remove(key)
        }

        /** Create a new [Parameters] instance. */
        fun build() = Parameters(map.toSortedMap())
    }
}
