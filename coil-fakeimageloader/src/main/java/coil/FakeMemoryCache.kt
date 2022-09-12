@file:JvmName("FakeMemoryCaches")

package coil

import android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
import android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
import android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
import androidx.collection.LruCache
import coil.annotation.ExperimentalCoilApi
import coil.memory.MemoryCache
import coil.memory.MemoryCache.Key
import coil.memory.MemoryCache.Value
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

@ExperimentalCoilApi
class FakeMemoryCache private constructor(
    override val maxSize: Int
): MemoryCache {

    private val cache = object : LruCache<Key, Value>(maxSize) {
        override fun sizeOf(key: Key, value: Value) = value.bitmap.allocationByteCount
    }

    private val _gets = MutableSharedFlow<Key>()
    val gets: Flow<Key> get() = _gets

    private val _sets = MutableSharedFlow<Pair<Key, Value>>()
    val sets: Flow<Pair<Key, Value>> get() = _sets

    private val _removes = MutableSharedFlow<Key>()
    val removes: Flow<Key> get() = _removes

    override val size: Int get() = cache.size()

    override val keys: Set<Key> get() = cache.snapshot().keys.toImmutableSet()

    val values: Set<Value> get() = cache.snapshot().values.toImmutableSet()

    val snapshot: Map<Key, Value> get() = cache.snapshot().toImmutableMap()

    override fun get(key: Key): Value? {
        _gets.tryEmit(key)
        return cache.get(key)
    }

    override fun set(key: Key, value: Value) {
        _sets.tryEmit(key to value)
        cache.put(key, value)
    }

    override fun remove(key: Key): Boolean {
        _removes.tryEmit(key)
        return cache.remove(key) != null
    }

    override fun clear() {
        cache.evictAll()
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
        return "FakeMemoryCache(cache=$cache)"
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
    snapshot.entries.forEach { (key, value) ->
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
