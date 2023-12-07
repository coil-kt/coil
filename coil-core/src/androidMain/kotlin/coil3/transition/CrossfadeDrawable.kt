package coil3.transition

import android.content.res.ColorStateList
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.core.graphics.withSave
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import coil3.decode.DecodeUtils
import coil3.request.DEFAULT_CROSSFADE_MILLIS
import coil3.size.Scale
import coil3.util.forEachIndices
import kotlin.math.roundToInt

/**
 * A [Drawable] that crossfades from [start] to [end].
 *
 * NOTE: The animation can only be executed once as the [start]
 * drawable is dereferenced at the end of the transition.
 *
 * @param start The [Drawable] to crossfade from.
 * @param end The [Drawable] to crossfade to.
 * @param scale The scaling algorithm for [start] and [end].
 * @param durationMillis The duration of the crossfade animation.
 * @param fadeStart If false, the start drawable will not fade out while the end drawable fades in.
 * @param preferExactIntrinsicSize If true, this drawable's intrinsic width/height will only be -1
 *  if [start] **and** [end] return -1 for that dimension. If false, the intrinsic width/height will
 *  be -1 if [start] **or** [end] return -1 for that dimension. This is useful for views that
 *  require an exact intrinsic size to scale the drawable.
 */
class CrossfadeDrawable @JvmOverloads constructor(
    start: Drawable?,
    end: Drawable?,
    val scale: Scale = Scale.FIT,
    val durationMillis: Int = DEFAULT_DURATION,
    val fadeStart: Boolean = true,
    val preferExactIntrinsicSize: Boolean = false,
) : Drawable(), Drawable.Callback, Animatable2Compat {

    private val callbacks = mutableListOf<Animatable2Compat.AnimationCallback>()

    private val intrinsicWidth = computeIntrinsicDimension(start?.intrinsicWidth, end?.intrinsicWidth)
    private val intrinsicHeight = computeIntrinsicDimension(start?.intrinsicHeight, end?.intrinsicHeight)

    private var startTimeMillis = 0L
    private var maxAlpha = 255
    private var state = STATE_START

    var start: Drawable? = start?.mutate()
        private set
    val end: Drawable? = end?.mutate()

    init {
        require(durationMillis > 0) { "durationMillis must be > 0." }

        this.start?.callback = this
        this.end?.callback = this
    }

    override fun draw(canvas: Canvas) {
        if (state == STATE_START) {
            start?.apply {
                alpha = maxAlpha
                canvas.withSave { draw(canvas) }
            }
            return
        }

        if (state == STATE_DONE) {
            end?.apply {
                alpha = maxAlpha
                canvas.withSave { draw(canvas) }
            }
            return
        }

        val percent = (SystemClock.uptimeMillis() - startTimeMillis) / durationMillis.toDouble()
        val endAlpha = (percent.coerceIn(0.0, 1.0) * maxAlpha).toInt()
        val startAlpha = if (fadeStart) maxAlpha - endAlpha else maxAlpha
        val isDone = percent >= 1.0

        // Draw the start drawable.
        if (!isDone) {
            start?.apply {
                alpha = startAlpha
                canvas.withSave { draw(canvas) }
            }
        }

        // Draw the end drawable.
        end?.apply {
            alpha = endAlpha
            canvas.withSave { draw(canvas) }
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

    @Deprecated("Deprecated in Java")
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
        start?.let { updateBounds(it, bounds) }
        end?.let { updateBounds(it, bounds) }
    }

    override fun onLevelChange(level: Int): Boolean {
        val startChanged = start?.setLevel(level) ?: false
        val endChanged = end?.setLevel(level) ?: false
        return startChanged || endChanged
    }

    override fun onStateChange(state: IntArray): Boolean {
        val startChanged = start?.setState(state) ?: false
        val endChanged = end?.setState(state) ?: false
        return startChanged || endChanged
    }

    override fun getIntrinsicWidth() = intrinsicWidth

    override fun getIntrinsicHeight() = intrinsicHeight

    override fun unscheduleDrawable(who: Drawable, what: Runnable) = unscheduleSelf(what)

    override fun invalidateDrawable(who: Drawable) = invalidateSelf()

    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) = scheduleSelf(what, `when`)

    override fun setTint(tintColor: Int) {
        start?.setTint(tintColor)
        end?.setTint(tintColor)
    }

    override fun setTintList(tint: ColorStateList?) {
        start?.setTintList(tint)
        end?.setTintList(tint)
    }

    override fun setTintMode(tintMode: PorterDuff.Mode?) {
        start?.setTintMode(tintMode)
        end?.setTintMode(tintMode)
    }

    @RequiresApi(29)
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
        callbacks.forEachIndices { it.onAnimationStart(this) }

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

    /** Update the [Drawable]'s bounds inside [targetBounds] preserving aspect ratio. */
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
        val multiplier = DecodeUtils.computeSizeMultiplier(width, height, targetWidth, targetHeight, scale)
        val dx = ((targetWidth - multiplier * width) / 2).roundToInt()
        val dy = ((targetHeight - multiplier * height) / 2).roundToInt()

        val left = targetBounds.left + dx
        val top = targetBounds.top + dy
        val right = targetBounds.right - dx
        val bottom = targetBounds.bottom - dy
        drawable.setBounds(left, top, right, bottom)
    }

    private fun computeIntrinsicDimension(startSize: Int?, endSize: Int?): Int {
        if (!preferExactIntrinsicSize && (startSize == -1 || endSize == -1)) {
            return -1
        } else {
            return maxOf(startSize ?: -1, endSize ?: -1)
        }
    }

    private fun markDone() {
        state = STATE_DONE
        start = null
        callbacks.forEachIndices { it.onAnimationEnd(this) }
    }

    companion object {
        private const val STATE_START = 0
        private const val STATE_RUNNING = 1
        private const val STATE_DONE = 2
        const val DEFAULT_DURATION = DEFAULT_CROSSFADE_MILLIS
    }
}
