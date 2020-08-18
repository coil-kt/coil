package coil.size

import android.content.Context
import coil.request.ImageRequest

/**
 * A [SizeResolver] that measures the size of the display.
 *
 * This is used as the fallback [SizeResolver] for [ImageRequest]s.
 */
class DisplaySizeResolver(private val context: Context) : SizeResolver {

    override suspend fun size(): Size {
        return context.resources.displayMetrics.run { PixelSize(widthPixels, heightPixels) }
    }

    override fun equals(other: Any?): Boolean {
        return (this === other) || (other is DisplaySizeResolver && context == other.context)
    }

    override fun hashCode() = context.hashCode()

    override fun toString() = "DisplaySizeResolver(context=$context)"
}
