package coil

interface Image {

    /** The size of the image in memory in bytes. */
    val size: Long

    /** The width of the image in pixels. */
    val width: Int

    /** The height of the image in pixels. */
    val height: Int

    /**
     * True if the image can be shared between multiple [Target]s at the same time.
     *
     * For example, a bitmap can be shared between multiple targets if it's not mutated.
     * Conversely, an animated image cannot be shared as its internal state is being mutated while
     * being played.
     */
    val shareable: Boolean
}
