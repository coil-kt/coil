@file:Suppress("DEPRECATION", "unused")

package coil.decode

import android.graphics.Bitmap
import android.graphics.Movie
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.JELLY_BEAN_MR2
import android.os.Build.VERSION_CODES.O
import coil.bitmappool.BitmapPool
import coil.drawable.MovieDrawable
import coil.extension.repeatCount
import coil.size.Size
import okio.BufferedSource

/**
 * A [Decoder] that uses [Movie] to decode GIFs.
 *
 * NOTE: Prefer using [ImageDecoderDecoder] on Android P and above.
 */
class GifDecoder : Decoder {

    companion object {
        const val REPEAT_COUNT_KEY = "coil#repeat_count"
    }

    override fun handles(source: BufferedSource, mimeType: String?): Boolean {
        return DecodeUtils.isGif(source)
    }

    override suspend fun decode(
        pool: BitmapPool,
        source: BufferedSource,
        size: Size,
        options: Options
    ): DecodeResult {
        val drawable = MovieDrawable(
            movie = if (SDK_INT <= JELLY_BEAN_MR2) {
                source.use {
                    val byteArray = it.readByteArray()
                    checkNotNull(Movie.decodeByteArray(byteArray, 0, byteArray.size))
                }
            } else {
                source.use { checkNotNull(Movie.decodeStream(it.inputStream())) }
            },
            config = when {
                options.allowRgb565 -> Bitmap.Config.RGB_565
                SDK_INT >= O && options.config == Bitmap.Config.HARDWARE -> Bitmap.Config.ARGB_8888
                else -> options.config
            },
            scale = options.scale,
            pool = pool
        )

        drawable.setRepeatCount(options.parameters.repeatCount() ?: MovieDrawable.REPEAT_INFINITE)

        return DecodeResult(
            drawable = drawable,
            isSampled = false
        )
    }
}
