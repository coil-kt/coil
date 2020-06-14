package coil.memory

import android.graphics.Bitmap
import coil.annotation.ExperimentalCoilApi
import coil.request.Parameters
import coil.size.Size
import coil.transform.Transformation
import coil.util.mapIndices

/**
 * An in-memory cache of recently loaded images.
 */
@ExperimentalCoilApi
interface MemoryCache {

    /** The current size of the cache in bytes. */
    val size: Int

    /** The maximum size of the cache in bytes. */
    val maxSize: Int

    /** Get the [Bitmap] associated with [key]. */
    fun get(key: Key): Bitmap?

    /**
     * Remove the [Bitmap] referenced by [key].
     *
     * @return `true` if the bitmap was removed. Return `false` if there was no bitmap for [key] in the cache.
     */
    fun remove(key: Key): Boolean

    /** Remove all values from this cache. */
    fun clear()

    class Key {

        internal val complex: Boolean
        internal val base: String
        internal val transformations: List<String>
        internal val size: Size?
        internal val parameters: Map<String, String>

        /** Public constructor to create a simple cache key. */
        constructor(base: String) {
            this.complex = false
            this.base = base
            this.transformations = emptyList()
            this.size = null
            this.parameters = emptyMap()
        }

        /** Internal constructor to create a complex cache key. */
        internal constructor(
            base: String,
            parameters: Parameters
        ) {
            this.complex = true
            this.base = base
            this.transformations = emptyList()
            this.size = null
            this.parameters = parameters.cacheKeys()
        }

        /** Internal constructor to create a complex cache key. */
        internal constructor(
            base: String,
            transformations: List<Transformation>,
            size: Size,
            parameters: Parameters
        ) {
            this.complex = true
            this.base = base
            if (transformations.isEmpty()) {
                this.transformations = emptyList()
                this.size = null
            } else {
                this.transformations = transformations.mapIndices { it.key() }
                this.size = size
            }
            this.parameters = parameters.cacheKeys()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Key &&
                complex == other.complex &&
                base == other.base &&
                transformations == other.transformations &&
                size == other.size &&
                parameters == other.parameters
        }

        override fun hashCode(): Int {
            var result = complex.hashCode()
            result = 31 * result + base.hashCode()
            result = 31 * result + transformations.hashCode()
            result = 31 * result + (size?.hashCode() ?: 0)
            result = 31 * result + parameters.hashCode()
            return result
        }

        override fun toString(): String {
            return "Key(complex=$complex, base=$base, transformations=$transformations, size=$size, parameters=$parameters)"
        }
    }
}
