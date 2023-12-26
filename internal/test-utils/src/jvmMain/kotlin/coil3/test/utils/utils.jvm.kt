package coil3.test.utils

import coil3.PlatformContext
import java.io.File
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.impl.use

fun PlatformContext.decodeBitmapAsset(
    path: String,
): Bitmap {
    // Retry multiple times as the emulator can be flaky.
    var failures = 0
    while (true) {
        try {
            val file = File(path)
            val image = Image.makeFromEncoded(file.readBytes())
            val bitmap = Bitmap()
            bitmap.allocN32Pixels(image.width, image.height)
            Canvas(bitmap).use { canvas ->
                canvas.drawImageRect(
                    image = image,
                    src = Rect.makeWH(image.width.toFloat(), image.height.toFloat()),
                    dst = Rect.makeWH(image.width.toFloat(), image.height.toFloat()),
                )
            }
            return bitmap
        } catch (e: Exception) {
            if (failures++ > 5) throw e
        }
    }
}
