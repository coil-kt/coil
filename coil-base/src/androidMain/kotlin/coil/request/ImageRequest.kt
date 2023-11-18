package coil.request

import android.widget.ImageView
import android.widget.ImageView.ScaleType.CENTER
import android.widget.ImageView.ScaleType.MATRIX
import coil.size.DisplaySizeResolver
import coil.size.Scale
import coil.size.Size
import coil.size.SizeResolver
import coil.size.ViewSizeResolver
import coil.target.ViewTarget
import coil.util.scale

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
