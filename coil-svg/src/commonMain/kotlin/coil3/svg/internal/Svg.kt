package coil3.svg.internal

import coil3.Image
import coil3.request.Options
import okio.BufferedSource

internal expect fun Svg.Companion.parse(source: BufferedSource): Svg

internal interface Svg {
    val viewBox: FloatArray?
    val width: Float
    val height: Float

    fun viewBox(value: FloatArray)
    fun width(value: String)
    fun height(value: String)
    fun options(options: Options)

    fun asImage(width: Int, height: Int): Image

    companion object
}
