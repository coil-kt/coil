package coil.memory

import android.graphics.Bitmap
import coil.memory.MemoryCache.Key

internal class RealMemoryCache(
    val strongMemoryCache: StrongMemoryCache,
    val weakMemoryCache: WeakMemoryCache
) : MemoryCache {

    override val size get() = strongMemoryCache.size

    override val maxSize get() = strongMemoryCache.maxSize

    override fun get(key: Key): Bitmap? {
        return (strongMemoryCache.get(key) ?: weakMemoryCache.get(key))?.bitmap
    }

    override fun set(key: Key, bitmap: Bitmap) {
        strongMemoryCache.set(key, bitmap, false)
        weakMemoryCache.remove(key) // Clear any existing weak values.
    }

    override fun remove(key: Key): Boolean {
        // Do not short circuit.
        val removedStrong = strongMemoryCache.remove(key)
        val removedWeak = weakMemoryCache.remove(key)
        return removedStrong || removedWeak
    }

    override fun clear() {
        strongMemoryCache.clearMemory()
        weakMemoryCache.clearMemory()
    }

    interface Value {
        val bitmap: Bitmap
        val isSampled: Boolean
    }
}
