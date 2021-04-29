@file:JvmName("Contexts")
@file:Suppress("NOTHING_TO_INLINE", "unused")

package coil

import android.content.Context

/**
 * Get the singleton [ImageLoader].
 */
val Context.imageLoader: ImageLoader
    @JvmName("imageLoader") get() = Coil.imageLoader(this)
