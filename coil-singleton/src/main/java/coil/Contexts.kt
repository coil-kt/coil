@file:JvmName("Contexts")
@file:Suppress("unused")

package coil

import android.content.Context

/**
 * Get the singleton [ImageLoader]. This is an alias for [Coil.imageLoader].
 */
inline val Context.imageLoader: ImageLoader
    @JvmName("imageLoader") get() = Coil.imageLoader(this)
