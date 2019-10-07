@file:Suppress("unused")
@file:JvmName("Requests")

package coil.extension

import coil.decode.VideoFrameDecoder
import coil.request.Parameters
import coil.request.RequestBuilder

private const val VIDEO_FRAME_MICROS_KEY = "coil#video_frame_micros"

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
    setParameter(VIDEO_FRAME_MICROS_KEY, frameMicros, frameMicros.toString())
}

/**
 * Get the time **in microseconds** of the frame to extract from a video.
 *
 * @see VideoFrameDecoder
 */
fun Parameters.videoFrameMicros(): Long? = value(VIDEO_FRAME_MICROS_KEY) as? Long
