package coil.size

import android.content.Context
import kotlin.math.max

/**
 * The default [SizeResolver] that returns the maximum dimension of the display as the size.
 */
internal class DisplaySizeResolver(private val context: Context) : SizeResolver {

    override suspend fun size(): Size {
        val metrics = context.resources.displayMetrics
        val maxDimension = Dimension(max(metrics.widthPixels, metrics.heightPixels))
        return Size(maxDimension, maxDimension)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is DisplaySizeResolver && context == other.context
    }

    override fun hashCode() = context.hashCode()
}
