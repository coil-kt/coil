package coil.size

import android.content.Context
import coil.request.Request
import coil.target.Target

/**
 * A [SizeResolver] that constrains a [Target] to the size of the display.
 *
 * This is used as the fallback [SizeResolver] for [Request]s.
 */
class DisplaySizeResolver(private val context: Context) : SizeResolver {

    override suspend fun size(): Size {
        return context.resources.displayMetrics.run { PixelSize(widthPixels, heightPixels) }
    }
}
