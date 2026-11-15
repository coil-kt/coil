package coil3.gif.internal

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

internal class BitmapSwapChain(imageInfo: ImageInfo) : AutoCloseable {

    private val first = Slot(imageInfo)
    private val second = try {
        Slot(imageInfo)
    } catch (throwable: Throwable) {
        first.close()
        throw throwable
    }

    fun decodeSlot(renderImage: Image?): Slot = if (renderImage === first.image) second else first

    override fun close() {
        try {
            second.close()
        } finally {
            first.close()
        }
    }

    class Slot(imageInfo: ImageInfo) : AutoCloseable {

        val bitmap = allocateBitmap(imageInfo)
        val image = try {
            bitmap.setImmutable()
            Image.makeFromBitmap(bitmap)
        } catch (throwable: Throwable) {
            bitmap.close()
            throw throwable
        }

        override fun close() {
            try {
                image.close()
            } finally {
                bitmap.close()
            }
        }
    }
}
