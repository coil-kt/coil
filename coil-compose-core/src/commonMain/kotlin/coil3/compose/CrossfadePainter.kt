package coil3.compose

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.times
import androidx.compose.ui.unit.IntSize
import coil3.compose.internal.toIntSize
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * A [Painter] that crossfades from [start] to [end].
 *
 * NOTE: The animation can only be executed once as the [start]
 * painter is dereferenced at the end of the transition.
 *
 * @param start The [Painter] to crossfade from.
 * @param end The [Painter] to crossfade to.
 * @param contentScale The scaling algorithm for [start] and [end].
 * @param duration The duration of the crossfade animation.
 * @param timeSource The source for measuring time intervals.
 * @param fadeStart If false, the start drawable will not fade out while the end drawable fades in.
 * @param preferExactIntrinsicSize If true, this drawable's intrinsic width/height will only be -1
 *  if [start] **and** [end] return -1 for that dimension. If false, the intrinsic width/height will
 *  be -1 if [start] **or** [end] return -1 for that dimension. This is useful for views that
 *  require an exact intrinsic size to scale the drawable.
 * @param preferEndFirstIntrinsicSize Returns `true` if this request prefers the end painter's intrinsic size
 * when calculating the `CrossfadePainter`'s intrinsic size.
 * When enabled, the end painter's intrinsic size takes precedence.
 * @param alignment Optional alignment parameter used to place the painters in the given bounds.
 */
@Stable
class CrossfadePainter(
    start: Painter?,
    val end: Painter?,
    val contentScale: ContentScale = ContentScale.Fit,
    val duration: Duration = 200.milliseconds,
    val timeSource: TimeSource = TimeSource.Monotonic,
    val fadeStart: Boolean = true,
    val preferExactIntrinsicSize: Boolean = false,
    val preferEndFirstIntrinsicSize: Boolean = false,
    val alignment: Alignment = Alignment.Center
) : Painter() {

    @Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
    constructor(
        start: Painter?,
        end: Painter?,
        contentScale: ContentScale = ContentScale.Fit,
        duration: Duration = 200.milliseconds,
        timeSource: TimeSource = TimeSource.Monotonic,
        fadeStart: Boolean = true,
        preferExactIntrinsicSize: Boolean = false,
    ) : this(start, end, contentScale, duration, timeSource, fadeStart, preferExactIntrinsicSize, Alignment.Center)

    private var invalidateTick by mutableIntStateOf(0)
    private var startTime: TimeMark? = null
    private var isDone = false

    private var maxAlpha: Float = DefaultAlpha
    private var colorFilter: ColorFilter? = null

    var start: Painter? = start
        private set

    override val intrinsicSize: Size = computeIntrinsicSize(start, end)

    override fun DrawScope.onDraw() {
        if (isDone) {
            drawPainter(end, maxAlpha)
            return
        }

        // Initialize startTime the first time we're drawn.
        val startTime = startTime ?: timeSource.markNow().also { startTime = it }
        val percent = startTime.elapsedNow().inWholeMilliseconds / duration.inWholeMilliseconds.toFloat()
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

    private fun computeIntrinsicSize(start: Painter?, end: Painter?): Size {
        val startSize = start?.intrinsicSize ?: Size.Zero
        val endSize = end?.intrinsicSize ?: Size.Zero

        val isStartSpecified = startSize.isSpecified
        val isEndSpecified = endSize.isSpecified

        if (preferEndFirstIntrinsicSize) {
            if (isEndSpecified) return endSize
            if (isStartSpecified) return startSize
        }

        if (isStartSpecified && isEndSpecified) {
            return Size(
                width = maxOf(startSize.width, endSize.width),
                height = maxOf(startSize.height, endSize.height),
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
                val (dx, dy) = alignment.align(
                    size = drawSize.toIntSize(),
                    space = size.toIntSize(),
                    layoutDirection = layoutDirection,
                )
                inset(
                    horizontal = dx.toFloat(),
                    vertical = dy.toFloat(),
                ) {
                    draw(drawSize, alpha, colorFilter)
                }
            }
        }
    }

    private fun computeDrawSize(srcSize: Size, dstSize: Size): Size {
        if (srcSize.isUnspecified || srcSize.isEmpty()) return dstSize
        if (dstSize.isUnspecified || dstSize.isEmpty()) return dstSize
        return srcSize * contentScale.computeScaleFactor(srcSize, dstSize)
    }
}
