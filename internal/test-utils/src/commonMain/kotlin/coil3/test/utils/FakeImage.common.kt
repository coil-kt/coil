package coil3.test.utils

import coil3.Image

expect class FakeImage(
    width: Int = 100,
    height: Int = 100,
    size: Long = 4L * width * height,
    shareable: Boolean = true,
) : Image

const val DEFAULT_FAKE_IMAGE_SIZE = 4L * 100 * 100
