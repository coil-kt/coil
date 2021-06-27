package coil.compose

import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.withSave
import androidx.compose.ui.unit.LayoutDirection
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.math.roundToInt

/**
 * Convert this [Drawable] into a [Painter] using Compose primitives if possible.
 */
internal fun Drawable.toPainter(): Painter {
    return when (this) {
        is BitmapDrawable -> BitmapPainter(bitmap.asImageBitmap())
        is ColorDrawable -> ColorPainter(Color(color))
        else -> DrawablePainter(mutate())
    }
}

/**
 * A [Painter] which draws an Android [Drawable] and supports [Animatable] drawables.
 *
 * Instances should be remembered to be able to start and stop [Animatable] animations.
 */
@Stable
private class DrawablePainter(
    private val drawable: Drawable
) : Painter(), RememberObserver {

    private var invalidateTick by mutableStateOf(0)

    private val callback = object : Drawable.Callback {
        override fun invalidateDrawable(who: Drawable) {
            // Update the tick so that we get re-drawn.
            invalidateTick++
        }

        override fun scheduleDrawable(who: Drawable, what: Runnable, time: Long) {
            MAIN_HANDLER.postAtTime(what, time)
        }

        override fun unscheduleDrawable(who: Drawable, what: Runnable) {
            MAIN_HANDLER.removeCallbacks(what)
        }
    }

    init {
        // Update the drawable's bounds to match the intrinsic size.
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
    }

    override val intrinsicSize: Size
        get() = Size(
            width = drawable.intrinsicWidth.toFloat(),
            height = drawable.intrinsicHeight.toFloat()
        )

    override fun DrawScope.onDraw() = drawIntoCanvas { canvas ->
        // Reading this ensures that we invalidate when invalidateDrawable() is called.
        invalidateTick

        // Update the drawable's bounds.
        drawable.setBounds(0, 0, size.width.roundToInt(), size.height.roundToInt())

        canvas.withSave {
            drawable.draw(canvas.nativeCanvas)
        }
    }

    override fun onRemembered() {
        drawable.callback = callback
        drawable.setVisible(true, true)
        if (drawable is Animatable) drawable.start()
    }

    override fun onForgotten() {
        if (drawable is Animatable) drawable.stop()
        drawable.setVisible(false, false)
        drawable.callback = null
    }

    override fun onAbandoned() = onForgotten()

    override fun applyAlpha(alpha: Float): Boolean {
        drawable.alpha = (alpha * 255).roundToInt().coerceIn(0, 255)
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        drawable.colorFilter = colorFilter?.asAndroidColorFilter()
        return true
    }

    override fun applyLayoutDirection(layoutDirection: LayoutDirection): Boolean {
        if (SDK_INT >= 23) {
            return drawable.setLayoutDirection(
                when (layoutDirection) {
                    LayoutDirection.Ltr -> View.LAYOUT_DIRECTION_LTR
                    LayoutDirection.Rtl -> View.LAYOUT_DIRECTION_RTL
                }
            )
        }
        return false
    }
}

private val MAIN_HANDLER by lazy(NONE) { Handler(Looper.getMainLooper()) }
