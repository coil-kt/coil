package coil3.gif.internal

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

internal class FrameBitmapPool(imageInfo: ImageInfo, capacity: Int) : AutoCloseable {

    private val slots = ArrayList<Slot>(capacity)
    private val available = ArrayDeque<Slot>(capacity)

    init {
        try {
            repeat(capacity) { slots += Slot(imageInfo) }
            available.addAll(slots)
        } catch (throwable: Throwable) {
            close()
            throw throwable
        }
    }

    fun take(): Slot? = available.removeFirstOrNull()

    fun recycle(slot: Slot) {
        available.addLast(slot)
    }

    override fun close() {
        slots.forEach(Slot::close)
        available.clear()
        slots.clear()
    }

    class Slot(imageInfo: ImageInfo) : AutoCloseable {

        val bitmap = allocateBitmap(imageInfo)
        private var image = try {
            bitmap.setImmutable()
            Image.makeFromBitmap(bitmap)
        } catch (throwable: Throwable) {
            bitmap.close()
            throw throwable
        }

        fun publish(source: Bitmap?): Image {
            val alphaChanged = if (source != null) {
                bitmap.updateAlphaType(source.imageInfo.colorAlphaType)
                source.scalePixelsTo(bitmap)
                image.imageInfo.colorAlphaType != bitmap.imageInfo.colorAlphaType
            } else {
                false
            }
            bitmap.notifyPixelsChanged()
            if (alphaChanged) {
                // Skia captures the alpha type when the image is created.
                val previousImage = image
                image = Image.makeFromBitmap(bitmap)
                previousImage.close()
            }
            return image
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
