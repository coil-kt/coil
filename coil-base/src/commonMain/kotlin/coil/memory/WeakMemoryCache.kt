package coil.memory

import coil.Image
import coil.memory.MemoryCache.Key
import coil.memory.MemoryCache.Value

/**
 * An in-memory cache that holds weak references.
 *
 * Values are added to [WeakMemoryCache] when they're evicted from [StrongMemoryCache].
 */
internal interface WeakMemoryCache {

    val keys: Set<Key>

    fun get(key: Key): Value?

    fun set(key: Key, image: Image, extras: Map<String, Any>, size: Long)

    fun remove(key: Key): Boolean

    fun clear()
}

internal class EmptyWeakMemoryCache : WeakMemoryCache {

    override val keys get() = emptySet<Key>()

    override fun get(key: Key): Value? = null

    override fun set(key: Key, image: Image, extras: Map<String, Any>, size: Long) {}

    override fun remove(key: Key) = false

    override fun clear() {}
}

internal expect fun RealWeakMemoryCache(): WeakMemoryCache
