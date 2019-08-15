@file:Suppress("DEPRECATION", "unused")

package coil.decode

import android.graphics.drawable.PictureDrawable
import coil.bitmappool.BitmapPool
import coil.size.PixelSize
import coil.size.Size
import com.caverock.androidsvg.RenderOptions
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import okio.BufferedSource

/**
 * A [Decoder] that uses android-SVG to decode SVG files.
 */
class SvgDecoder : Decoder {

    override fun handles(source: BufferedSource, mimeType: String?) = mimeType == "image/svg+xml"

    override suspend fun decode(pool: BitmapPool, source: BufferedSource, size: Size, options: Options): DecodeResult {
        try {
            val svg = source.use { SVG.getFromInputStream(it.inputStream()) }
            return DecodeResult(
                    drawable = PictureDrawable(svg.renderToPicture()),
                    isSampled = false
            )
        } catch (e: SVGParseException) {
            throw IllegalStateException("Failed to load SVG.", e)
        }
    }
}
