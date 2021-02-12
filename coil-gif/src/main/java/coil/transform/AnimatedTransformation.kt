package coil.transform

import android.graphics.Canvas
import android.graphics.PixelFormat as AndroidPixelFormat

/**
 * An interface for making transformations to an animated image's pixel data.
 */
fun interface AnimatedTransformation {

    /**
     * Apply the transformation to the [canvas].
     *
     * Note: Do not allocate objects in this method as it will be invoked on each frame of the animation.
     *
     * @param canvas The [Canvas] on which to apply the transformation.
     * @return The [PixelFormat] to use when rendering
     */
    fun transform(canvas: Canvas): PixelFormat

    enum class PixelFormat(val opacity: Int) {
        UNKNOWN(AndroidPixelFormat.UNKNOWN),
        TRANSLUCENT(AndroidPixelFormat.TRANSLUCENT),
        OPAQUE(AndroidPixelFormat.OPAQUE),
        TRANSPARENT(AndroidPixelFormat.TRANSPARENT)
    }
}
