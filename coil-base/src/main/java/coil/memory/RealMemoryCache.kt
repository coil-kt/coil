package coil.memory

import android.graphics.Bitmap
import coil.ComponentRegistry
import coil.annotation.ExperimentalCoilApi
import coil.memory.StrongMemoryCache.Key

@OptIn(ExperimentalCoilApi::class)
internal class RealMemoryCache(
    private val componentRegistry: ComponentRegistry,
    private val strongMemoryCache: StrongMemoryCache,
    private val weakMemoryCache: WeakMemoryCache,
    private val bitmapReferenceCounter: BitmapReferenceCounter
) : MemoryCache {

    override val size get() = strongMemoryCache.size

    override val maxSize get() = strongMemoryCache.maxSize

    override fun get(key: String): Bitmap? {
        val cacheKey = Key(key)
        val value = strongMemoryCache.get(cacheKey) ?: weakMemoryCache.get(cacheKey)
        return value?.bitmap?.also(bitmapReferenceCounter::invalidate)
    }

    override fun remove(key: String) {
        val cacheKey = Key(key)
        strongMemoryCache.remove(cacheKey)
        weakMemoryCache.remove(cacheKey)
    }

    override fun clear() {
        strongMemoryCache.clearMemory()
        weakMemoryCache.clearMemory()
    }
}
