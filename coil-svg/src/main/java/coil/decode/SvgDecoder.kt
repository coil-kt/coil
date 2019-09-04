@file:Suppress("unused")

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

    companion object {
        private const val MIME_TYPE_SVG = "image/svg+xml"
    }

    override fun handles(source: BufferedSource, mimeType: String?) = mimeType == MIME_TYPE_SVG

    override suspend fun decode(
        pool: BitmapPool,
        source: BufferedSource,
        size: Size,
        options: Options
    ): DecodeResult {
        val svg = source.use { SVG.getFromInputStream(it.inputStream()) }
        return DecodeResult(
            drawable = PictureDrawable(svg.renderToPicture()),
            isSampled = false
        )
    }
}
