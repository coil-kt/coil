package coil.target

import android.graphics.drawable.Drawable
import android.widget.ImageView

/**
 * A [Target] that handles setting images on an [ImageView].
 */
open class ImageViewTarget(override val view: ImageView) : GenericViewTarget<ImageView>() {

    override var drawable: Drawable?
        get() = view.drawable
        set(value) = view.setImageDrawable(value)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is ImageViewTarget && view == other.view
    }

    override fun hashCode() = view.hashCode()
}
