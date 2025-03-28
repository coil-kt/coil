package coil3.transform

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import coil3.decode.DecodeUtils
import coil3.size.Scale

/**
 * Create a [Paint] that draws [input] centered and scaled inside the output dimensions.
 */
internal fun newBitmapShaderPaint(
    input: Bitmap,
    outputWidth: Int,
    outputHeight: Int,
): Paint {
    val matrix = Matrix()
    val multiplier = DecodeUtils.computeSizeMultiplier(
        srcWidth = input.width,
        srcHeight = input.height,
        dstWidth = outputWidth,
        dstHeight = outputHeight,
        scale = Scale.FILL,
    ).toFloat()
    val dx = (outputWidth - multiplier * input.width) / 2
    val dy = (outputHeight - multiplier * input.height) / 2
    matrix.setTranslate(dx, dy)
    matrix.preScale(multiplier, multiplier)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val shader = BitmapShader(input, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    shader.setLocalMatrix(matrix)
    paint.shader = shader
    return paint
}
