@file:Suppress("unused")

package coil.request

import coil.decode.VideoFrameDecoder

/**
 * Set the time (in milliseconds) of the frame to extract from a video.
 *
 * @see VideoFrameDecoder
 */
fun RequestBuilder<*>.videoFrame(frameMillis: Long) {
    require(frameMillis >= 0) { "frameMillis must be >= 0" }
    TODO()
}
