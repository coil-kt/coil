@file:Suppress("DEPRECATION", "unused")

package coil.decode

import android.graphics.drawable.PictureDrawable
import coil.bitmappool.BitmapPool
import coil.size.Size
import com.caverock.androidsvg.SVG
import okio.BufferedSource

/**
 * A [Decoder] that uses [AndroidSVG](https://bigbadaboom.github.io/androidsvg/) to decode SVG files.
 */
class SvgDecoder : Decoder {

    override fun handles(source: BufferedSource, mimeType: String?) = mimeType == "image/svg+xml"

    override suspend fun decode(pool: BitmapPool, source: BufferedSource, size: Size, options: Options): DecodeResult {
        val svg = source.use { SVG.getFromInputStream(it.inputStream()) }
        return DecodeResult(
            drawable = PictureDrawable(svg.renderToPicture()),
            isSampled = false
        )
    }
}
