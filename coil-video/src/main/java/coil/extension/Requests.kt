@file:Suppress("unused")
@file:JvmName("Requests")

package coil.extension

import coil.decode.VideoFrameDecoder
import coil.request.RequestBuilder

/**
 * Set the time **in milliseconds** of the frame to extract from a video.
 *
 * @see VideoFrameDecoder
 */
fun RequestBuilder<*>.videoFrameMillis(frameMillis: Long) = videoFrameMicros(1000 * frameMillis)

/**
 * Set the time **in microseconds** of the frame to extract from a video.
 *
 * @see VideoFrameDecoder
 */
fun RequestBuilder<*>.videoFrameMicros(frameMicros: Long) {
    require(frameMicros >= 0) { "frameMicros must be >= 0" }
    setParameter(VideoFrameDecoder.VIDEO_FRAME_MICROS_KEY, frameMicros, frameMicros.toString())
}
