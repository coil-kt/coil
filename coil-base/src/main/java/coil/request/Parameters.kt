@file:JvmName("ParametersKt")
@file:Suppress("NOTHING_TO_INLINE", "unused")

package coil.request

import coil.decode.Decoder
import coil.fetch.Fetcher
import coil.request.Parameters.Entry
import coil.util.mapNotNullValues

/** A map of generic values that can be used to pass custom data to [Fetcher]s and [Decoder]s. */
class Parameters private constructor(
    private val map: Map<String, Entry>
) : Iterable<Pair<String, Entry>> {

    constructor() : this(emptyMap())

    /** Returns the number of parameters in this object. */
    val size: Int @JvmName("size") get() = map.size

    /** Returns the value associated with [key] or null if [key] has no mapping. */
    fun value(key: String): Any? = map[key]?.value

    /** Returns the cache key associated with [key] or null if [key] has no mapping. */
    fun cacheKey(key: String): String? = map[key]?.cacheKey

    /** Returns the entry associated with [key] or null if [key] has no mapping. */
    fun entry(key: String): Entry? = map[key]

    /** Returns true if this object has no parameters. */
    fun isEmpty(): Boolean = map.isEmpty()

    /** Returns a map of keys to values. */
    fun values(): Map<String, Any?> {
        return if (isEmpty()) {
            emptyMap()
        } else {
            map.mapValues { it.value.value }
        }
    }

    /** Returns a map of keys to non null cache keys. Parameters with a null cache key are filtered out. */
    fun cacheKeys(): Map<String, String> {
        return if (isEmpty()) {
            emptyMap()
        } else {
            map.mapNotNullValues { it.value.cacheKey }
        }
    }

    /** Returns an [Iterator] over the entries in the [Parameters]. */
    override operator fun iterator(): Iterator<Pair<String, Entry>> {
        return map.map { (key, value) -> key to value }.iterator()
    }

    override fun equals(other: Any?): Boolean {
        return (this === other) || (other is Parameters && map == other.map)
    }

    override fun hashCode() = map.hashCode()

    override fun toString() = "Parameters(map=$map)"

    fun newBuilder() = Builder(this)

    data class Entry(
        val value: Any?,
        val cacheKey: String?
    )

    class Builder {

        private val map: MutableMap<String, Entry>

        constructor() {
            map = mutableMapOf()
        }

        constructor(parameters: Parameters) {
            map = parameters.map.toMutableMap()
        }

        /**
         * Set a parameter.
         *
         * @param key The parameter's key.
         * @param value The parameter's value.
         * @param cacheKey The parameter's cache key. If not null, this value will be added to a request's cache key.
         */
        @JvmOverloads
        fun set(key: String, value: Any?, cacheKey: String? = value?.toString()) = apply {
            map[key] = Entry(value, cacheKey)
        }

        /**
         * Remove a parameter.
         *
         * @param key The parameter's key.
         */
        fun remove(key: String) = apply {
            map.remove(key)
        }

        /** Create a new [Parameters] instance. */
        fun build() = Parameters(map.toMap())
    }

    companion object {
        @JvmField val EMPTY = Parameters()
    }
}

/** Returns the number of parameters in this object. */
inline fun Parameters.count(): Int = size

/** Return true when the set contains elements. */
inline fun Parameters.isNotEmpty(): Boolean = !isEmpty()

/** Returns the value associated with [key] or null if [key] has no mapping. */
inline operator fun Parameters.get(key: String): Any? = value(key)
