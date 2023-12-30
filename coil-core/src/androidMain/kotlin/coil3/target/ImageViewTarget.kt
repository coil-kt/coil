package coil3.target

import android.graphics.drawable.Drawable
import android.widget.ImageView
import coil3.annotation.Data

/**
 * A [Target] that handles setting images on an [ImageView].
 */
@Data
open class ImageViewTarget(
    override val view: ImageView,
) : GenericViewTarget<ImageView>() {

    override var drawable: Drawable?
        get() = view.drawable
        set(value) = view.setImageDrawable(value)
}
