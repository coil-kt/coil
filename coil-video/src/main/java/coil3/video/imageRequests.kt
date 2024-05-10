package coil3.video

import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.OPTION_CLOSEST
import android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC
import android.media.MediaMetadataRetriever.OPTION_NEXT_SYNC
import android.media.MediaMetadataRetriever.OPTION_PREVIOUS_SYNC
import androidx.annotation.RequiresApi
import coil3.Extras
import coil3.getExtra
import coil3.request.ImageRequest
import coil3.request.Options

// region videoFrameIndex

/**
 * Set the frame index to extract from a video.
 *
 * When both [videoFrameIndex] and other videoFrame-prefixed properties are set,
 * [videoFrameIndex] will take precedence.
 */
@RequiresApi(28)
fun ImageRequest.Builder.videoFrameIndex(frameIndex: Int) = apply {
    require(frameIndex >= 0) { "frameIndex must be >= 0." }
    memoryCacheKeyExtra("coil#videoFrameIndex", frameIndex.toString())
    extras[videoFrameIndexKey] = frameIndex
}

val ImageRequest.videoFrameIndex: Int
    @RequiresApi(28)
    get() = getExtra(videoFrameIndexKey)

val Options.videoFrameIndex: Int
    @RequiresApi(28)
    get() = getExtra(videoFrameIndexKey)

val Extras.Key.Companion.videoFrameIndex: Extras.Key<Int>
    @RequiresApi(28)
    get() = videoFrameIndexKey

private val videoFrameIndexKey = Extras.Key(default = -1)

// endregion
// region videoFrameMicros

/**
 * Set the time **in milliseconds** of the frame to extract from a video.
 *
 * When both [videoFrameMicros] (or [videoFrameMillis]) and [videoFramePercent] are set,
 * [videoFrameMicros] (or [videoFrameMillis]) will take precedence.
 */
fun ImageRequest.Builder.videoFrameMillis(frameMillis: Long) =
    videoFrameMicros(1000 * frameMillis)

/**
 * Set the time **in microseconds** of the frame to extract from a video.
 *
 * When both [videoFrameMicros] (or [videoFrameMillis]) and [videoFramePercent] are set,
 * [videoFrameMicros] (or [videoFrameMillis]) will take precedence.
 */
fun ImageRequest.Builder.videoFrameMicros(frameMicros: Long) = apply {
    require(frameMicros >= 0) { "frameMicros must be >= 0." }
    memoryCacheKeyExtra("coil#videoFrameMicros", frameMicros.toString())
    extras[videoFrameMicrosKey] = frameMicros
}

val ImageRequest.videoFrameMicros: Long
    get() = getExtra(videoFrameMicrosKey)

val Options.videoFrameMicros: Long
    get() = getExtra(videoFrameMicrosKey)

val Extras.Key.Companion.videoFrameMicros: Extras.Key<Long>
    get() = videoFrameMicrosKey

private val videoFrameMicrosKey = Extras.Key(default = -1L)

// endregion
// region videoFramePercent

/**
 * Set the time **as a percentage** of the total duration for the frame to extract from a video.
 *
 * When both [videoFrameMicros] (or [videoFrameMillis]) and [videoFramePercent] are set,
 * [videoFrameMicros] (or [videoFrameMillis]) will take precedence.
 */
fun ImageRequest.Builder.videoFramePercent(framePercent: Double) = apply {
    require(framePercent in 0.0..1.0) { "framePercent must be in the range [0.0, 1.0]." }
    memoryCacheKeyExtra("coil#videoFramePercent", framePercent.toString())
    extras[videoFramePercentKey] = framePercent
}

val ImageRequest.videoFramePercent: Double
    get() = getExtra(videoFramePercentKey)

val Options.videoFramePercent: Double
    get() = getExtra(videoFramePercentKey)

val Extras.Key.Companion.videoFramePercent: Extras.Key<Double>
    get() = videoFramePercentKey

private val videoFramePercentKey = Extras.Key(default = -1.0)

// endregion
// region videoFrameOption

/**
 * Set the option for how to decode the video frame.
 *
 * Must be one of [OPTION_PREVIOUS_SYNC], [OPTION_NEXT_SYNC], [OPTION_CLOSEST_SYNC], [OPTION_CLOSEST].
 *
 * @see MediaMetadataRetriever
 */
fun ImageRequest.Builder.videoFrameOption(option: Int) = apply {
    require(option == OPTION_PREVIOUS_SYNC ||
        option == OPTION_NEXT_SYNC ||
        option == OPTION_CLOSEST_SYNC ||
        option == OPTION_CLOSEST) { "Invalid video frame option: $option." }
    memoryCacheKeyExtra("coil#videoFrameOption", option.toString())
    extras[videoFrameOptionKey] = option
}

val ImageRequest.videoFrameOption: Int
    get() = getExtra(videoFrameOptionKey)

val Options.videoFrameOption: Int
    get() = getExtra(videoFrameOptionKey)

val Extras.Key.Companion.videoFrameOption: Extras.Key<Int>
    get() = videoFrameOptionKey

private val videoFrameOptionKey = Extras.Key(default = OPTION_CLOSEST_SYNC)

// endregion
