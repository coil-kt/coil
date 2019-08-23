package coil.drawable

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.SystemClock
import androidx.core.graphics.withTranslation
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A [Drawable] that crossfades from [start] to [end].
 *
 * NOTE: The transition can only be executed once as the [start] drawable is dereferenced at the end of the transition.
 */
class CrossfadeDrawable(
    private var start: Drawable?,
    val end: Drawable,
    private val duration: Int = DEFAULT_DURATION,
    private val onEnd: (() -> Unit)? = null
) : Drawable(), Drawable.Callback, Animatable {

    companion object {
        const val DEFAULT_DURATION = 100
    }

    private val boundsRect = Rect()

    private var startTimeMillis = 0L
    private var maxAlpha = 255
    private var isDone = false
    private var isRunning = false

    private var startPaddingLeft = 0f
    private var startPaddingTop = 0f

    private var endPaddingLeft = 0f
    private var endPaddingTop = 0f

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
            start?.let { start ->
                canvas.withTranslation(
                    x = startPaddingLeft,
                    y = startPaddingTop
                ) {
                    start.alpha = maxAlpha
                    start.draw(canvas)
                }
            }
        }

        // Draw the end Drawable.
        canvas.withTranslation(
            x = endPaddingLeft,
            y = endPaddingTop
        ) {
            end.alpha = (percent.coerceIn(0.0, 1.0) * maxAlpha).toInt()
            end.draw(canvas)
        }

        if (isDone) {
            markDone()
        }
        invalidateSelf()
    }

    override fun setAlpha(alpha: Int) {
        require(alpha in 0..255) { "Invalid alpha: $alpha" }
        maxAlpha = alpha
    }

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
        start?.let { updateBounds(it, bounds, true) }
        updateBounds(end, bounds, false)
    }

    /** Scale and fill the [Drawable] inside [targetBounds] preserving aspect ratio. */
    private fun updateBounds(drawable: Drawable, targetBounds: Rect, isStart: Boolean) {
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
        val scale = min(widthPercent, heightPercent)
        val dx = (targetWidth - scale * width) / 2
        val dy = (targetHeight - scale * height) / 2

        boundsRect.set(targetBounds)
        boundsRect.inset(dx.roundToInt(), dy.roundToInt())
        drawable.bounds = boundsRect

        if (isStart) {
            startPaddingLeft = dx
            startPaddingTop = dy
        } else {
            endPaddingLeft = dx
            endPaddingTop = dy
        }
    }

    override fun getIntrinsicWidth(): Int {
        val start = start
        return if (isRunning && start != null) {
            max(start.intrinsicWidth, end.intrinsicWidth)
        } else {
            end.intrinsicWidth
        }
    }

    override fun getIntrinsicHeight(): Int {
        val start = start
        return if (isRunning && start != null) {
            max(start.intrinsicHeight, end.intrinsicHeight)
        } else {
            end.intrinsicHeight
        }
    }

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

    private fun markDone() {
        isDone = true
        isRunning = false
        start = null
        onEnd?.invoke()
    }
}
