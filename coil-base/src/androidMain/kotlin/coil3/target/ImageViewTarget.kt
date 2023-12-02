package coil3.target

import android.graphics.drawable.Drawable
import android.widget.ImageView
import dev.drewhamilton.poko.Poko

/**
 * A [Target] that handles setting images on an [ImageView].
 */
@Poko
open class ImageViewTarget(override val view: ImageView) : GenericViewTarget<ImageView>() {

    override var drawable: Drawable?
        get() = view.drawable
        set(value) = view.setImageDrawable(value)
}
