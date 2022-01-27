package coil.decode

import android.graphics.drawable.Drawable

/**
 * The result of [Decoder.decode].
 *
 * @param drawable The decoded [Drawable].
 * @param isSampled 'true' if [drawable] is sampled (i.e. loaded into memory at less than its
 *  original size).
 *
 * @see Decoder
 */
class DecodeResult(
    val drawable: Drawable,
    val isSampled: Boolean,
) {

    fun copy(
        drawable: Drawable = this.drawable,
        isSampled: Boolean = this.isSampled,
    ) = DecodeResult(
        drawable = drawable,
        isSampled = isSampled
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is DecodeResult &&
            drawable == other.drawable &&
            isSampled == other.isSampled
    }

    override fun hashCode(): Int {
        var result = drawable.hashCode()
        result = 31 * result + isSampled.hashCode()
        return result
    }
}
