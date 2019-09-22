package coil.drawable

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.Q
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import coil.size.Scale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A [Drawable] that crossfades from [start] to [end].
 *
 * NOTE: The transition can only be executed once as the [start] drawable is dereferenced at the end of the transition.
 *
 * @param start The Drawable to crossfade from.
 * @param end The Drawable to crossfade to.
 * @param scale The scaling algorithm for [start] and [end].
 * @param duration The duration of the crossfade animation.
 * @param onEnd A callback for when the animation completes.
 */
class CrossfadeDrawable(
    private var start: Drawable?,
    val end: Drawable,
    private val scale: Scale = Scale.FIT,
    private val duration: Int = DEFAULT_DURATION,
    private val onEnd: (() -> Unit)? = null
) : Drawable(), Drawable.Callback, Animatable {

    companion object {
        const val DEFAULT_DURATION = 100
    }

    private val width = max(start?.intrinsicWidth ?: -1, end.intrinsicWidth)
    private val height = max(start?.intrinsicHeight ?: -1, end.intrinsicHeight)

    private var startTimeMillis = 0L
    private var maxAlpha = 255
    private var isDone = false
    private var isRunning = false

    init {
        start?.callback = this
        end.callback = this
    }

    override fun draw(canvas: Canvas) {
        if (!isRunning || isDone) {
            start = null
            end.alpha = maxAlpha
            end.draw(canvas)
            return
        }

        val percent = (SystemClock.uptimeMillis() - startTimeMillis) / duration.toDouble()
        val isDone = percent >= 1.0

        // Draw the start Drawable.
        if (!isDone) {
            start?.apply {
                alpha = maxAlpha
                draw(canvas)
            }
        }

        // Draw the end Drawable.
        end.alpha = (percent.coerceIn(0.0, 1.0) * maxAlpha).toInt()
        end.draw(canvas)

        if (isDone) {
            markDone()
        } else {
            invalidateSelf()
        }
    }

    override fun setAlpha(alpha: Int) {
        require(alpha in 0..255) { "Invalid alpha: $alpha" }
        maxAlpha = alpha
    }

    @Suppress("DEPRECATION")
    override fun getOpacity(): Int {
        val start = start
        return if (isRunning && start != null) {
            resolveOpacity(start.opacity, end.opacity)
        } else {
            end.opacity
        }
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        start?.colorFilter = colorFilter
        end.colorFilter = colorFilter
    }

    override fun onBoundsChange(bounds: Rect) {
        start?.let { updateBounds(it, bounds) }
        updateBounds(end, bounds)
    }

    override fun getIntrinsicWidth() = width

    override fun getIntrinsicHeight() = height

    override fun unscheduleDrawable(who: Drawable, what: Runnable) = unscheduleSelf(what)

    override fun invalidateDrawable(who: Drawable) = invalidateSelf()

    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) = scheduleSelf(what, `when`)

    override fun isRunning() = isRunning

    override fun start() {
        if (isRunning || isDone) {
            return
        }

        isRunning = true
        startTimeMillis = SystemClock.uptimeMillis()

        (start as? Animatable)?.start()
        (end as? Animatable)?.start()

        invalidateSelf()
    }

    override fun stop() {
        (start as? Animatable)?.stop()
        (end as? Animatable)?.stop()

        if (!isDone) {
            markDone()
        }
    }

    /** Scale and position the [Drawable] inside [targetBounds] preserving aspect ratio. */
    @VisibleForTesting
    internal fun updateBounds(drawable: Drawable, targetBounds: Rect) {
        val width = drawable.intrinsicWidth
        val height = drawable.intrinsicHeight
        if (width <= 0 || height <= 0) {
            drawable.bounds = targetBounds
            return
        }

        val targetWidth = targetBounds.width()
        val targetHeight = targetBounds.height()
        val widthPercent = targetWidth / width.toFloat()
        val heightPercent = targetHeight / height.toFloat()
        val scale = when (scale) {
            Scale.FIT -> min(widthPercent, heightPercent)
            Scale.FILL -> max(widthPercent, heightPercent)
        }
        val dx = ((targetWidth - scale * width) / 2).roundToInt()
        val dy = ((targetHeight - scale * height) / 2).roundToInt()

        val left = targetBounds.left + dx
        val top = targetBounds.top + dy
        val right = targetBounds.right - dx
        val bottom = targetBounds.bottom - dy
        drawable.setBounds(left, top, right, bottom)
    }

    private fun markDone() {
        isDone = true
        isRunning = false
        start = null
        onEnd?.invoke()
    }
}
