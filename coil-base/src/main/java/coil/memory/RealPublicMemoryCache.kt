package coil.memory

import android.graphics.Bitmap
import coil.ComponentRegistry
import coil.memory.PublicMemoryCache.Criteria
import coil.util.assertMainThread
import coil.util.mapData
import coil.util.mapIndices

internal class RealPublicMemoryCache(
    private val componentRegistry: ComponentRegistry,
    private val memoryCache: MemoryCache,
    private val weakMemoryCache: WeakMemoryCache,
    private val bitmapReferenceCounter: BitmapReferenceCounter
) : PublicMemoryCache {

    override val size: Int
        get() {
            assertMainThread()
            return memoryCache.size
        }

    override val maxSize: Int
        get() {
            assertMainThread()
            return memoryCache.maxSize
        }

    override fun find(key: String): Bitmap? {
        assertMainThread()

        val bitmap = memoryCache.get(MemoryCache.Key(key))?.bitmap
        if (bitmap != null) {
            bitmapReferenceCounter.invalidate(bitmap)
        }
        return bitmap
    }

    override fun find(criteria: Criteria): Bitmap? {
        assertMainThread()

        val predicate = criteria.toPredicate()
        memoryCache.find(predicate)?.let { return memoryCache.get(it)?.bitmap }
        weakMemoryCache.find(predicate)?.let { return weakMemoryCache.get(it)?.bitmap }
        return null
    }

    override fun remove(key: String) {
        assertMainThread()

        val cacheKey = MemoryCache.Key(key)
        memoryCache.remove(cacheKey)
        weakMemoryCache.remove(cacheKey)
    }

    override fun remove(criteria: Criteria) {
        assertMainThread()

        val predicate = criteria.toPredicate()
        memoryCache.find(predicate)?.let(memoryCache::remove)
        weakMemoryCache.find(predicate)?.let(weakMemoryCache::remove)
    }

    override fun clear() {
        assertMainThread()

        memoryCache.clearMemory()
        weakMemoryCache.clearMemory()
    }

    private fun Criteria.toPredicate(): (MemoryCache.Key) -> Boolean {
        val mappedData = componentRegistry.mapData(data) { measuredMapper ->
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
        }
    }
}
