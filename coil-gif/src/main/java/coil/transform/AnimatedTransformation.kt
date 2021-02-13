package coil.transform

import android.graphics.Canvas

/**
 * An interface for making transformations to an animated image's pixel data.
 */
fun interface AnimatedTransformation {

    /**
     * Apply the transformation to the [canvas].
     *
     * NOTE: Avoid allocating objects in this method as it will be invoked on each frame of the animation.
     *
     * @param canvas The [Canvas] to draw on.
     * @return The opacity of the image after drawing.
     */
    fun transform(canvas: Canvas): PixelOpacity
}
