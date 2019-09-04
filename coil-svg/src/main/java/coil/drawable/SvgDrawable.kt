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
import android.os.Build.VERSION_CODES.M
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
        // Lazily render a new picture.
        val bounds = checkNotNull(currentBounds)
        val picture = picture ?: svg.renderToPicture(bounds.width(), bounds.height()).also { picture = it }

        // Hardware canvases don't support rendering pictures before API 23.
        // If we're on pre-23, render the picture on a software canvas first.
        if (SDK_INT >= M) {
            picture.draw(canvas)
        } else {
            val softwareCanvas = checkNotNull(softwareCanvas)
            val softwareBitmap = checkNotNull(softwareBitmap)

            softwareCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            picture.draw(softwareCanvas)
            canvas.drawBitmap(softwareBitmap, 0f, 0f, paint)
        }
    }

    override fun setAlpha(alpha: Int) {}

    override fun getOpacity() = PixelFormat.TRANSLUCENT

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    override fun onBoundsChange(bounds: Rect) {
        if (currentBounds == bounds) {
            return
        }
        currentBounds = bounds

        val width = bounds.width()
        val height = bounds.height()

        // Invalidate the current picture.
        picture = null

        // We don't need to allocate a software canvas pre-23.
        if (SDK_INT >= M) {
            val bitmap = pool.get(width, height, config)
            softwareBitmap?.let(pool::put)
            softwareBitmap = bitmap
            softwareCanvas = Canvas(bitmap)
        }
    }

    override fun getIntrinsicWidth() = svg.documentWidth.toInt()

    override fun getIntrinsicHeight() = svg.documentHeight.toInt()
}
