package coil3.util

import coil3.decode.VideoFrameDecoder

internal class VideoFrameDecoderServiceLoaderTarget : DecoderServiceLoaderTarget {
    override fun factory() = VideoFrameDecoder.Factory()
}
