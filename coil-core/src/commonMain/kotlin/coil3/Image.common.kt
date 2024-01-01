package coil3

import coil3.annotation.ExperimentalCoilApi

/**
 * A platform-agnostic image class that exposes basic metadata about the underlying native
 * image representation.
 *
 * To draw the image it must be converted into its platform-specific graphics system representation.
 * See `DrawableImage` on Android and `BitmapImage` on non-Android platforms.
 */
@ExperimentalCoilApi
expect interface Image {

    /** The size of the image in memory in bytes. */
    val size: Long

    /** The width of the image in pixels. */
    val width: Int

    /** The height of the image in pixels. */
    val height: Int

    /**
     * True if the image can be shared between multiple [Target]s at the same time.
     *
     * For example, a bitmap can be shared between multiple targets if it's immutable.
     * Conversely, an animated image cannot be shared as its internal state is being mutated while
     * its animation is running.
     */
    val shareable: Boolean
}
