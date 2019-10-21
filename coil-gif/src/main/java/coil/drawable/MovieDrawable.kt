@file:Suppress("DEPRECATION", "unused")
@file:SuppressLint("SupportAnnotationUsage")

package coil.drawable

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Movie
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import android.os.SystemClock
import androidx.annotation.Px
import androidx.core.graphics.withScale
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import coil.bitmappool.BitmapPool
import coil.decode.ImageDecoderDecoder
import coil.size.Scale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * A [Drawable] that supports rendering [Movie]s (i.e. GIFs).
 *
 * NOTE: Prefer using [ImageDecoderDecoder] and [AnimatedImageDrawable] on Android P and above.
 */
class MovieDrawable(
    private val movie: Movie,
    private val config: Bitmap.Config,
    private val scale: Scale,
    private val pool: BitmapPool
) : Drawable(), Animatable2Compat {

    companion object {
        /** Pass this to [setRepeatCount] to repeat infinitely. */
        const val REPEAT_INFINITE = -1
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private val callbacks = mutableListOf<Animatable2Compat.AnimationCallback>()

    private var currentBounds: Rect? = null
    private var softwareCanvas: Canvas? = null
    private var softwareBitmap: Bitmap? = null

    private var softwareScale = 1f
    private var hardwareScale = 1f
    private var hardwareDx = 0f
    private var hardwareDy = 0f

    private var isRunning = false
    private var startTimeMillis = 0L
    private var frameTimeMillis = 0L

    private var repeatCount = REPEAT_INFINITE
    private var loopIteration = 0

    init {
        require(SDK_INT < O || config != Bitmap.Config.HARDWARE) { "Bitmap config must not be hardware." }
    }

    override fun draw(canvas: Canvas) {
        val softwareCanvas = checkNotNull(softwareCanvas)
        val softwareBitmap = checkNotNull(softwareBitmap)

        val invalidate: Boolean
        val time: Int
        val duration = movie.duration()
        if (duration == 0) {
            invalidate = false
            time = 0
        } else {
            if (isRunning) {
                frameTimeMillis = SystemClock.uptimeMillis()
            }
            val elapsedTime = (frameTimeMillis - startTimeMillis).toInt()
            loopIteration = elapsedTime / duration
            invalidate = repeatCount == REPEAT_INFINITE || loopIteration <= repeatCount
            time = if (invalidate) elapsedTime - loopIteration * duration else duration
        }
        movie.setTime(time)

        // Clear the software canvas.
        softwareCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        // Draw onto a software canvas first.
        softwareCanvas.withScale(
            x = softwareScale,
            y = softwareScale
        ) {
            movie.draw(this, 0f, 0f, paint)
        }

        // Draw onto the input canvas (may or may not be hardware).
        canvas.withScale(
            x = hardwareScale,
            y = hardwareScale,
            pivotX = hardwareDx,
            pivotY = hardwareDy
        ) {
            drawBitmap(softwareBitmap, 0f, 0f, paint)
        }

        if (isRunning && invalidate) {
            invalidateSelf()
        } else {
            stop()
        }
    }

    /**
     * Set the number of times to repeat the animation.
     *
     * If the animation is already running, any iterations that have already occurred will count towards the new count.
     *
     * NOTE: This method matches the behavior of [AnimatedImageDrawable.setRepeatCount]. i.e. setting [repeatCount] to 2 will
     * result in the animation playing 3 times. Setting [repeatCount] to 0 will result in the animation playing once.
     *
     * Default: [REPEAT_INFINITE]
     */
    fun setRepeatCount(repeatCount: Int) {
        require(repeatCount >= REPEAT_INFINITE) { "Invalid repeatCount: $repeatCount" }
        this.repeatCount = repeatCount
    }

    /** Get the number of times the animation will repeat. */
    fun getRepeatCount(): Int = repeatCount

    override fun setAlpha(alpha: Int) {
        require(alpha in 0..255) { "Invalid alpha: $alpha" }
        paint.alpha = alpha
    }

    override fun getOpacity(): Int {
        return if (paint.alpha == 255 && movie.isOpaque) {
            PixelFormat.OPAQUE
        } else {
            PixelFormat.TRANSLUCENT
        }
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun onBoundsChange(bounds: Rect) {
        if (currentBounds == bounds) {
            return
        }
        currentBounds = bounds

        val boundsWidth = bounds.width().toFloat()
        val boundsHeight = bounds.height().toFloat()

        val movieWidth = movie.width().toFloat()
        val movieHeight = movie.height().toFloat()

        softwareScale = computeScale(movieWidth, movieHeight, boundsWidth, boundsHeight)
        val bitmapWidth = ceil(softwareScale * movieWidth)
        val bitmapHeight = ceil(softwareScale * movieHeight)

        val bitmap = pool.get(bitmapWidth.toInt(), bitmapHeight.toInt(), config)
        softwareBitmap?.let(pool::put)
        softwareBitmap = bitmap
        softwareCanvas = Canvas(bitmap)

        hardwareScale = computeScale(bitmapWidth, bitmapHeight, boundsWidth, boundsHeight)
        hardwareDx = (boundsWidth - bitmapWidth / hardwareScale) / 2
        hardwareDy = (boundsHeight - bitmapHeight / hardwareScale) / 2
    }

    private fun computeScale(
        @Px srcWidth: Float,
        @Px srcHeight: Float,
        @Px destWidth: Float,
        @Px destHeight: Float
    ): Float {
        val bitmapWidthPercent = srcWidth / min(destWidth, srcWidth)
        val bitmapHeightPercent = srcHeight / min(destHeight, srcHeight)
        return when (scale) {
            Scale.FILL -> max(bitmapWidthPercent, bitmapHeightPercent)
            Scale.FIT -> min(bitmapWidthPercent, bitmapHeightPercent)
        }
    }

    override fun getIntrinsicWidth() = movie.width()

    override fun getIntrinsicHeight() = movie.height()

    override fun isRunning() = isRunning

    override fun start() {
        if (isRunning) {
            return
        }

        isRunning = true
        loopIteration = 0
        startTimeMillis = SystemClock.uptimeMillis()
        callbacks.forEach { it.onAnimationStart(this) }

        invalidateSelf()
    }

    override fun stop() {
        if (!isRunning) {
            return
        }

        isRunning = false
        callbacks.forEach { it.onAnimationEnd(this) }
    }

    override fun registerAnimationCallback(callback: Animatable2Compat.AnimationCallback) {
        callbacks.add(callback)
    }

    override fun unregisterAnimationCallback(callback: Animatable2Compat.AnimationCallback): Boolean {
        return callbacks.remove(callback)
    }

    override fun clearAnimationCallbacks() = callbacks.clear()
}
