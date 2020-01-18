package coil.drawable

import android.content.res.ColorStateList
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION_CODES.Q
import androidx.annotation.RequiresApi
import androidx.core.graphics.withSave
import coil.decode.DecodeUtils
import coil.size.Scale
import kotlin.math.roundToInt

/**
 * A [Drawable] that scales its [child] to fill its bounds.
 *
 * This allows drawables that only draw within their intrinsic dimensions
 * (e.g. [AnimatedImageDrawable]) to fill their entire bounds.
 */
class ScaleDrawable(
    val child: Drawable,
    private val scale: Scale
) : Drawable(), Drawable.Callback, Animatable {

    private var childScale = 1f

    override fun draw(canvas: Canvas) {
        canvas.withSave {
            scale(childScale, childScale)
            child.draw(this)
        }
    }

    @RequiresApi(KITKAT)
    override fun getAlpha() = child.alpha

    override fun setAlpha(alpha: Int) {
        child.alpha = alpha
    }

    @Suppress("DEPRECATION")
    override fun getOpacity() = child.opacity

    @RequiresApi(LOLLIPOP)
    override fun getColorFilter() = child.colorFilter

    @RequiresApi(LOLLIPOP)
    override fun setColorFilter(colorFilter: ColorFilter?) {
        child.colorFilter = colorFilter
    }

    override fun onBoundsChange(bounds: Rect) {
        val width = child.intrinsicWidth
        val height = child.intrinsicHeight
        if (width <= 0 || height <= 0) {
            child.bounds = bounds
            childScale = 1f
            return
        }

        val targetWidth = bounds.width()
        val targetHeight = bounds.height()
        val multiplier = DecodeUtils.computeSizeMultiplier(
            srcWidth = width,
            srcHeight = height,
            destWidth = targetWidth,
            destHeight = targetHeight,
            scale = scale
        )
        val dx = ((targetWidth - multiplier * width) / 2).roundToInt()
        val dy = ((targetHeight - multiplier * height) / 2).roundToInt()

        val left = bounds.left + dx
        val top = bounds.top + dy
        val right = left + width
        val bottom = top + height
        child.setBounds(left, top, right, bottom)
        childScale = multiplier.toFloat()
    }

    override fun onLevelChange(level: Int) = child.setLevel(level)

    override fun onStateChange(state: IntArray) = child.setState(state)

    override fun getIntrinsicWidth() = child.intrinsicWidth

    override fun getIntrinsicHeight() = child.intrinsicHeight

    override fun unscheduleDrawable(who: Drawable, what: Runnable) = unscheduleSelf(what)

    override fun invalidateDrawable(who: Drawable) = invalidateSelf()

    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) = scheduleSelf(what, `when`)

    @RequiresApi(LOLLIPOP)
    override fun setTint(tintColor: Int) = child.setTint(tintColor)

    @RequiresApi(LOLLIPOP)
    override fun setTintList(tint: ColorStateList?) = child.setTintList(tint)

    @RequiresApi(LOLLIPOP)
    override fun setTintMode(tintMode: PorterDuff.Mode?) = child.setTintMode(tintMode)

    @RequiresApi(Q)
    override fun setTintBlendMode(blendMode: BlendMode?) = child.setTintBlendMode(blendMode)

    override fun isRunning() = if (child is Animatable) child.isRunning else false

    override fun start() {
        if (child is Animatable) child.start()
    }

    override fun stop() {
        if (child is Animatable) child.stop()
    }
}
