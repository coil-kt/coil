package coil3.decode

import coil3.Image

/**
 * The result of [Decoder.decode].
 *
 * @param image The decoded [Image].
 * @param isSampled 'true' if [image] is sampled
 *  (i.e. loaded into memory at less than its original size).
 *
 * @see Decoder
 */
data class DecodeResult(
    val image: Image,
    val isSampled: Boolean,
)
