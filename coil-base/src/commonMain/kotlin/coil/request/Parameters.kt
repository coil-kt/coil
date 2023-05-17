package coil.request

import coil.decode.Decoder
import coil.fetch.Fetcher
import coil.request.Parameters.Entry
import coil.util.mapNotNullValues
import coil.util.toImmutableMap
import okhttp3.Request

/**
 * A map of generic values that can be used to pass custom data to [Fetcher]s and [Decoder]s.
 *
 * Parameters are different from [Tags] as parameters are used to add extensions to [ImageRequest]
 * whereas [Tags] are used for custom user metadata. Parameters also are not attached to any OkHttp
 * [Request]s and also modify the request's memory cache key by default.
 */
class Parameters private constructor(
    private val entries: Map<String, Entry>
) : Iterable<Pair<String, Entry>> {

    constructor() : this(emptyMap())

    /** Returns the number of parameters in this object. */
    val size: Int @JvmName("size") get() = entries.size

    /** Returns the value associated with [key] or null if [key] has no mapping. */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> value(key: String): T? = entries[key]?.value as T?

    /** Returns the cache key associated with [key] or null if [key] has no mapping. */
    fun memoryCacheKey(key: String): String? = entries[key]?.memoryCacheKey

    /** Returns the entry associated with [key] or null if [key] has no mapping. */
    fun entry(key: String): Entry? = entries[key]

    /** Returns 'true' if this object has no parameters. */
    fun isEmpty(): Boolean = entries.isEmpty()

    /** Returns a map of keys to values. */
    fun values(): Map<String, Any?> {
        return if (isEmpty()) {
            emptyMap()
        } else {
            entries.mapValues { it.value.value }
        }
    }

    /** Returns a map of keys to non-null memory cache keys. Entries with a null keys are filtered. */
    fun memoryCacheKeys(): Map<String, String> {
        return if (isEmpty()) {
            emptyMap()
        } else {
            entries.mapNotNullValues { it.value.memoryCacheKey }
        }
    }

    /** Returns an [Iterator] over the entries in the [Parameters]. */
    override operator fun iterator(): Iterator<Pair<String, Entry>> {
        return entries.map { (key, value) -> key to value }.iterator()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is Parameters && entries == other.entries
    }

    override fun hashCode() = entries.hashCode()

    override fun toString() = "Parameters(entries=$entries)"

    fun newBuilder() = Builder(this)

    class Entry(
        val value: Any?,
        val memoryCacheKey: String?,
    ) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Entry &&
                value == other.value &&
                memoryCacheKey == other.memoryCacheKey
        }

        override fun hashCode(): Int {
            var result = value.hashCode()
            result = 31 * result + memoryCacheKey.hashCode()
            return result
        }

        override fun toString(): String {
            return "Entry(value=$value, memoryCacheKey=$memoryCacheKey)"
        }
    }

    class Builder {

        private val entries: MutableMap<String, Entry>

        constructor() {
            entries = mutableMapOf()
        }

        constructor(parameters: Parameters) {
            entries = parameters.entries.toMutableMap()
        }

        /**
         * Set a parameter.
         *
         * @param key The parameter's key.
         * @param value The parameter's value.
         * @param memoryCacheKey The parameter's memory cache key. If not
         *  null, this value will be added to a request's memory cache key.
         */
        @JvmOverloads
        fun set(key: String, value: Any?, memoryCacheKey: String? = value?.toString()) = apply {
            entries[key] = Entry(value, memoryCacheKey)
        }

        /**
         * Remove a parameter.
         *
         * @param key The parameter's key.
         */
        fun remove(key: String) = apply {
            entries.remove(key)
        }

        /** Create a new [Parameters] instance. */
        fun build() = Parameters(entries.toImmutableMap())
    }

    companion object {
        @JvmField val EMPTY = Parameters()
    }
}
