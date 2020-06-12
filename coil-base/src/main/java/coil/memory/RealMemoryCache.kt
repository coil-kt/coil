package coil.memory

import android.graphics.Bitmap
import coil.ComponentRegistry
import coil.memory.MemoryCache.Criteria
import coil.memory.StrongMemoryCache.Key

internal class RealMemoryCache(
    private val componentRegistry: ComponentRegistry,
    private val strongMemoryCache: StrongMemoryCache,
    private val weakMemoryCache: WeakMemoryCache,
    private val bitmapReferenceCounter: BitmapReferenceCounter
) : MemoryCache {

    override val size get() = strongMemoryCache.size

    override val maxSize get() = strongMemoryCache.maxSize

    override fun find(key: String): Bitmap? {
        return invalidate(strongMemoryCache.get(Key(key))?.bitmap)
    }

    override fun find(criteria: Criteria): Bitmap? {
        val predicate = criteria.toPredicate()
        strongMemoryCache.find(predicate)?.let {
            return invalidate(strongMemoryCache.get(it)?.bitmap)
        }
        weakMemoryCache.find(predicate)?.let {
            return invalidate(weakMemoryCache.get(it)?.bitmap)
        }
        return null
    }

    override fun remove(key: String) {
        val cacheKey = Key(key)
        strongMemoryCache.remove(cacheKey)
        weakMemoryCache.remove(cacheKey)
    }

    override fun remove(criteria: Criteria) {
        val predicate = criteria.toPredicate()
        strongMemoryCache.find(predicate)?.let(strongMemoryCache::remove)
        weakMemoryCache.find(predicate)?.let(weakMemoryCache::remove)
    }

    override fun clear() {
        strongMemoryCache.clearMemory()
        weakMemoryCache.clearMemory()
    }

    private fun invalidate(bitmap: Bitmap?): Bitmap? {
        if (bitmap != null) {
            bitmapReferenceCounter.invalidate(bitmap)
        }
        return bitmap
    }

    private fun Criteria.toPredicate(): (Key) -> Boolean {
        TODO()
        /*val mappedData = componentRegistry.mapData(data) { measuredMapper ->
            checkNotNull(size) { "'$measuredMapper' requires a non null size to map '$data'." }
        }
        val baseKey = componentRegistry.requireFetcher(mappedData).key(mappedData)
        val transformationKeys = transformations?.mapIndices { it.key() }
        val parameterKeys = parameters?.cacheKeys()
        return { key ->
            baseKey == key.baseKey &&
                transformationKeys == null || transformationKeys == key.transformationKeys &&
                parameterKeys == null || parameterKeys == key.parameterKeys
                size == null || size == key.size
        }*/
    }
}
