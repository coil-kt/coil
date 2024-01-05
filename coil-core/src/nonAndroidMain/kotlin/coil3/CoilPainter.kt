package coil3

import coil3.annotation.ExperimentalCoilApi
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas

@ExperimentalCoilApi
sealed interface CoilPainter {
    @ExperimentalCoilApi
    fun interface BitmapPainter : CoilPainter {
        fun asBitmap(): Bitmap
    }

    @ExperimentalCoilApi
    fun interface VectorPainter : CoilPainter {
        fun Canvas.onDraw()
    }
}
