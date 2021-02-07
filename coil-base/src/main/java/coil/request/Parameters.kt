@file:JvmName("ParametersKt")
@file:Suppress("NOTHING_TO_INLINE", "unused")

package coil.request

import coil.decode.Decoder
import coil.fetch.Fetcher
import coil.request.Parameters.Entry
import coil.util.mapNotNullValues

/** A map of generic values that can be used to pass custom data to [Fetcher]s and [Decoder]s. */
public class Parameters private constructor(
    private val map: Map<String, Entry>,
) : Iterable<Pair<String, Entry>> {

    public constructor() : this(emptyMap())

    /** Returns the number of parameters in this object. */
    public val size: Int @JvmName("size") get() = map.size

    /** Returns the value associated with [key] or null if [key] has no mapping. */
    public fun value(key: String): Any? = map[key]?.value

    /** Returns the cache key associated with [key] or null if [key] has no mapping. */
    public fun cacheKey(key: String): String? = map[key]?.cacheKey

    /** Returns the entry associated with [key] or null if [key] has no mapping. */
    public fun entry(key: String): Entry? = map[key]

    /** Returns true if this object has no parameters. */
    public fun isEmpty(): Boolean = map.isEmpty()

    /** Returns a map of keys to values. */
    public fun values(): Map<String, Any?> {
        return if (isEmpty()) {
            emptyMap()
        } else {
            map.mapValues { it.value.value }
        }
    }

    /** Returns a map of keys to non null cache keys. Parameters with a null cache key are filtered out. */
    public fun cacheKeys(): Map<String, String> {
        return if (isEmpty()) {
            emptyMap()
        } else {
            map.mapNotNullValues { it.value.cacheKey }
        }
    }

    /** Returns an [Iterator] over the entries in the [Parameters]. */
    public override operator fun iterator(): Iterator<Pair<String, Entry>> {
        return map.map { (key, value) -> key to value }.iterator()
    }

    public override fun equals(other: Any?): Boolean {
        return (this === other) || (other is Parameters && map == other.map)
    }

    override fun hashCode(): Int = map.hashCode()

    override fun toString(): String = "Parameters(map=$map)"

    public fun newBuilder(): Builder = Builder(this)

    public data class Entry(
        val value: Any?,
        val cacheKey: String?,
    )

    public class Builder {

        private val map: MutableMap<String, Entry>

        public constructor() {
            map = mutableMapOf()
        }

        public constructor(parameters: Parameters) {
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
        public fun set(key: String, value: Any?, cacheKey: String? = value?.toString()): Builder = apply {
            map[key] = Entry(value, cacheKey)
        }

        /**
         * Remove a parameter.
         *
         * @param key The parameter's key.
         */
        public fun remove(key: String): Builder = apply {
            map.remove(key)
        }

        /** Create a new [Parameters] instance. */
        public fun build(): Parameters = Parameters(map.toMap())
    }

    public companion object {
        @JvmField
        public val EMPTY: Parameters = Parameters()
    }
}

/** Returns the number of parameters in this object. */
public inline fun Parameters.count(): Int = size

/** Return true when the set contains elements. */
public inline fun Parameters.isNotEmpty(): Boolean = !isEmpty()

/** Returns the value associated with [key] or null if [key] has no mapping. */
public inline operator fun Parameters.get(key: String): Any? = value(key)
