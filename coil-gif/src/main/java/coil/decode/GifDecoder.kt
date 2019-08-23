@file:Suppress("DEPRECATION", "unused")

package coil.decode

import android.graphics.Bitmap
import android.graphics.Movie
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import coil.bitmappool.BitmapPool
import coil.drawable.MovieDrawable
import coil.size.Size
import okio.BufferedSource

/**
 * A [Decoder] that uses [Movie] to decode GIFs.
 *
 * NOTE: Prefer using [ImageDecoderDecoder] on Android P and above.
 */
class GifDecoder : Decoder {

    override fun handles(source: BufferedSource, mimeType: String?): Boolean {
        return DecodeUtils.isGif(source)
    }

    override suspend fun decode(
        pool: BitmapPool,
        source: BufferedSource,
        size: Size,
        options: Options
    ): DecodeResult {
        return DecodeResult(
            drawable = MovieDrawable(
                movie = source.use { checkNotNull(Movie.decodeStream(it.inputStream())) },
                config = when {
                    options.allowRgb565 -> Bitmap.Config.RGB_565
                    SDK_INT >= O && options.config == Bitmap.Config.HARDWARE -> Bitmap.Config.ARGB_8888
                    else -> options.config
                },
                scale = options.scale,
                pool = pool
            ),
            isSampled = false
        )
    }
}
