@file:JvmName("Videos")

package coil.request

import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.OPTION_CLOSEST
import android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC
import android.media.MediaMetadataRetriever.OPTION_NEXT_SYNC
import android.media.MediaMetadataRetriever.OPTION_PREVIOUS_SYNC
import androidx.annotation.FloatRange
import coil.decode.VideoFrameDecoder.Companion.VIDEO_FRAME_MICROS_KEY
import coil.decode.VideoFrameDecoder.Companion.VIDEO_FRAME_OPTION_KEY
import coil.decode.VideoFrameDecoder.Companion.VIDEO_FRAME_PERCENT_KEY

/**
 * Set the time **in milliseconds** of the frame to extract from a video.
 *
 * When both [videoFrameMicros] (or [videoFrameMillis]) and [videoFramePercent] are set,
 * [videoFrameMicros] (or [videoFrameMillis]) will take precedence.
 *
 * Default: 0
 */
fun ImageRequest.Builder.videoFrameMillis(frameMillis: Long): ImageRequest.Builder {
    return videoFrameMicros(1000 * frameMillis)
}

/**
 * Set the time **in microseconds** of the frame to extract from a video.
 *
 * When both [videoFrameMicros] (or [videoFrameMillis]) and [videoFramePercent] are set,
 * [videoFrameMicros] (or [videoFrameMillis]) will take precedence.
 *
 * Default: 0
 */
fun ImageRequest.Builder.videoFrameMicros(frameMicros: Long): ImageRequest.Builder {
    require(frameMicros >= 0) { "frameMicros must be >= 0." }
    return setParameter(VIDEO_FRAME_MICROS_KEY, frameMicros)
}

/**
 * Set the time **as a percentage** of the total duration for the frame to extract from a video.
 *
 * When both [videoFrameMicros] (or [videoFrameMillis]) and [videoFramePercent] are set,
 * [videoFrameMicros] (or [videoFrameMillis]) will take precedence.
 *
 * Default: 0.0
 */
fun ImageRequest.Builder.videoFramePercent(
    @FloatRange(from = 0.0, to = 1.0) framePercent: Double
): ImageRequest.Builder {
    require(framePercent in 0.0..1.0) { "framePercent must be in the range [0.0, 1.0]." }
    return setParameter(VIDEO_FRAME_PERCENT_KEY, framePercent)
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
fun Parameters.videoFrameMicros(): Long? = value(VIDEO_FRAME_MICROS_KEY)

/**
 * Get the time **as a percentage** of the total duration for the frame to extract from a video.
 */
fun Parameters.videoFramePercent(): Double? = value(VIDEO_FRAME_PERCENT_KEY)

/**
 * Get the option for how to decode the video frame.
 */
fun Parameters.videoFrameOption(): Int? = value(VIDEO_FRAME_OPTION_KEY)
