package coil.decode

import coil.ComponentRegistry
import coil.bitmappool.BitmapPool
import coil.size.Size
import coil.util.EMPTY_DRAWABLE
import okio.BufferedSource
import okio.blackholeSink

/**
 * A [Decoder] that exhausts the source and returns a hardcoded, empty result.
 *
 * NOTE: **Do not** register this in your [ComponentRegistry]. It will be used automatically for disk-only preload requests.
 */
internal object EmptyDecoder : Decoder {

    private val result = DecodeResult(EMPTY_DRAWABLE, false)
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
