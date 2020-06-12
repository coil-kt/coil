package coil.memory

import android.graphics.Bitmap
import coil.fetch.Fetcher
import coil.request.Parameters
import coil.size.Size
import coil.transform.Transformation
import coil.util.mapIndices

/** An in-memory cache of recently loaded images. */
interface MemoryCache {

    /** The current size of the cache in bytes. */
    val size: Int

    /** The maximum size of the cache in bytes. */
    val maxSize: Int

    /** Get the value associated with [key]. */
    fun get(key: Key): Value?

    /** Remove the value referenced by [key] from this cache if it is present. */
    fun remove(key: Key)

    /** Remove all values from this cache. */
    fun clear()

    class Key {

        /** The base component of the cache key. This is typically [Fetcher.key]. */
        internal val baseKey: String

        /** An ordered list of [Transformation.key]s. */
        internal val transformationKeys: List<String>

        /** The resolved size for the request. This is null if [transformationKeys] is empty. */
        internal val size: Size?

        /** @see Parameters.cacheKeys */
        internal val parameterKeys: Map<String, String>

        @JvmOverloads
        constructor(
            baseKey: String,
            parameters: Parameters = Parameters.EMPTY
        ) {
            this.baseKey = baseKey
            this.transformationKeys = emptyList()
            this.size = null
            this.parameterKeys = parameters.cacheKeys()
        }

        @JvmOverloads
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
    }

    interface Value {
        val bitmap: Bitmap
        val isSampled: Boolean
    }
}
