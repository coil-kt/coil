package coil.memory

import android.graphics.Bitmap
import coil.annotation.ExperimentalCoilApi

/** An in-memory cache of recently loaded images. */
@ExperimentalCoilApi
interface MemoryCache {

    /** The current size of the cache in bytes. */
    val size: Int

    /** The maximum size of the cache in bytes. */
    val maxSize: Int

    fun get(key: String): Bitmap?

    fun remove(key: String)

    fun clear()
}
