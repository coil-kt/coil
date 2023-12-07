package coil3.request

import android.widget.ImageView
import android.widget.ImageView.ScaleType.CENTER
import android.widget.ImageView.ScaleType.MATRIX
import coil3.size.DisplaySizeResolver
import coil3.size.Scale
import coil3.size.Size
import coil3.size.SizeResolver
import coil3.size.ViewSizeResolver
import coil3.target.ViewTarget
import coil3.util.scale

internal actual fun ImageRequest.Builder.resolveSizeResolver(): SizeResolver {
    val target = target
    if (target is ViewTarget<*>) {
        // CENTER and MATRIX scale types should be decoded at the image's original size.
        val view = target.view
        if (view is ImageView && view.scaleType.let { it == CENTER || it == MATRIX }) {
            return SizeResolver(Size.ORIGINAL)
        } else {
            return ViewSizeResolver(view)
        }
    } else {
        // Fall back to the size of the display.
        return DisplaySizeResolver(context)
    }
}

internal actual fun ImageRequest.Builder.resolveScale(): Scale {
    val view = (sizeResolver as? ViewSizeResolver<*>)?.view ?: (target as? ViewTarget<*>)?.view
    if (view is ImageView) {
        return view.scale
    } else {
        return Scale.FIT
    }
}
