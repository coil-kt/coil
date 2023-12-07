package coil3.util

/**
 * A least recently used cache that evicts the eldest entry when the cache's current size
 * exceeds its max size.
 */
internal open class LruCache<K : Any, V : Any>(
    val maxSize: Long,
) {
    private val map = LruMutableMap<K, V>()

    var size: Long = 0
        get() {
            if (field == -1L) {
                field = recomputeSize()
            }
            return field
        }
        private set

    val keys: Set<K> get() = map.keys.toSet()

    init {
        require(maxSize > 0) { "maxSize <= 0" }
    }

    /** Implementations **must** return a consistent, non-negative value for the same entry. */
    open fun sizeOf(key: K, value: V): Long = 1L

    open fun entryRemoved(key: K, oldValue: V, newValue: V?) {}

    fun put(key: K, value: V): V? {
        val oldValue = map.put(key, value)
        size += safeSizeOf(key, value)

        if (oldValue != null) {
            size -= safeSizeOf(key, oldValue)
            entryRemoved(key, oldValue, value)
        }

        trimToSize(maxSize)

        return oldValue
    }

    operator fun get(key: K): V? {
        return map[key]
    }

    fun remove(key: K): V? {
        val oldValue = map.remove(key)
        if (oldValue != null) {
            size -= safeSizeOf(key, oldValue)
            entryRemoved(key, oldValue, null)
        }
        return oldValue
    }

    fun trimToSize(size: Long) {
        while (this.size > size) {
            if (map.isEmpty()) {
                if (this.size != 0L) {
                    error("sizeOf() is returning inconsistent values")
                }
                break
            }

            val (key, value) = map.entries.first()
            map.remove(key)
            this.size -= safeSizeOf(key, value)
            entryRemoved(key, value, null)
        }
    }

    fun clear() {
        // -1 evicts 0-sized entries.
        trimToSize(-1)
    }

    private fun recomputeSize(): Long {
        return map.entries.sumOf { (key, value) ->
            safeSizeOf(key, value)
        }
    }

    private fun safeSizeOf(key: K, value: V): Long {
        try {
            val size = sizeOf(key, value)
            check(size >= 0) { "sizeOf($key, $value) returned a negative value: $size" }
            return size
        } catch (e: Exception) {
            size = -1
            throw e
        }
    }
}
