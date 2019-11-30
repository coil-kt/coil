package coil.decode

import android.graphics.drawable.Drawable

/**
 * The result of [Decoder.decode].
 *
 * @param drawable The decoded [Drawable].
 * @param isSampled True if [drawable] is sampled (i.e. loaded into memory at less than its original size).
 *
 * @see Decoder
 */
data class DecodeResult(
    val drawable: Drawable,
    val isSampled: Boolean
)
