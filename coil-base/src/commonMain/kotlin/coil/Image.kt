package coil

interface Image {

    /** The size of the image in memory in bytes. */
    val size: Long

    /** The width of the image in pixels. */
    val width: Int

    /** The height of the image in pixels. */
    val height: Int

    /**
     * True if the image can be shared between multiple [Target]s.
     *
     * For example, an Android bitmap can be shared between multiple targets if it's not mutated.
     * Conversely, a crossfade drawable cannot be shared as its alpha is being mutated frequently.
     */
    val shareable: Boolean
}
