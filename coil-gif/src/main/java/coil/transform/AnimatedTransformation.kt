package coil.transform

import android.graphics.Canvas
import coil.size.Size
import android.graphics.PixelFormat as AndroidPixelFormat

/**
 * An interface for applying transformation on GIFs, animated WebPs, and animated HEIFs.
 */
interface AnimatedTransformation {

    /**
     * Apply transformation on [canvas]
     *
     * Note: Do not allocate objects within if you are using [coil.decode.GifDecoder] as this method would be invoked
     * for each frame.
     *
     * @return Opacity of the result after drawing.
     * [PixelFormat.UNKNOWN] means that the implementation did not change whether the image has alpha. Return
     * this unless you added transparency (e.g. with the code above, in which case you should return
     * [PixelFormat.TRANSLUCENT]) or you forced the image to be opaque (e.g. by drawing everywhere with an
     * opaque color and [PorterDuff.Mode.DST_OVER], in which case you should return [PixelFormat.OPAQUE]).
     * [PixelFormat.TRANSLUCENT] means that the implementation added transparency. This is safe to return even
     * if the image already had transparency. This is also safe to return if the result is opaque,
     * though it may draw more slowly.
     * [PixelFormat.OPAQUE] means that the implementation forced the image to be opaque. This is safe to return
     * even if the image was already opaque.
     * [PixelFormat.TRANSPARENT] (or any other integer) is not allowed, and will result in throwing an
     * [java.lang.IllegalArgumentException].
     */
    fun transform(canvas: Canvas, size: Size): PixelFormat

    /**
     * Opacity of the result after drawing.
     */
    enum class PixelFormat(val opacity: Int) {
        UNKNOWN(AndroidPixelFormat.UNKNOWN),
        TRANSLUCENT(AndroidPixelFormat.TRANSLUCENT),
        OPAQUE(AndroidPixelFormat.OPAQUE),
        TRANSPARENT(AndroidPixelFormat.TRANSPARENT)
    }
}
