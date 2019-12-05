package coil.drawable

import android.content.res.ColorStateList
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION_CODES.Q
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.core.graphics.withScale
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import coil.decode.DecodeUtils
import coil.size.Scale
import kotlin.math.max

/**
 * A [Drawable] that crossfades from [start] to [end].
 *
 * NOTE: The animation can only be executed once as the [start] drawable is dereferenced at the end of the transition.
 *
 * @param start The [Drawable] to crossfade from.
 * @param end The [Drawable] to crossfade to.
 * @param scale The scaling algorithm for [start] and [end].
 * @param durationMillis The duration of the crossfade animation.
 */
class CrossfadeDrawable(
    private var start: Drawable?,
    val end: Drawable?,
    private val scale: Scale = Scale.FIT,
    private val durationMillis: Int = DEFAULT_DURATION
) : Drawable(), Drawable.Callback, Animatable2Compat {

    companion object {
        private const val STATE_START = 0
        private const val STATE_RUNNING = 1
        private const val STATE_DONE = 2

        const val DEFAULT_DURATION = 100
    }

    private val callbacks = mutableListOf<Animatable2Compat.AnimationCallback>()

    private val intrinsicWidth = computeIntrinsicDimension(start?.intrinsicWidth, end?.intrinsicWidth)
    private val intrinsicHeight = computeIntrinsicDimension(start?.intrinsicHeight, end?.intrinsicHeight)

    private var startTimeMillis = 0L
    private var maxAlpha = 255
    private var state = STATE_START

    private var startScale = 1f
    private var startDx = 0f
    private var startDy = 0f

    private var endScale = 1f
    private var endDx = 0f
    private var endDy = 0f

    init {
        require(durationMillis > 0) { "durationMillis must be > 0." }

        start?.callback = this
        end?.callback = this
    }

    override fun draw(canvas: Canvas) {
        if (state == STATE_START) {
            start?.apply {
                alpha = maxAlpha
                canvas.withScale(startScale, startScale, startDx, startDy, ::draw)
            }
            return
        }

        if (state == STATE_DONE) {
            end?.apply {
                alpha = maxAlpha
                canvas.withScale(endScale, endScale, endDx, endDy, ::draw)
            }
            return
        }

        val percent = (SystemClock.uptimeMillis() - startTimeMillis) / durationMillis.toDouble()
        val isDone = percent >= 1.0

        // Draw the start Drawable.
        if (!isDone) {
            start?.apply {
                alpha = maxAlpha
                canvas.withScale(startScale, startScale, startDx, startDy, ::draw)
            }
        }

        // Draw the end Drawable.
        end?.apply {
            alpha = (percent.coerceIn(0.0, 1.0) * maxAlpha).toInt()
            canvas.withScale(endScale, endScale, endDx, endDy, ::draw)
        }

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
        val end = end

        if (state == STATE_START) {
            return start?.opacity ?: PixelFormat.TRANSPARENT
        }

        if (state == STATE_DONE) {
            return end?.opacity ?: PixelFormat.TRANSPARENT
        }

        return when {
            start != null && end != null -> resolveOpacity(start.opacity, end.opacity)
            start != null -> start.opacity
            end != null -> end.opacity
            else -> PixelFormat.TRANSPARENT
        }
    }

    @RequiresApi(LOLLIPOP)
    override fun getColorFilter(): ColorFilter? = when (state) {
        STATE_START -> start?.colorFilter
        STATE_RUNNING -> end?.colorFilter ?: start?.colorFilter
        STATE_DONE -> end?.colorFilter
        else -> null
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        start?.colorFilter = colorFilter
        end?.colorFilter = colorFilter
    }

    override fun onBoundsChange(bounds: Rect) {
        val start = start
        if (start != null) {
            start.bounds = bounds

            val width = start.intrinsicWidth
            val height = start.intrinsicHeight
            if (width <= 0 || height <= 0) {
                startScale = 1f
                startDx = 0f
                startDy = 0f
            } else {
                val targetWidth = bounds.width()
                val targetHeight = bounds.height()
                startScale = DecodeUtils.computeSizeMultiplier(width, height, targetWidth, targetHeight, scale).toFloat()
                startDx = (targetWidth - startScale * width) / 2
                startDy = (targetHeight - startScale * height) / 2
            }
        } else {
            startScale = 1f
            startDx = 0f
            startDy = 0f
        }

        val end = end
        if (end != null) {
            end.bounds = bounds

            val width = end.intrinsicWidth
            val height = end.intrinsicHeight
            if (width <= 0 || height <= 0) {
                endScale = 1f
                endDx = 0f
                endDy = 0f
            } else {
                val targetWidth = bounds.width()
                val targetHeight = bounds.height()
                endScale = DecodeUtils.computeSizeMultiplier(width, height, targetWidth, targetHeight, scale).toFloat()
                endDx = (targetWidth - endScale * width) / 2
                endDy = (targetHeight - endScale * height) / 2
            }
        } else {
            endScale = 1f
            endDx = 0f
            endDy = 0f
        }
    }

    override fun getIntrinsicWidth() = intrinsicWidth

    override fun getIntrinsicHeight() = intrinsicHeight

    override fun unscheduleDrawable(who: Drawable, what: Runnable) = unscheduleSelf(what)

    override fun invalidateDrawable(who: Drawable) = invalidateSelf()

    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) = scheduleSelf(what, `when`)

    @RequiresApi(LOLLIPOP)
    override fun setTint(tintColor: Int) {
        start?.setTint(tintColor)
        end?.setTint(tintColor)
    }

    @RequiresApi(LOLLIPOP)
    override fun setTintList(tint: ColorStateList?) {
        start?.setTintList(tint)
        end?.setTintList(tint)
    }

    @RequiresApi(LOLLIPOP)
    override fun setTintMode(tintMode: PorterDuff.Mode?) {
        start?.setTintMode(tintMode)
        end?.setTintMode(tintMode)
    }

    @RequiresApi(Q)
    override fun setTintBlendMode(blendMode: BlendMode?) {
        start?.setTintBlendMode(blendMode)
        end?.setTintBlendMode(blendMode)
    }

    override fun isRunning() = state == STATE_RUNNING

    override fun start() {
        (start as? Animatable)?.start()
        (end as? Animatable)?.start()

        if (state != STATE_START) {
            return
        }

        state = STATE_RUNNING
        startTimeMillis = SystemClock.uptimeMillis()
        callbacks.forEach { it.onAnimationStart(this) }

        invalidateSelf()
    }

    override fun stop() {
        (start as? Animatable)?.stop()
        (end as? Animatable)?.stop()

        if (state != STATE_DONE) {
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

    private fun computeIntrinsicDimension(startSize: Int?, endSize: Int?): Int {
        return if (startSize == -1 || endSize == -1) -1 else max(startSize ?: -1, endSize ?: -1)
    }

    private fun markDone() {
        state = STATE_DONE
        start = null
        callbacks.forEach { it.onAnimationEnd(this) }
    }
}
