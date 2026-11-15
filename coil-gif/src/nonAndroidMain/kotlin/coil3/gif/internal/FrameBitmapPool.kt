package coil3.gif.internal

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
