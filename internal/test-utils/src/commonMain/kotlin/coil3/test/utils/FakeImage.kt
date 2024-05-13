package coil3.test.utils

import coil3.Image

expect class FakeImage(
    width: Int = 100,
    height: Int = 100,
    size: Int = 4 * width * height,
    shareable: Boolean = true,
) : Image {
    override val size: Int
    override val width: Int
    override val height: Int
    override val shareable: Boolean
}

const val DEFAULT_FAKE_IMAGE_SIZE = 4L * 100 * 100
