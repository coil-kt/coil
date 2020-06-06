package coil.size

import android.widget.ImageView
import coil.request.ImageRequest

/**
 * Represents a scaling policy.
 *
 * Conceptually, you can think of this as [ImageView.ScaleType] without any knowledge of an image's gravity in the view.
 *
 * @see ImageRequest.Builder.scale
 */
enum class Scale {

    /**
     * Fill the image in the view such that both dimensions (width and height) of the image will be **equal to or larger than**
     * the corresponding dimension of the view.
     */
    FILL,

    /**
     * Fit the image to the view so that both dimensions (width and height) of the image will be **equal to or less than**
     * the corresponding dimension of the view.
     *
     * Generally, this is the default value for functions that accept a [Scale].
     */
    FIT
}
