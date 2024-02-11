package coil3.compose.internal

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
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
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.times
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import kotlin.math.roundToInt

/**
 * A custom [paint] modifier used by [AsyncImage] and [SubcomposeAsyncImage].
 */
internal data class ContentPainterElement(
    private val painter: Painter,
    private val alignment: Alignment,
    private val contentScale: ContentScale,
    private val alpha: Float,
    private val colorFilter: ColorFilter?,
) : ModifierNodeElement<ContentPainterNode>() {

    override fun create(): ContentPainterNode {
        return ContentPainterNode(
            painter = painter,
            alignment = alignment,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter,
        )
    }

    override fun update(node: ContentPainterNode) {
        val intrinsicsChanged = node.painter.intrinsicSize != painter.intrinsicSize

        node.painter = painter
        node.alignment = alignment
        node.contentScale = contentScale
        node.alpha = alpha
        node.colorFilter = colorFilter

        // Only remeasure if intrinsics have changed.
        if (intrinsicsChanged) {
            node.invalidateMeasurement()
        }

        // Redraw because one of the node properties has changed.
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "content"
        properties["painter"] = painter
        properties["alignment"] = alignment
        properties["contentScale"] = contentScale
        properties["alpha"] = alpha
        properties["colorFilter"] = colorFilter
    }
}

internal class ContentPainterNode(
    var painter: Painter,
    var alignment: Alignment,
    var contentScale: ContentScale,
    var alpha: Float,
    var colorFilter: ColorFilter?,
) : Modifier.Node(), DrawModifierNode, LayoutModifierNode {

    override val shouldAutoInvalidate get() = false

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(modifyConstraints(constraints))
        return layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int,
    ): Int {
        return if (painter.intrinsicSize.isSpecified) {
            val constraints = Constraints(maxHeight = height)
            val layoutWidth = measurable.minIntrinsicWidth(modifyConstraints(constraints).maxHeight)
            val scaledSize = calculateScaledSize(Size(layoutWidth.toFloat(), height.toFloat()))
            maxOf(scaledSize.width.roundToInt(), layoutWidth)
        } else {
            measurable.minIntrinsicWidth(height)
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int,
    ): Int {
        return if (painter.intrinsicSize.isSpecified) {
            val constraints = Constraints(maxHeight = height)
            val layoutWidth = measurable.maxIntrinsicWidth(modifyConstraints(constraints).maxHeight)
            val scaledSize = calculateScaledSize(Size(layoutWidth.toFloat(), height.toFloat()))
            maxOf(scaledSize.width.roundToInt(), layoutWidth)
        } else {
            measurable.maxIntrinsicWidth(height)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ): Int {
        return if (painter.intrinsicSize.isSpecified) {
            val constraints = Constraints(maxWidth = width)
            val layoutHeight =
                measurable.minIntrinsicHeight(modifyConstraints(constraints).maxWidth)
            val scaledSize = calculateScaledSize(Size(width.toFloat(), layoutHeight.toFloat()))
            maxOf(scaledSize.height.roundToInt(), layoutHeight)
        } else {
            measurable.minIntrinsicHeight(width)
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ): Int {
        return if (painter.intrinsicSize.isSpecified) {
            val constraints = Constraints(maxWidth = width)
            val layoutHeight =
                measurable.maxIntrinsicHeight(modifyConstraints(constraints).maxWidth)
            val scaledSize = calculateScaledSize(Size(width.toFloat(), layoutHeight.toFloat()))
            maxOf(scaledSize.height.roundToInt(), layoutHeight)
        } else {
            measurable.maxIntrinsicHeight(width)
        }
    }

    private fun calculateScaledSize(dstSize: Size): Size {
        if (dstSize.isEmpty()) {
            return Size.Zero
        }

        val intrinsicSize = painter.intrinsicSize
        if (intrinsicSize.isUnspecified) {
            return dstSize
        }

        val srcSize = Size(
            width = intrinsicSize.width.takeOrElse { dstSize.width },
            height = intrinsicSize.height.takeOrElse { dstSize.height },
        )
        val scaleFactor = contentScale.computeScaleFactor(srcSize, dstSize)
        if (!scaleFactor.scaleX.isFinite() || !scaleFactor.scaleY.isFinite()) {
            return dstSize
        }

        return scaleFactor * srcSize
    }

    private fun modifyConstraints(constraints: Constraints): Constraints {
        // The constraints are a fixed pixel value that can't be modified.
        val hasFixedWidth = constraints.hasFixedWidth
        val hasFixedHeight = constraints.hasFixedHeight
        if (hasFixedWidth && hasFixedHeight) {
            return constraints
        }

        // Fill the available space if the painter has no intrinsic size.
        val hasBoundedSize = constraints.hasBoundedWidth && constraints.hasBoundedHeight
        val intrinsicSize = painter.intrinsicSize
        if (intrinsicSize.isUnspecified) {
            if (hasBoundedSize) {
                return constraints.copy(
                    minWidth = constraints.maxWidth,
                    minHeight = constraints.maxHeight,
                )
            } else {
                return constraints
            }
        }

        // Changed from `PainterModifier`:
        // Use the maximum space as the destination size if the constraints are bounded and at
        // least one dimension is a fixed pixel value. Else, use the intrinsic size of the painter.
        val dstWidth: Float
        val dstHeight: Float
        if (hasBoundedSize && (hasFixedWidth || hasFixedHeight)) {
            dstWidth = constraints.maxWidth.toFloat()
            dstHeight = constraints.maxHeight.toFloat()
        } else {
            val (intrinsicWidth, intrinsicHeight) = intrinsicSize
            dstWidth = when {
                intrinsicWidth.isFinite() -> constraints.constrainWidth(intrinsicWidth)
                else -> constraints.minWidth.toFloat()
            }
            dstHeight = when {
                intrinsicHeight.isFinite() -> constraints.constrainHeight(intrinsicHeight)
                else -> constraints.minHeight.toFloat()
            }
        }

        // Scale the source dimensions into the destination dimensions and update the constraints.
        val (scaledWidth, scaledHeight) = calculateScaledSize(Size(dstWidth, dstHeight))
        return constraints.copy(
            minWidth = constraints.constrainWidth(scaledWidth.roundToInt()),
            minHeight = constraints.constrainHeight(scaledHeight.roundToInt()),
        )
    }

    override fun ContentDrawScope.draw() {
        val scaledSize = calculateScaledSize(size)
        val (dx, dy) = alignment.align(
            size = scaledSize.toIntSize(),
            space = size.toIntSize(),
            layoutDirection = layoutDirection,
        )

        // Draw the painter.
        translate(dx.toFloat(), dy.toFloat()) {
            with(painter) {
                draw(scaledSize, alpha, colorFilter)
            }
        }

        // Draw any child content on top of the painter.
        drawContent()
    }
}
