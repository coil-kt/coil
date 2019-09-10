@file:Suppress("unused")

package coil.decode

import android.graphics.Bitmap
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import coil.bitmappool.BitmapPool
import coil.drawable.SvgDrawable
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
        return DecodeResult(
            drawable = SvgDrawable(
                svg = source.use { SVG.getFromInputStream(it.inputStream()) },
                config = when {
                    options.allowRgb565 -> Bitmap.Config.RGB_565
                    SDK_INT >= O && options.config == Bitmap.Config.HARDWARE -> Bitmap.Config.ARGB_8888
                    else -> options.config
                },
                pool = pool
            ),
            isSampled = false
        )
    }
}
