package coil.util

import android.graphics.Bitmap
import coil.size.Size

val Bitmap.size: Size
    get() = Size(width, height)
