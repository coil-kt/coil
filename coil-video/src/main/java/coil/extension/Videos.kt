@file:Suppress("unused")
@file:JvmName("Videos")

package coil.extension

import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.OPTION_CLOSEST
import android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC
import android.media.MediaMetadataRetriever.OPTION_NEXT_SYNC
import android.media.MediaMetadataRetriever.OPTION_PREVIOUS_SYNC
import coil.fetch.VideoFrameFetcher
import coil.fetch.VideoFrameFetcher.Companion.VIDEO_FRAME_MICROS_KEY
import coil.fetch.VideoFrameFetcher.Companion.VIDEO_FRAME_OPTION_KEY
import coil.request.Parameters
import coil.request.RequestBuilder

/**
 * Set the time **in milliseconds** of the frame to extract from a video.
 *
 * Default: 0
 *
 * @see VideoFrameFetcher
 */
fun RequestBuilder<*>.videoFrameMillis(frameMillis: Long) = videoFrameMicros(1000 * frameMillis)

/**
 * Set the time **in microseconds** of the frame to extract from a video.
 *
 * Default: 0
 *
 * @see VideoFrameFetcher
 */
fun RequestBuilder<*>.videoFrameMicros(frameMicros: Long) {
    require(frameMicros >= 0) { "frameMicros must be >= 0" }
    setParameter(VIDEO_FRAME_MICROS_KEY, frameMicros, frameMicros.toString())
}

/**
 * Set the option for how to decode the video frame.
 *
 * Must be one of [OPTION_PREVIOUS_SYNC], [OPTION_NEXT_SYNC], [OPTION_CLOSEST_SYNC], [OPTION_CLOSEST].
 *
 * Default: [OPTION_CLOSEST_SYNC]
 *
 * @see MediaMetadataRetriever
 * @see VideoFrameFetcher
 */
fun RequestBuilder<*>.videoFrameOption(option: Int) {
    require(option == OPTION_PREVIOUS_SYNC ||
        option == OPTION_NEXT_SYNC ||
        option == OPTION_CLOSEST_SYNC ||
        option == OPTION_CLOSEST) { "Invalid video frame option: $option." }
    setParameter(VIDEO_FRAME_OPTION_KEY, option, option.toString())
}

/**
 * Get the time **in microseconds** of the frame to extract from a video.
 *
 * @see VideoFrameFetcher
 */
fun Parameters.videoFrameMicros(): Long? = value(VIDEO_FRAME_MICROS_KEY) as Long?

/**
 * Get the option for how to decode the video frame.
 *
 * @see VideoFrameFetcher
 */
fun Parameters.videoFrameOption(): Int? = value(VIDEO_FRAME_OPTION_KEY) as Int?
