@file:Suppress("unused")

package coil.decode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import androidx.core.graphics.drawable.toDrawable
import coil.bitmappool.BitmapPool
import coil.size.OriginalSize
import coil.size.PixelSize
import coil.size.Size
import com.caverock.androidsvg.SVG
import okio.BufferedSource

/**
 * A [Decoder] that uses [AndroidSVG](https://bigbadaboom.github.io/androidsvg/) to decode SVG files.
 */
class SvgDecoder(private val context: Context) : Decoder {

    companion object {
        private const val MIME_TYPE_SVG = "image/svg+xml"
        private const val DEFAULT_SIZE = 512
    }

    override fun handles(source: BufferedSource, mimeType: String?) = mimeType == MIME_TYPE_SVG

    override suspend fun decode(
        pool: BitmapPool,
        source: BufferedSource,
        size: Size,
        options: Options
    ): DecodeResult {
        val svg = source.use { SVG.getFromInputStream(it.inputStream()) }

        val svgWidth = svg.documentWidth
        val svgHeight = svg.documentHeight

        val bitmapWidth: Int
        val bitmapHeight: Int
        when (size) {
            is PixelSize -> {
                if (svgWidth > 0 && svgHeight > 0) {
                    val multiplier = DecodeUtils.computeSizeMultiplier(
                        srcWidth = svgWidth,
                        srcHeight = svgHeight,
                        dstWidth = size.width.toFloat(),
                        dstHeight = size.height.toFloat(),
                        scale = options.scale
                    )
                    bitmapWidth = (multiplier * svgWidth).toInt()
                    bitmapHeight = (multiplier * svgHeight).toInt()
                } else {
                    bitmapWidth = size.width
                    bitmapHeight = size.height
                }
            }
            is OriginalSize -> {
                if (svgWidth > 0 && svgHeight > 0) {
                    bitmapWidth = svgWidth.toInt()
                    bitmapHeight = svgHeight.toInt()
                } else {
                    bitmapWidth = DEFAULT_SIZE
                    bitmapHeight = DEFAULT_SIZE
                }
            }
        }

        val config = when {
            options.allowRgb565 -> Bitmap.Config.RGB_565
            SDK_INT >= O && options.config == Bitmap.Config.HARDWARE -> Bitmap.Config.ARGB_8888
            else -> options.config
        }
        val bitmap = pool.get(bitmapWidth, bitmapHeight, config)

        // Set the SVG's view box to enable scaling if it is not set.
        if (svg.documentViewBox == null && svgWidth > 0 && svgHeight > 0) {
            svg.setDocumentViewBox(0f, 0f, svgWidth, svgHeight)
        }

        svg.setDocumentWidth("100%")
        svg.setDocumentHeight("100%")
        svg.renderToCanvas(Canvas(bitmap))

        return DecodeResult(
            drawable = bitmap.toDrawable(context.resources),
            isSampled = true // SVGs can always be re-decoded at a higher resolution.
        )
    }
}
