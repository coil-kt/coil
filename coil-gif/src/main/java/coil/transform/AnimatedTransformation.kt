package coil.transform

import android.graphics.Canvas
import coil.annotation.ExperimentalCoilApi

/**
 * An interface for making transformations to an animated image's pixel data.
 */
@ExperimentalCoilApi
fun interface AnimatedTransformation {

    /**
     * Apply the transformation to the [canvas].
     *
     * @param canvas The [Canvas] to draw on.
     * @return The opacity of the image after drawing.
     */
    fun transform(canvas: Canvas): PixelOpacity
}
