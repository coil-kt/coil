package coil.memory

import android.graphics.Bitmap
import coil.request.Parameters
import coil.size.Size
import coil.transform.Transformation

interface MemoryCache {

    /** The **current size** of the memory cache in bytes. */
    val size: Int

    /** The **maximum size** of the memory cache in bytes. */
    val maxSize: Int

    fun find(criteria: Criteria): Bitmap?

    fun find(key: String): Bitmap?

    fun remove(criteria: Criteria)

    fun remove(key: String)

    fun clear()

    data class Criteria(
        val data: Any,
        val size: Size? = null,
        val transformations: List<Transformation>? = null,
        val parameters: Parameters? = null
    )
}
