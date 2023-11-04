package coil.memory

import coil.Image
import coil.PlatformContext
import coil.key.Keyer

/**
 * An LRU cache of [Image]s.
 */
interface MemoryCache {

    /** The current size of the cache in bytes. */
    val size: Long

    /** The maximum size of the cache in bytes. */
    val maxSize: Long

    /** The keys present in the cache. */
    val keys: Set<Key>

    /** Get the [Value] associated with [key]. */
    operator fun get(key: Key): Value?

    /** Set the [Value] associated with [key]. */
    operator fun set(key: Key, value: Value)

    /**
     * Remove the [Value] referenced by [key].
     *
     * @return 'true' if [key] was present in the cache. Else, return 'false'.
     */
    fun remove(key: Key): Boolean

    /** Remove the eldest entries until the cache's size is at or below [size]. */
    fun trimToSize(size: Long)

    /** Remove all values from the memory cache. */
    fun clear()

    /**
     * The cache key for a [Value] in the memory cache.
     *
     * @param key The value returned by [Keyer.key] (or a custom value).
     * @param extras Extra values that differentiate the associated
     *  cached value from other values with the same [key]. This map
     *  **must be** treated as immutable and should not be modified.
     */
    data class Key(
        val key: String,
        val extras: Map<String, String> = emptyMap(),
    )

    /**
     * The value for an [Image] in the memory cache.
     *
     * @param image The cached [Image].
     * @param extras Metadata for the [image]. This map **must be**
     *  treated as immutable and should not be modified.
     */
    data class Value(
        val image: Image,
        val extras: Map<String, Any> = emptyMap(),
    )

    class Builder(private val context: PlatformContext) {

        private var maxSizePercent = context.defaultMemoryCacheSizePercent()
        private var maxSizeBytes = 0L
        private var strongReferencesEnabled = true
        private var weakReferencesEnabled = true

        /**
         * Set the maximum size of the memory cache as a percentage of this application's
         * available memory.
         */
        fun maxSizePercent(percent: Double) = apply {
            require(percent in 0.0..1.0) { "percent must be in the range [0.0, 1.0]." }
            this.maxSizeBytes = 0
            this.maxSizePercent = percent
        }

        /**
         * Set the maximum size of the memory cache in bytes.
         */
        fun maxSizeBytes(size: Long) = apply {
            require(size >= 0) { "size must be >= 0." }
            this.maxSizePercent = 0.0
            this.maxSizeBytes = size
        }

        /**
         * Enables/disables strong reference tracking of values added to this memory cache.
         */
        fun strongReferencesEnabled(enable: Boolean) = apply {
            this.strongReferencesEnabled = enable
        }

        /**
         * Enables/disables weak reference tracking of values added to this memory cache.
         * Weak references do not contribute to the current size of the memory cache.
         * This ensures that if a [Value] hasn't been garbage collected yet it will be
         * returned from the memory cache.
         */
        fun weakReferencesEnabled(enable: Boolean) = apply {
            this.weakReferencesEnabled = enable
        }

        /**
         * Create a new [MemoryCache] instance.
         */
        fun build(): MemoryCache {
            val weakMemoryCache = if (weakReferencesEnabled) {
                RealWeakMemoryCache()
            } else {
                EmptyWeakMemoryCache()
            }
            val strongMemoryCache = if (strongReferencesEnabled) {
                val maxSize = if (maxSizePercent > 0) {
                    (maxSizePercent * context.totalAvailableMemoryBytes).toLong()
                } else {
                    maxSizeBytes
                }
                if (maxSize > 0) {
                    RealStrongMemoryCache(maxSize, weakMemoryCache)
                } else {
                    EmptyStrongMemoryCache(weakMemoryCache)
                }
            } else {
                EmptyStrongMemoryCache(weakMemoryCache)
            }
            return RealMemoryCache(strongMemoryCache, weakMemoryCache)
        }
    }
}
