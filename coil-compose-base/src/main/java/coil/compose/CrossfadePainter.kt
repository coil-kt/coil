package coil.compose

import android.os.SystemClock
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.painter.Painter
import coil.decode.DecodeUtils
import coil.size.Scale
import kotlin.math.max

/**
 * A [Painter] that crossfades from [start] to [end].
 *
 * NOTE: The animation can only be executed once as the [start] drawable is
 * dereferenced at the end of the transition.
 */
@Stable
internal class CrossfadePainter(
    private var start: Painter?,
    private val end: Painter?,
    private val scale: Scale,
    private val durationMillis: Int,
    private val fadeStart: Boolean,
    private val preferExactIntrinsicSize: Boolean,
) : Painter() {

    private var invalidateTick by mutableStateOf(0)
    private var startTimeMillis = -1L
    private var isDone = false

    private var maxAlpha: Float by mutableStateOf(DefaultAlpha)
    private var colorFilter: ColorFilter? by mutableStateOf(null)

    override val intrinsicSize get() = computeIntrinsicSize()

    override fun DrawScope.onDraw() {
        if (isDone) {
            drawPainter(end, maxAlpha)
            return
        }

        // Initialize startTimeMillis the first time we're drawn.
        val uptimeMillis = SystemClock.uptimeMillis()
        if (startTimeMillis == -1L) {
            startTimeMillis = uptimeMillis
        }

        val percent = (uptimeMillis - startTimeMillis) / durationMillis.toFloat()
        val endAlpha = percent.coerceIn(0f, 1f) * maxAlpha
        val startAlpha = if (fadeStart) maxAlpha - endAlpha else maxAlpha
        isDone = percent >= 1f

        drawPainter(start, startAlpha)
        drawPainter(end, endAlpha)

        if (isDone) {
            start = null
        } else {
            // Increment this value to force the painter to be redrawn.
            invalidateTick++
        }
    }

    override fun applyAlpha(alpha: Float): Boolean {
        this.maxAlpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        return true
    }

    private fun computeIntrinsicSize(): Size {
        val startSize = start?.intrinsicSize ?: Size.Zero
        val endSize = end?.intrinsicSize ?: Size.Zero

        val isStartSpecified = startSize.isSpecified
        val isEndSpecified = endSize.isSpecified
        if (isStartSpecified && isEndSpecified) {
            return Size(
                width = max(startSize.width, endSize.width),
                height = max(startSize.height, endSize.height),
            )
        }
        if (preferExactIntrinsicSize) {
            if (isStartSpecified) return startSize
            if (isEndSpecified) return endSize
        }
        return Size.Unspecified
    }

    private fun DrawScope.drawPainter(painter: Painter?, alpha: Float) {
        if (painter == null || alpha <= 0) return

        with(painter) {
            val size = size
            val drawSize = computeDrawSize(intrinsicSize, size)

            if (size.isUnspecified || size.isEmpty()) {
                draw(drawSize, alpha, colorFilter)
            } else {
                inset(
                    horizontal = (size.width - drawSize.width) / 2,
                    vertical = (size.height - drawSize.height) / 2
                ) {
                    draw(drawSize, alpha, colorFilter)
                }
            }
        }
    }

    /** Scale the src size into the dst size preserving aspect ratio. */
    private fun computeDrawSize(srcSize: Size, dstSize: Size): Size {
        if (srcSize.isUnspecified || srcSize.isEmpty()) return dstSize
        if (dstSize.isUnspecified || dstSize.isEmpty()) return dstSize

        val srcWidth = srcSize.width
        val srcHeight = srcSize.height
        val multiplier = DecodeUtils.computeSizeMultiplier(
            srcWidth = srcWidth,
            srcHeight = srcHeight,
            dstWidth = dstSize.width,
            dstHeight = dstSize.height,
            scale = scale
        )
        return Size(
            width = multiplier * srcWidth,
            height = multiplier * srcHeight
        )
    }
}
