package coil

import org.jetbrains.skia.Image as SkiaImage

private class WrappedSkiaImage(
    val image: SkiaImage,
    override val shareable: Boolean,
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
}

fun SkiaImage.asCoilImage(shareable: Boolean): Image {
    return WrappedSkiaImage(this, shareable)
}

val Image.image: SkiaImage
    get() = (this as WrappedSkiaImage).image
