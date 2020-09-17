@file:JvmName("Contexts")
@file:Suppress("NOTHING_TO_INLINE", "unused")

package coil

import android.content.Context

/**
 * Get the default [ImageLoader]. This is an alias for [Coil.imageLoader].
 */
inline val Context.imageLoader: ImageLoader
    @JvmName("imageLoader") get() = Coil.imageLoader(this)
