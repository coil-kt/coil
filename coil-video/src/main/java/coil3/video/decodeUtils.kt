package coil3.video

import coil3.annotation.ExperimentalCoilApi
import coil3.decode.DecodeUtils
import okio.BufferedSource
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.encodeUtf8

private val MP4_FTYP = "ftyp".encodeUtf8()
private val OGG_SIGNATURE = "OggS".encodeUtf8()
private val AVI_SIGNATURE = "AVI ".encodeUtf8()
private val EBML_SIGNATURE = ByteString.of(0x1A.toByte(), 0x45.toByte(), 0xDF.toByte(), 0xA3.toByte())
private val RIFF_SIGNATURE = "RIFF".encodeUtf8()
private val FLV_SIGNATURE = "FLV".encodeUtf8()
private val REAL_MEDIA_SIGNATURE = ByteString.of(0x2E, 0x52, 0x4D, 0x46)
private val MPEG_PROGRAM_SIGNATURE = ByteString.of(0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0xBA.toByte())
private val ASF_SIGNATURE = "3026B2758E66CF11A6D900AA0062CE6C".decodeHex()
private val MPEG_TS_SYNC_BYTE = ByteString.of(0x47.toByte())
private const val MPEG_TS_PACKET_SIZE = 188L
private const val MPEG_TS_PACKETS_TO_CHECK = 4L

@ExperimentalCoilApi
fun DecodeUtils.isVideo(source: BufferedSource): Boolean {
    val peek = source.peek()
    return when {
        peek.rangeEquals(0, OGG_SIGNATURE) -> true // OGG, WebM, Matroska cousins
        peek.rangeEquals(0, EBML_SIGNATURE) -> true // Matroska/WebM
        peek.rangeEquals(4, MP4_FTYP) -> true // MP4 family (MP4, MOV, 3GP, etc.)
        peek.rangeEquals(0, RIFF_SIGNATURE) && peek.rangeEquals(8, AVI_SIGNATURE) -> true // AVI
        peek.rangeEquals(0, FLV_SIGNATURE) -> true // FLV
        peek.rangeEquals(0, MPEG_PROGRAM_SIGNATURE) -> true // MPEG program stream (MPG, VOB)
        peek.rangeEquals(0, ASF_SIGNATURE) -> true // ASF / WMV / WMA containers
        peek.rangeEquals(0, REAL_MEDIA_SIGNATURE) -> true // RealMedia
        peek.isMpegTransportStream() -> true // MPEG transport stream (.ts)
        else -> false
    }
}

private fun BufferedSource.isMpegTransportStream(): Boolean {
    for (packet in 0 until MPEG_TS_PACKETS_TO_CHECK) {
        val offset = packet * MPEG_TS_PACKET_SIZE
        if (!rangeEquals(offset, MPEG_TS_SYNC_BYTE)) {
            return false
        }
    }
    return true
}
