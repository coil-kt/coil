@file:Suppress("unused")
@file:JvmName("Videos")

package coil.request

import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.OPTION_CLOSEST
import android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC
import android.media.MediaMetadataRetriever.OPTION_NEXT_SYNC
import android.media.MediaMetadataRetriever.OPTION_PREVIOUS_SYNC
import coil.decode.VideoFrameDecoder.Companion.VIDEO_FRAME_MICROS_KEY
import coil.decode.VideoFrameDecoder.Companion.VIDEO_FRAME_OPTION_KEY

/**
 * Set the time **in milliseconds** of the frame to extract from a video.
 *
 * Default: 0
 */
fun ImageRequest.Builder.videoFrameMillis(frameMillis: Long): ImageRequest.Builder {
    return videoFrameMicros(1000 * frameMillis)
}

/**
 * Set the time **in microseconds** of the frame to extract from a video.
 *
 * Default: 0
 */
fun ImageRequest.Builder.videoFrameMicros(frameMicros: Long): ImageRequest.Builder {
    require(frameMicros >= 0) { "frameMicros must be >= 0." }
    return setParameter(VIDEO_FRAME_MICROS_KEY, frameMicros)
}

/**
 * Set the option for how to decode the video frame.
 *
 * Must be one of [OPTION_PREVIOUS_SYNC], [OPTION_NEXT_SYNC], [OPTION_CLOSEST_SYNC], [OPTION_CLOSEST].
 *
 * Default: [OPTION_CLOSEST_SYNC]
 *
 * @see MediaMetadataRetriever
 */
fun ImageRequest.Builder.videoFrameOption(option: Int): ImageRequest.Builder {
    require(option == OPTION_PREVIOUS_SYNC ||
        option == OPTION_NEXT_SYNC ||
        option == OPTION_CLOSEST_SYNC ||
        option == OPTION_CLOSEST) { "Invalid video frame option: $option." }
    return setParameter(VIDEO_FRAME_OPTION_KEY, option)
}

/**
 * Get the time **in microseconds** of the frame to extract from a video.
 */
fun Parameters.videoFrameMicros(): Long? = value(VIDEO_FRAME_MICROS_KEY) as Long?

/**
 * Get the option for how to decode the video frame.
 */
fun Parameters.videoFrameOption(): Int? = value(VIDEO_FRAME_OPTION_KEY) as Int?
