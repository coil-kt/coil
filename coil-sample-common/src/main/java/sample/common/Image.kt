package sample.common

import androidx.annotation.ColorInt
import androidx.annotation.Px
import coil.Extras

data class Image(
    val uri: String,
    @ColorInt val color: Int,
    @Px val width: Int,
    @Px val height: Int,
    val extras: Extras = Extras.EMPTY,
)
