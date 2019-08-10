package coil.decode

import android.graphics.drawable.Drawable

/**
 * The result of [Decoder.decode].
 *
 * @param drawable The loaded [Drawable].
 * @param isSampled True if [drawable] is sampled (i.e. not loaded into memory at full size).
 *
 * @see Decoder
 */
data class DecodeResult(
    val drawable: Drawable,
    val isSampled: Boolean
)
