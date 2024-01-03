package coil3.test.utils

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.impl.use

actual fun decodeBitmapResource(
    path: String,
): CoilBitmap {
    // Retry multiple times as the emulator can be flaky.
    var failures = 0
    while (true) {
        try {
            val source = FileSystem.RESOURCES.source(path.toPath())
            val image = Image.makeFromEncoded(source.buffer().readByteArray())
            val bitmap = Bitmap()
            bitmap.allocN32Pixels(image.width, image.height)
            Canvas(bitmap).use { canvas ->
                canvas.drawImageRect(
                    image = image,
                    src = Rect.makeWH(image.width.toFloat(), image.height.toFloat()),
                    dst = Rect.makeWH(image.width.toFloat(), image.height.toFloat()),
                )
            }
            return bitmap.toCoilBitmap()
        } catch (e: Exception) {
            if (failures++ > 5) throw e
        }
    }
}
