package coil.drawable

import android.content.res.ColorStateList
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION_CODES.Q
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import coil.decode.DecodeUtils
import coil.size.Scale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A [Drawable] that crossfades from [start] to [end].
 *
 * NOTE: The transition can only be executed once as the [start] drawable is dereferenced at the end of the transition.
 *
 * @param start The Drawable to crossfade from.
 * @param end The Drawable to crossfade to.
 * @param scale The scaling algorithm for [start] and [end].
 * @param durationMillis The duration of the crossfade animation.
 */
class CrossfadeDrawable(
    private var start: Drawable?,
    val end: Drawable,
    private val scale: Scale = Scale.FIT,
    private val durationMillis: Int = DEFAULT_DURATION
) : Drawable(), Drawable.Callback, Animatable2Compat {

    companion object {
        const val DEFAULT_DURATION = 100
    }

    private val callbacks = mutableListOf<Animatable2Compat.AnimationCallback>()

    private val width = max(start?.intrinsicWidth ?: -1, end.intrinsicWidth)
    private val height = max(start?.intrinsicHeight ?: -1, end.intrinsicHeight)

    private var startTimeMillis = 0L
    private var maxAlpha = 255
    private var isDone = false
    private var isRunning = false

    init {
        require(durationMillis > 0) { "durationMillis must be > 0." }

        start?.callback = this
        end.callback = this
    }

    override fun draw(canvas: Canvas) {
        if (!isRunning || isDone) {
            end.alpha = maxAlpha
            end.draw(canvas)
            return
        }

        val percent = (SystemClock.uptimeMillis() - startTimeMillis) / durationMillis.toDouble()
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

    override fun getAlpha() = maxAlpha

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

    @RequiresApi(LOLLIPOP)
    override fun getColorFilter(): ColorFilter? = end.colorFilter

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

    @RequiresApi(LOLLIPOP)
    override fun setTint(tintColor: Int) {
        start?.setTint(tintColor)
        end.setTint(tintColor)
    }

    @RequiresApi(LOLLIPOP)
    override fun setTintList(tint: ColorStateList?) {
        start?.setTintList(tint)
        end.setTintList(tint)
    }

    @RequiresApi(LOLLIPOP)
    override fun setTintMode(tintMode: PorterDuff.Mode?) {
        start?.setTintMode(tintMode)
        end.setTintMode(tintMode)
    }

    @RequiresApi(Q)
    override fun setTintBlendMode(blendMode: BlendMode?) {
        start?.setTintBlendMode(blendMode)
        end.setTintBlendMode(blendMode)
    }

    override fun isRunning() = isRunning

    override fun start() {
        (start as? Animatable)?.start()
        (end as? Animatable)?.start()

        if (isRunning || isDone) {
            return
        }

        isRunning = true
        startTimeMillis = SystemClock.uptimeMillis()
        callbacks.forEach { it.onAnimationStart(this) }

        invalidateSelf()
    }

    override fun stop() {
        (start as? Animatable)?.stop()
        (end as? Animatable)?.stop()

        if (!isDone) {
            markDone()
        }
    }

    override fun registerAnimationCallback(callback: Animatable2Compat.AnimationCallback) {
        callbacks.add(callback)
    }

    override fun unregisterAnimationCallback(callback: Animatable2Compat.AnimationCallback): Boolean {
        return callbacks.remove(callback)
    }

    override fun clearAnimationCallbacks() = callbacks.clear()

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
        val multiplier = DecodeUtils.computeSizeMultiplier(
            srcWidth = width.toFloat(),
            srcHeight = height.toFloat(),
            destWidth = targetWidth.toFloat(),
            destHeight = targetHeight.toFloat(),
            scale = scale
        )
        val dx = ((targetWidth - multiplier * width) / 2).roundToInt()
        val dy = ((targetHeight - multiplier * height) / 2).roundToInt()

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
        callbacks.forEach { it.onAnimationEnd(this) }
    }
}
