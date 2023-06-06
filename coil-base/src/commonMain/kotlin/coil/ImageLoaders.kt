@file:JvmName("ImageLoaders")

package coil

import android.content.Context
import kotlin.jvm.JvmName

/**
 * Create a new [ImageLoader] without configuration.
 */
@JvmName("create")
fun ImageLoader(context: Context): ImageLoader {
    return ImageLoader.Builder(context).build()
}
