package coil3.svg.skia

import coil3.Canvas
import android.graphics.Bitmap as AndroidBitmap
import android.graphics.Paint
import org.jetbrains.skia.Bitmap as SkiaBitmap
import org.jetbrains.skia.ImageInfo
import androidx.core.graphics.createBitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.jetbrains.skia.Surface

internal actual fun render(image: SvgImage, canvas: Canvas) {
    val surface = Surface.makeRasterN32Premul(image.width, image.height)

    image.svg.render(surface.canvas)

    val skiaBitmap = SkiaBitmap()
    skiaBitmap.allocPixels(ImageInfo.makeN32Premul(image.width, image.height))

    surface.readPixels(skiaBitmap, 0, 0)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(skiaBitmap.toAndroidBitmap(), 0f, 0f, paint)
}

private fun SkiaBitmap.toAndroidBitmap(): AndroidBitmap {
    val pixels = checkNotNull(readPixels()) { "Failed to read pixels from Skiko bitmap" }
    val buffer = ByteBuffer.wrap(pixels).order(ByteOrder.nativeOrder())

    return createBitmap(width, height).apply {
        copyPixelsFromBuffer(buffer)
    }
}
