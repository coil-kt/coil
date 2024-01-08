package coil3.video.internal

import coil3.util.DecoderServiceLoaderTarget
import coil3.video.VideoFrameDecoder

internal class VideoFrameDecoderServiceLoaderTarget : DecoderServiceLoaderTarget {
    override fun factory() = VideoFrameDecoder.Factory()
}
