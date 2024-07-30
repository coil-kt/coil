package coil3

import coil3.annotation.Poko
import kotlin.jvm.JvmOverloads
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorInfo
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

actual typealias Bitmap = org.jetbrains.skia.Bitmap

actual typealias Canvas = org.jetbrains.skia.Canvas

@JvmOverloads
actual fun Bitmap.asImage(shareable: Boolean): BitmapImage {
    return BitmapImage(this, shareable)
}

@JvmOverloads
actual fun Image.toBitmap(
    width: Int,
    height: Int,
): Bitmap = toBitmap(width, height, ColorType.N32, ColorAlphaType.PREMUL, null)

fun Image.toBitmap(
    width: Int,
    height: Int,
    colorType: ColorType,
    colorAlphaType: ColorAlphaType,
    colorSpace: ColorSpace?,
): Bitmap {
    if (this is BitmapImage &&
        bitmap.width == width &&
        bitmap.height == height &&
        bitmap.colorType == colorType &&
        bitmap.imageInfo.colorAlphaType == colorAlphaType &&
        bitmap.colorSpace == colorSpace
    ) {
        return bitmap
    }

    val bitmap = Bitmap()
    val imageInfo = ImageInfo(ColorInfo(colorType, colorAlphaType, colorSpace), width, height)
    check(bitmap.allocPixels(imageInfo)) { "allocPixels($imageInfo) failed" }
    val canvas = Canvas(bitmap).apply(::draw)
    check(canvas.readPixels(bitmap, 0, 0)) { "readPixels($bitmap) failed" }
    return bitmap
}

/**
 * An [Image] backed by a Skia [Bitmap].
 */
@Poko
actual class BitmapImage internal constructor(
    actual val bitmap: Bitmap,
    actual override val shareable: Boolean,
) : Image {

    actual override val size: Long
        get() {
            var size = bitmap.imageInfo.computeMinByteSize().toLong()
            if (size <= 0L) {
                // Estimate 4 bytes per pixel.
                size = 4L * bitmap.width * bitmap.height
            }
            return size.coerceAtLeast(0)
        }

    actual override val width: Int
        get() = bitmap.width

    actual override val height: Int
        get() = bitmap.height

    actual override fun draw(canvas: Canvas) {
        canvas.writePixels(bitmap, 0, 0)
    }
}
