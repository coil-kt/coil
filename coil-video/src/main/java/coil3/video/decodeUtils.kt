package coil3.video

import coil3.annotation.ExperimentalCoilApi
import coil3.decode.DecodeUtils
import okio.BufferedSource
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8

private val MP4_FTYP = "ftyp".encodeUtf8()
private val OGG_SIGNATURE = "OggS".encodeUtf8()
private val AVI_SIGNATURE = "AVI ".encodeUtf8()
private val EBML_SIGNATURE = ByteString.of(0x1A.toByte(), 0x45.toByte(), 0xDF.toByte(), 0xA3.toByte())
private val RIFF_SIGNATURE = "RIFF".encodeUtf8()

@ExperimentalCoilApi
fun DecodeUtils.isVideo(source: BufferedSource): Boolean {
    return when {
        source.rangeEquals(0, OGG_SIGNATURE) -> true
        source.rangeEquals(0, EBML_SIGNATURE) -> true
        source.rangeEquals(4, MP4_FTYP) -> true
        source.rangeEquals(0, RIFF_SIGNATURE) && source.rangeEquals(8, AVI_SIGNATURE) -> true
        else -> false
    }
}
