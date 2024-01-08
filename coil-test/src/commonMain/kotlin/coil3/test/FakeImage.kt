package coil3.test

import coil3.Image
import coil3.annotation.ExperimentalCoilApi

@ExperimentalCoilApi
expect class FakeImage(
    width: Int = 100,
    height: Int = 100,
    size: Long = 4L * width * height,
    shareable: Boolean = true,
    color: Int = 0x000000,
) : Image {
    val color: Int
}
