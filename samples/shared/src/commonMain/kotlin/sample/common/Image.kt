package sample.common

import coil3.Extras

data class Image(
    val uri: String,
    val color: Int,
    val width: Int,
    val height: Int,
    val extras: Extras = Extras.EMPTY,
)
