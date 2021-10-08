package coil

import android.graphics.Bitmap
import coil.size.PixelSize

val Bitmap.size: PixelSize
    get() = PixelSize(width, height)
