package coil.decode

import android.graphics.drawable.ColorDrawable
import coil.ComponentRegistry
import coil.bitmap.BitmapPool
import coil.size.Size
import okio.BufferedSource
import okio.blackholeSink

/**
 * A [Decoder] that exhausts the source and returns a hardcoded, empty result.
 *
 * NOTE: **Do not** register this in your [ComponentRegistry]. It will be used automatically for disk-only preload requests.
 */
internal object EmptyDecoder : Decoder {

    private val result = DecodeResult(ColorDrawable(), false)
    private val sink = blackholeSink()

    /** Hardcode this to false to prevent accidental use. */
    override fun handles(source: BufferedSource, mimeType: String?) = false

    override suspend fun decode(
        pool: BitmapPool,
        source: BufferedSource,
        size: Size,
        options: Options
    ): DecodeResult {
        source.use { it.readAll(sink) }
        return result
    }
}
