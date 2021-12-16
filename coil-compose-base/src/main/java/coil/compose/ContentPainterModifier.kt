package coil.compose

import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.times
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A custom `PainterModifier` that fills the remaining space if one dimension of
 * the incoming constraints is fixed.
 */
internal data class ContentPainterModifier(
    private val painter: Painter,
    private val alignment: Alignment,
    private val contentScale: ContentScale,
    private val alpha: Float,
    private val colorFilter: ColorFilter?,
) : LayoutModifier, DrawModifier, InspectorValueInfo(
    debugInspectorInfo {
        name = "content"
        properties["painter"] = painter
        properties["alignment"] = alignment
        properties["contentScale"] = contentScale
        properties["alpha"] = alpha
        properties["colorFilter"] = colorFilter
    }
) {

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(modifyConstraints(constraints))
        return layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        return if (painter.intrinsicSize.isSpecified) {
            val constraints = Constraints(maxHeight = height)
            val layoutWidth = measurable.minIntrinsicWidth(modifyConstraints(constraints).maxHeight)
            val scaledSize = calculateScaledSize(Size(layoutWidth.toFloat(), height.toFloat()))
            max(scaledSize.width.roundToInt(), layoutWidth)
        } else {
            measurable.minIntrinsicWidth(height)
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        return if (painter.intrinsicSize.isSpecified) {
            val constraints = Constraints(maxHeight = height)
            val layoutWidth = measurable.maxIntrinsicWidth(modifyConstraints(constraints).maxHeight)
            val scaledSize = calculateScaledSize(Size(layoutWidth.toFloat(), height.toFloat()))
            max(scaledSize.width.roundToInt(), layoutWidth)
        } else {
            measurable.maxIntrinsicWidth(height)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int {
        return if (painter.intrinsicSize.isSpecified) {
            val constraints = Constraints(maxWidth = width)
            val layoutHeight = measurable.minIntrinsicHeight(modifyConstraints(constraints).maxWidth)
            val scaledSize = calculateScaledSize(Size(width.toFloat(), layoutHeight.toFloat()))
            max(scaledSize.height.roundToInt(), layoutHeight)
        } else {
            measurable.minIntrinsicHeight(width)
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int {
        return if (painter.intrinsicSize.isSpecified) {
            val constraints = Constraints(maxWidth = width)
            val layoutHeight = measurable.maxIntrinsicHeight(modifyConstraints(constraints).maxWidth)
            val scaledSize = calculateScaledSize(Size(width.toFloat(), layoutHeight.toFloat()))
            max(scaledSize.height.roundToInt(), layoutHeight)
        } else {
            measurable.maxIntrinsicHeight(width)
        }
    }

    private fun calculateScaledSize(dstSize: Size): Size {
        if (dstSize.isEmpty()) return Size.Zero
        val intrinsicSize = painter.intrinsicSize
        if (intrinsicSize.isUnspecified) return dstSize

        val srcSize = Size(
            width = intrinsicSize.width.takeOrElse { dstSize.width },
            height = intrinsicSize.height.takeOrElse { dstSize.height }
        )
        return srcSize * contentScale.computeScaleFactor(srcSize, dstSize)
    }

    private fun modifyConstraints(constraints: Constraints): Constraints {
        val hasFixedWidth = constraints.hasFixedWidth
        val hasFixedHeight = constraints.hasFixedHeight
        if (hasFixedWidth && hasFixedHeight) {
            return constraints
        }

        val intrinsicSize = painter.intrinsicSize
        if (intrinsicSize.isUnspecified) {
            // Fill the available space if the painter has no intrinsic size.
            if (constraints.hasBoundedWidth && constraints.hasBoundedHeight) {
                return constraints.copy(
                    minWidth = constraints.maxWidth,
                    minHeight = constraints.maxHeight
                )
            } else {
                return constraints
            }
        }

        // Changed from PainterModifier:
        // Scale the image to fill the maximum space if one dimension is fixed.
        val srcWidth = if (hasFixedHeight) {
            constraints.maxWidth.toFloat()
        } else {
            intrinsicSize.width.takeOrElse { constraints.minWidth.toFloat() }
        }
        val srcHeight = if (hasFixedWidth) {
            constraints.maxHeight.toFloat()
        } else {
            intrinsicSize.height.takeOrElse { constraints.minHeight.toFloat() }
        }

        val constrainedSize = Size(
            width = constraints.constrainWidth(srcWidth),
            height = constraints.constrainHeight(srcHeight)
        )
        val scaledSize = calculateScaledSize(constrainedSize)
        return constraints.copy(
            minWidth = constraints.constrainWidth(scaledSize.width.roundToInt()),
            minHeight = constraints.constrainHeight(scaledSize.height.roundToInt())
        )
    }

    override fun ContentDrawScope.draw() {
        val scaledSize = calculateScaledSize(size)
        val (dx, dy) = alignment.align(
            size = scaledSize.toIntSize(),
            space = size.toIntSize(),
            layoutDirection = layoutDirection
        )

        // Draw the painter.
        translate(dx.toFloat(), dy.toFloat()) {
            with(painter) {
                draw(size = scaledSize, alpha = alpha, colorFilter = colorFilter)
            }
        }

        // Draw any child content on top of the painter.
        drawContent()
    }
}
