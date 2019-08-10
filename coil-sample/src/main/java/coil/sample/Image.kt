package coil.sample

import androidx.annotation.ColorInt
import androidx.annotation.Px

data class Image(
    val url: String,
    @ColorInt val color: Int,
    @Px val width: Int,
    @Px val height: Int
)
