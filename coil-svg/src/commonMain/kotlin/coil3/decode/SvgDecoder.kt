package coil3.decode

object SvgDecoder {
    fun Factory(
        useViewBoundsAsIntrinsicSize: Boolean = true
    ): Decoder.Factory = SvgDecoderFactory(useViewBoundsAsIntrinsicSize)
}

internal expect fun SvgDecoderFactory(
    useViewBoundsAsIntrinsicSize: Boolean,
): Decoder.Factory
