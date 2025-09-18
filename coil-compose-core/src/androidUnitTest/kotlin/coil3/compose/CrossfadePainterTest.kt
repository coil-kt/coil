package coil3.compose

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.graphics.createBitmap
import coil3.test.utils.FakeTimeSource
import coil3.test.utils.RobolectricTest
import coil3.test.utils.assertIsSimilarTo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.test.runTest

class CrossfadePainterTest : RobolectricTest() {

    @Test
    fun intrinsicSize_prefersMaxDimensions() {
        val start = SolidColorPainter(Color.Red, Size(64f, 32f))
        val end = SolidColorPainter(Color.Blue, Size(128f, 48f))

        val painter = CrossfadePainter(start, end)

        assertEquals(Size(128f, 48f), painter.intrinsicSize)
    }

    @Test
    fun intrinsicSize_prefersSpecifiedDimensionWhenRequested() {
        val start = SolidColorPainter(Color.Red, Size(40f, 30f))
        val end = SolidColorPainter(Color.Blue, Size.Unspecified)

        val preferExact = CrossfadePainter(
            start = start,
            end = end,
            preferExactIntrinsicSize = true,
        )
        val preferUnspecified = CrossfadePainter(
            start = start,
            end = end,
            preferExactIntrinsicSize = false,
        )

        assertEquals(Size(40f, 30f), preferExact.intrinsicSize)
        assertEquals(Size.Unspecified, preferUnspecified.intrinsicSize)
    }

    @Test
    fun intrinsicSize_remainsStableAfterCrossfadeCompletes() {
        val start = SolidColorPainter(Color.Red, Size(24f, 24f))
        val end = SolidColorPainter(Color.Blue, Size(32f, 16f))
        val timeSource = FakeTimeSource()
        val painter = CrossfadePainter(
            start = start,
            end = end,
            duration = 100.milliseconds,
            timeSource = timeSource,
        )

        val expected = Size(32f, 24f)
        assertEquals(expected, painter.intrinsicSize)

        render(painter, 48, 48)
        timeSource.advanceBy(200.milliseconds)
        render(painter, 48, 48)

        assertEquals(expected, painter.intrinsicSize)
    }

    @Test
    fun draw_crossfadesBetweenStartAndEnd() = runTest {
        val startColor = Color.Red
        val endColor = Color.Blue
        val start = SolidColorPainter(startColor, Size(64f, 64f))
        val end = SolidColorPainter(endColor, Size(64f, 64f))
        val timeSource = FakeTimeSource()
        val painter = CrossfadePainter(
            start = start,
            end = end,
            duration = 100.milliseconds,
            timeSource = timeSource,
        )

        val canvasSize = 64

        val frameStart = render(painter, canvasSize, canvasSize)
        val expectedStart = renderFrame(canvasSize, canvasSize) {
            drawRect(startColor, alpha = 1f)
        }

        timeSource.advanceBy(50.milliseconds)
        val frameMiddle = render(painter, canvasSize, canvasSize)
        val expectedMiddle = renderFrame(canvasSize, canvasSize) {
            drawRect(startColor, alpha = 0.5f)
            drawRect(endColor, alpha = 0.5f)
        }

        timeSource.advanceBy(100.milliseconds)
        val frameEnd = render(painter, canvasSize, canvasSize)
        val expectedEnd = renderFrame(canvasSize, canvasSize) {
            drawRect(endColor, alpha = 1f)
        }

        frameStart.assertIsSimilarTo(expectedStart)
        frameMiddle.assertIsSimilarTo(expectedMiddle, threshold = 0.97)
        frameEnd.assertIsSimilarTo(expectedEnd)
    }

    private fun render(painter: Painter, width: Int, height: Int): Bitmap {
        val image = createBitmap(width, height).asImageBitmap()
        val canvas = Canvas(image)
        val drawScope = CanvasDrawScope()
        val size = Size(width.toFloat(), height.toFloat())
        drawScope.draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = size,
        ) {
            with(painter) {
                draw(size)
            }
        }
        return image.asAndroidBitmap()
    }

    private fun renderFrame(
        width: Int,
        height: Int,
        block: DrawScope.() -> Unit,
    ): Bitmap {
        val image = createBitmap(width, height).asImageBitmap()
        val canvas = Canvas(image)
        val drawScope = CanvasDrawScope()
        drawScope.draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(width.toFloat(), height.toFloat()),
            block = block,
        )
        return image.asAndroidBitmap()
    }

    private class SolidColorPainter(
        private val color: Color,
        override val intrinsicSize: Size,
    ) : Painter() {
        override fun DrawScope.onDraw() = drawRect(color)
    }
}
