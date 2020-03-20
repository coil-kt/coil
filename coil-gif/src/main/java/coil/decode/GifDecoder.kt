@file:Suppress("DEPRECATION", "unused")

package coil.decode

import android.graphics.Bitmap
import android.graphics.Movie
import android.os.Build.VERSION.SDK_INT
import coil.bitmappool.BitmapPool
import coil.drawable.MovieDrawable
import coil.extension.repeatCount
import coil.size.Size
import okio.BufferedSource

/**
 * A [Decoder] that uses [Movie] to decode GIFs.
 *
 * NOTE: Prefer using [ImageDecoderDecoder] on API 28 and above.
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
        // Movie requires an InputStream to resettable on API 18 and below.
        // Read the data as a ByteArray to work around this.
        val movie = if (SDK_INT <= 18) {
            source.use {
                val byteArray = it.readByteArray()
                checkNotNull(Movie.decodeByteArray(byteArray, 0, byteArray.size))
            }
        } else {
            source.use { checkNotNull(Movie.decodeStream(it.inputStream())) }
        }

        check(movie.width() > 0 && movie.height() > 0) { "Failed to decode GIF." }

        val drawable = MovieDrawable(
            movie = movie,
            pool = pool,
            config = when {
                options.allowRgb565 -> Bitmap.Config.RGB_565
                SDK_INT >= 26 && options.config == Bitmap.Config.HARDWARE -> Bitmap.Config.ARGB_8888
                else -> options.config
            },
            scale = options.scale
        )

        drawable.setRepeatCount(options.parameters.repeatCount() ?: MovieDrawable.REPEAT_INFINITE)

        return DecodeResult(
            drawable = drawable,
            isSampled = false
        )
    }
}
