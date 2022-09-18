@file:JvmName("FakeMemoryCaches")

package coil.memory

import android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
import android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
import android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
import androidx.collection.LruCache
import coil.allocationByteCountCompat
import coil.annotation.ExperimentalCoilApi
import coil.memory.MemoryCache.Key
import coil.memory.MemoryCache.Value
import coil.toImmutableMap
import coil.toImmutableSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@ExperimentalCoilApi
class FakeMemoryCache private constructor(
    override val maxSize: Int
) : MemoryCache {

    private val cache = object : LruCache<Key, Value>(maxSize) {
        override fun sizeOf(key: Key, value: Value): Int {
            return value.bitmap.allocationByteCountCompat
        }
        override fun entryRemoved(evicted: Boolean, key: Key, oldValue: Value, newValue: Value?) {
            if (evicted) _evicts.tryEmit(key)
        }
    }

    private val _gets = MutableSharedFlow<Key>()
    private val _sets = MutableSharedFlow<Pair<Key, Value>>()
    private val _removes = MutableSharedFlow<Key>()
    private val _evicts = MutableSharedFlow<Key>()

    /** Returns a [Flow] that emits when [get] is called. */
    val gets: Flow<Key> = _gets.asSharedFlow()

    /** Returns a [Flow] that emits when [set] is called. */
    val sets: Flow<Pair<Key, Value>> = _sets.asSharedFlow()

    /** Returns a [Flow] that emits when [remove] is called. */
    val removes: Flow<Key> = _removes.asSharedFlow()

    /** Returns a [Flow] that emits when an entry is evicted due to the cache exceeding [maxSize]. */
    val evicts: Flow<Key> = _evicts.asSharedFlow()

    /** Returns an immutable snapshot of the keys in this cache. */
    override val keys: Set<Key> get() = cache.snapshot().keys.toImmutableSet()

    /** Returns an immutable snapshot of the keys in this cache. */
    val values: Set<Value> get() = cache.snapshot().values.toImmutableSet()

    /** Returns an immutable snapshot of the entries in this cache. */
    val entries: Map<Key, Value> get() = cache.snapshot().toImmutableMap()

    override val size: Int get() = cache.size()

    override fun get(key: Key): Value? {
        return cache.get(key).also { _gets.tryEmit(key) }
    }

    override fun set(key: Key, value: Value) {
        cache.put(key, value).also { _sets.tryEmit(key to value) }
    }

    override fun remove(key: Key): Boolean {
        return (cache.remove(key) != null).also { _removes.tryEmit(key) }
    }

    override fun clear() {
        // Don't use `evictAll` as deletes via this method should be treated
        // as removals - not evicts.
        cache.snapshot().keys.forEach(cache::remove)
    }

    override fun trimMemory(level: Int) {
        // Follows the same behaviour as in RealStrongMemoryCache.
        if (level >= TRIM_MEMORY_BACKGROUND) {
            clear()
        } else if (level in TRIM_MEMORY_RUNNING_LOW until TRIM_MEMORY_UI_HIDDEN) {
            cache.trimToSize(size / 2)
        }
    }

    override fun toString(): String {
        return "FakeMemoryCache(entries=$cache)"
    }

    class Builder {

        private var maxSize: Int = 0

        fun maxSize(size: Int) = apply {
            this.maxSize = size
        }

        fun build() = FakeMemoryCache(
            maxSize = maxSize,
        )
    }
}

/**
 * Create a new [FakeMemoryCache] without configuration.
 */
@JvmName("create")
fun FakeMemoryCache(): FakeMemoryCache {
    return FakeMemoryCache.Builder().build()
}

/**
 * Assert the [FakeMemoryCache] contains an entry that matches [predicate].
 */
fun FakeMemoryCache.assertContains(predicate: (key: Key, value: Value) -> Boolean) {
    entries.forEach { (key, value) ->
        if (predicate(key, value)) return
    }
    throw AssertionError("No entries matched the predicate: $this")
}

/**
 * Assert the [FakeMemoryCache] does not contain any entries.
 */
fun FakeMemoryCache.assertEmpty() {
    val size = size
    if (size != 0) {
        throw AssertionError("The memory cache is not empty: $this")
    }
}
