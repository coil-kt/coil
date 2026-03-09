package coil3.test.utils

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Image
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.impl.use

actual fun decodeBitmapResource(
    path: String,
): Bitmap {
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
                    srcLeft = 0f,
                    srcTop = 0f,
                    srcRight = image.width.toFloat(),
                    srcBottom = image.height.toFloat(),
                    dstLeft = 0f,
                    dstTop = 0f,
                    dstRight = image.width.toFloat(),
                    dstBottom = image.height.toFloat(),
                    samplingMode = SamplingMode.DEFAULT,
                    paint = null,
                    strict = true,
                )
            }
            return bitmap
        } catch (e: Exception) {
            if (failures++ > 5) throw e
        }
    }
}
