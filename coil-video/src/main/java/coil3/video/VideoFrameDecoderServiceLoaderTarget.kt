package coil3.video

import coil3.util.DecoderServiceLoaderTarget

internal class VideoFrameDecoderServiceLoaderTarget : DecoderServiceLoaderTarget {
    override fun factory() = VideoFrameDecoder.Factory()
}
