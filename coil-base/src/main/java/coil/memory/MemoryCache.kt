package coil.memory

import android.content.ComponentCallbacks2
import android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
import android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
import android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
import android.graphics.Bitmap
import android.util.Log
import androidx.collection.LruCache
import coil.fetch.Fetcher
import coil.memory.MemoryCache.Key
import coil.memory.MemoryCache.Value
import coil.request.Parameters
import coil.size.Size
import coil.transform.Transformation
import coil.util.Logger
import coil.util.allocationByteCountCompat
import coil.util.log
import coil.util.mapIndices

/** An in-memory cache for [Bitmap]s. */
internal interface MemoryCache {

    companion object {
        operator fun invoke(
            weakMemoryCache: WeakMemoryCache,
            referenceCounter: BitmapReferenceCounter,
            maxSize: Int,
            logger: Logger?
        ): MemoryCache {
            return when {
                maxSize > 0 -> RealMemoryCache(weakMemoryCache, referenceCounter, maxSize, logger)
                weakMemoryCache is RealWeakMemoryCache -> ForwardingMemoryCache(weakMemoryCache)
                else -> EmptyMemoryCache
            }
        }
    }

    /** Get the value associated with [key]. */
    fun get(key: Key): Value?

    /** Set the value associated with [key]. */
    fun set(key: Key, bitmap: Bitmap, isSampled: Boolean)

    /** Return the **current size** of the memory cache in bytes. */
    fun size(): Int

    /** Return the **maximum size** of the memory cache in bytes. */
    fun maxSize(): Int

    /** Remove the value referenced by [key] from this cache if it is present. */
    fun invalidate(key: Key)

    /** Remove all values from this cache. */
    fun clearMemory()

    /** @see ComponentCallbacks2.onTrimMemory */
    fun trimMemory(level: Int)

    /** Cache key for [MemoryCache] and [WeakMemoryCache]. */
    class Key {

        /** The base component of the cache key. This is typically [Fetcher.key]. */
        val baseKey: String

        /** An ordered list of [Transformation.key]s. */
        val transformationKeys: List<String>

        /** The resolved size for the request. This is null if [transformationKeys] is empty. */
        val size: Size?

        /** @see Parameters.cacheKeys */
        val parameterKeys: Map<String, String>

        constructor(
            baseKey: String,
            parameters: Parameters = Parameters.EMPTY
        ) {
            this.baseKey = baseKey
            this.transformationKeys = emptyList()
            this.size = null
            this.parameterKeys = parameters.cacheKeys()
        }

        constructor(
            baseKey: String,
            transformations: List<Transformation>,
            size: Size,
            parameters: Parameters = Parameters.EMPTY
        ) {
            this.baseKey = baseKey
            if (transformations.isEmpty()) {
                this.transformationKeys = emptyList()
                this.size = null
            } else {
                this.transformationKeys = transformations.mapIndices { it.key() }
                this.size = size
            }
            this.parameterKeys = parameters.cacheKeys()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Key &&
                baseKey == other.baseKey &&
                transformationKeys == other.transformationKeys &&
                size == other.size &&
                parameterKeys == other.parameterKeys
        }

        override fun hashCode(): Int {
            var result = baseKey.hashCode()
            result = 31 * result + transformationKeys.hashCode()
            result = 31 * result + (size?.hashCode() ?: 0)
            result = 31 * result + parameterKeys.hashCode()
            return result
        }

        override fun toString(): String {
            return "MemoryCache.Key(baseKey='$baseKey', transformationKeys=$transformationKeys, size=$size, parameterKeys=$parameterKeys)"
        }
    }

    /** Cache value for [MemoryCache] and [WeakMemoryCache]. */
    interface Value {
        val bitmap: Bitmap
        val isSampled: Boolean
    }
}

/** A [MemoryCache] implementation that caches nothing. */
private object EmptyMemoryCache : MemoryCache {

    override fun get(key: Key): Value? = null

    override fun set(key: Key, bitmap: Bitmap, isSampled: Boolean) {}

    override fun size(): Int = 0

    override fun maxSize(): Int = 0

    override fun invalidate(key: Key) {}

    override fun clearMemory() {}

    override fun trimMemory(level: Int) {}
}

/** A [MemoryCache] implementation that caches nothing and delegates to [weakMemoryCache]. */
private class ForwardingMemoryCache(
    private val weakMemoryCache: WeakMemoryCache
) : MemoryCache {

    override fun get(key: Key) = weakMemoryCache.get(key)

    override fun set(key: Key, bitmap: Bitmap, isSampled: Boolean) {
        weakMemoryCache.set(key, bitmap, isSampled, bitmap.allocationByteCountCompat)
    }

    override fun size() = 0

    override fun maxSize() = 0

    override fun invalidate(key: Key) {}

    override fun clearMemory() {}

    override fun trimMemory(level: Int) {}
}

/** A [MemoryCache] implementation backed by an [LruCache]. */
private class RealMemoryCache(
    private val weakMemoryCache: WeakMemoryCache,
    private val referenceCounter: BitmapReferenceCounter,
    maxSize: Int,
    private val logger: Logger?
) : MemoryCache {

    companion object {
        private const val TAG = "RealMemoryCache"
    }

    private val cache = object : LruCache<Key, InternalValue>(maxSize) {
        override fun entryRemoved(
            evicted: Boolean,
            key: Key,
            oldValue: InternalValue,
            newValue: InternalValue?
        ) {
            val isPooled = referenceCounter.decrement(oldValue.bitmap)
            if (!isPooled) {
                // Add the bitmap to the WeakMemoryCache if it wasn't just added to the BitmapPool.
                weakMemoryCache.set(key, oldValue.bitmap, oldValue.isSampled, oldValue.size)
            }
        }

        override fun sizeOf(key: Key, value: InternalValue) = value.size
    }

    override fun get(key: Key) = cache.get(key) ?: weakMemoryCache.get(key)

    override fun set(key: Key, bitmap: Bitmap, isSampled: Boolean) {
        // If the bitmap is too big for the cache, don't even attempt to store it. Doing so will cause
        // the cache to be cleared. Instead just evict an existing element with the same key if it exists.
        val size = bitmap.allocationByteCountCompat
        if (size > maxSize()) {
            val previous = cache.remove(key)
            if (previous == null) {
                // If previous != null, the value was already added to the weak memory cache in LruCache.entryRemoved.
                weakMemoryCache.set(key, bitmap, isSampled, size)
            }
            return
        }

        referenceCounter.increment(bitmap)
        cache.put(key, InternalValue(bitmap, isSampled, size))
    }

    override fun size() = cache.size()

    override fun maxSize() = cache.maxSize()

    override fun invalidate(key: Key) {
        logger?.log(TAG, Log.VERBOSE) { "invalidate, key=$key" }
        cache.remove(key)
    }

    override fun clearMemory() {
        logger?.log(TAG, Log.VERBOSE) { "clearMemory" }
        cache.trimToSize(-1)
    }

    override fun trimMemory(level: Int) {
        logger?.log(TAG, Log.VERBOSE) { "trimMemory, level=$level" }
        if (level >= TRIM_MEMORY_BACKGROUND) {
            clearMemory()
        } else if (level in TRIM_MEMORY_RUNNING_LOW until TRIM_MEMORY_UI_HIDDEN) {
            cache.trimToSize(size() / 2)
        }
    }

    private class InternalValue(
        override val bitmap: Bitmap,
        override val isSampled: Boolean,
        val size: Int
    ) : Value
}
