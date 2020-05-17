package coil.memory

import android.graphics.Bitmap
import androidx.annotation.MainThread
import coil.request.Parameters
import coil.size.Size
import coil.transform.Transformation

interface MemoryCache {

    /** The **current size** of the memory cache in bytes. */
    @get:MainThread val size: Int

    /** The **maximum size** of the memory cache in bytes. */
    @get:MainThread val maxSize: Int

    @MainThread
    fun find(criteria: Criteria): Bitmap?

    @MainThread
    fun find(key: String): Bitmap?

    @MainThread
    fun remove(criteria: Criteria)

    @MainThread
    fun remove(key: String)

    @MainThread
    fun clear()

    data class Criteria(
        val data: Any,
        val size: Size? = null,
        val transformations: List<Transformation>? = null,
        val parameters: Parameters? = null
    )
}
