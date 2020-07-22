@file:Suppress("DEPRECATION", "unused")

package coil.decode

import android.graphics.Bitmap
import android.graphics.Movie
import android.os.Build.VERSION.SDK_INT
import coil.annotation.InternalCoilApi
import coil.bitmap.BitmapPool
import coil.drawable.MovieDrawable
import coil.request.repeatCount
import coil.size.Size
import okio.BufferedSource
import okio.buffer

/**
 * A [Decoder] that uses [Movie] to decode GIFs.
 *
 * NOTE: Prefer using [ImageDecoderDecoder] on API 28 and above.
 */
class GifDecoder : Decoder {

    override fun handles(source: BufferedSource, mimeType: String?): Boolean {
        return DecodeUtils.isGif(source)
    }

    @OptIn(InternalCoilApi::class)
    override suspend fun decode(
        pool: BitmapPool,
        source: BufferedSource,
        size: Size,
        options: Options
    ): DecodeResult = withInterruptibleSource(source) { interruptibleSource ->
        // Movie requires an InputStream to resettable on API 18 and below.
        // Read the data as a ByteArray to work around this.
        val bufferedSource = interruptibleSource.buffer()
        val movie = if (SDK_INT <= 18) {
            bufferedSource.use {
                val byteArray = it.readByteArray()
                checkNotNull(Movie.decodeByteArray(byteArray, 0, byteArray.size))
            }
        } else {
            bufferedSource.use { checkNotNull(Movie.decodeStream(it.inputStream())) }
        }

        check(movie.width() > 0 && movie.height() > 0) { "Failed to decode GIF." }

        val drawable = MovieDrawable(
            movie = movie,
            pool = pool,
            config = when {
                movie.isOpaque && options.allowRgb565 -> Bitmap.Config.RGB_565
                SDK_INT >= 26 && options.config == Bitmap.Config.HARDWARE -> Bitmap.Config.ARGB_8888
                else -> options.config
            },
            scale = options.scale
        )

        drawable.setRepeatCount(options.parameters.repeatCount() ?: MovieDrawable.REPEAT_INFINITE)

        DecodeResult(
            drawable = drawable,
            isSampled = false
        )
    }

    companion object {
        const val REPEAT_COUNT_KEY = "coil#repeat_count"
    }
}
