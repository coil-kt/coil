package coil3

import coil3.annotation.ExperimentalCoilApi
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas

@ExperimentalCoilApi
abstract class DrawableImage : Image {
    override fun toBitmap(): Bitmap {
        val bitmap = Bitmap()
        bitmap.allocN32Pixels(width, height)
        Canvas(bitmap).onDraw()
        return bitmap
    }

    abstract fun Canvas.onDraw()
}
