package coil.size

import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import coil.util.scale

internal class ImageViewScaleResolver(private val view: ImageView) : ScaleResolver {

    override fun scale(): Scale {
        val params = view.layoutParams
        if (params != null && (params.width == WRAP_CONTENT || params.height == WRAP_CONTENT)) {
            // Always use `Scale.FIT` if one or more dimensions are unbounded.
            return Scale.FIT
        } else {
            return view.scale
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is ImageViewScaleResolver && view == other.view
    }

    override fun hashCode() = view.hashCode()
}
