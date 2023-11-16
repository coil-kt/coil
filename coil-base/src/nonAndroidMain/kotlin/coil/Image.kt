package coil

import org.jetbrains.skia.Image as SkiaImage

private class RealImage(
    val image: SkiaImage,
) : Image {

    override val size: Long
        get() {
            var bytesPerPixel = image.bytesPerPixel
            if (bytesPerPixel <= 0) {
                // Estimate a standard 4 bytes per pixel.
                bytesPerPixel = 4
            }
            return bytesPerPixel.toLong() * image.width * image.height
        }

    override val width: Int
        get() = image.width

    override val height: Int
        get() = image.height

    // TODO: Return false if this is an animated image type.
    override val shareable: Boolean
        get() = true
}

fun SkiaImage.asCoilImage(): Image = RealImage(this)

fun Image.asSkiaImage(): SkiaImage = (this as RealImage).image
