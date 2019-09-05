package coil.drawable

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Picture
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import coil.bitmappool.BitmapPool
import com.caverock.androidsvg.SVG

/** A [Drawable] that supports rendering [SVG]s. */
class SvgDrawable(
    private val svg: SVG,
    private val config: Bitmap.Config,
    private val pool: BitmapPool
) : Drawable() {

    init {
        require(SDK_INT < O || config != Bitmap.Config.HARDWARE) { "Bitmap config must not be hardware." }
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private var currentBounds: Rect? = null
    private var picture: Picture? = null

    private var softwareCanvas: Canvas? = null
    private var softwareBitmap: Bitmap? = null

    override fun draw(canvas: Canvas) {
        val bounds = checkNotNull(currentBounds)
        val width = bounds.width()
        val height = bounds.height()

        // Lazily render a new picture.
        val picture = picture ?: svg.renderToPicture(width, height).also { picture = it }

        if (!canvas.isHardwareAccelerated && paint.alpha == 255 && paint.colorFilter == null) {
            // Fast path: draw directly on the given software canvas.
            picture.draw(canvas)
        } else {
            // Slow path: lazily create a new software canvas.
            val softwareBitmap = softwareBitmap ?: pool.get(width, height, config).also { softwareBitmap = it }
            val softwareCanvas = softwareCanvas ?: Canvas(softwareBitmap).also { softwareCanvas = it }

            // Draw the SVG on the private software canvas first.
            softwareCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            picture.draw(softwareCanvas)
            canvas.drawBitmap(softwareBitmap, 0f, 0f, paint)
        }
    }

    override fun setAlpha(alpha: Int) {
        require(alpha in 0..255) { "Invalid alpha: $alpha" }
        paint.alpha = alpha
    }

    override fun getOpacity() = PixelFormat.TRANSLUCENT

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun onBoundsChange(bounds: Rect) {
        if (currentBounds == bounds) {
            return
        }
        currentBounds = bounds

        // Pool the current bitmap.
        softwareBitmap?.let(pool::put)

        // Invalidate everything.
        picture = null
        softwareBitmap = null
        softwareCanvas = null
    }

    override fun getIntrinsicWidth() = svg.documentWidth.toInt()

    override fun getIntrinsicHeight() = svg.documentHeight.toInt()
}
