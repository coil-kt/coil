package coil.size

import android.widget.ImageView
import coil.util.scale

class RealImageViewScaleResolver(private val view: ImageView) : ScaleResolver {

    override suspend fun scale() = view.scale

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is RealImageViewScaleResolver && view == other.view
    }

    override fun hashCode() = view.hashCode()
}
