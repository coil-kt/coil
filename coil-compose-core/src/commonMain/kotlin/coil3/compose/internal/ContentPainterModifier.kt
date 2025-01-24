package coil3.compose.internal

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
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
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidateMeasurement
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.AsyncImageModelEqualityDelegate
import coil3.compose.AsyncImagePainter
import coil3.compose.AsyncImagePainter.Input
import coil3.compose.AsyncImagePainter.State
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.ConstraintsSizeResolver
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A custom [paint] modifier used by [AsyncImage].
 */
internal data class ContentPainterElement(
    private val request: ImageRequest,
    private val imageLoader: ImageLoader,
    private val modelEqualityDelegate: AsyncImageModelEqualityDelegate,
    private val transform: (State) -> State,
    private val onState: ((State) -> Unit)?,
    private val filterQuality: FilterQuality,
    private val alignment: Alignment,
    private val contentScale: ContentScale,
    private val alpha: Float,
    private val colorFilter: ColorFilter?,
    private val clipToBounds: Boolean,
    private val previewHandler: AsyncImagePreviewHandler?,
    private val contentDescription: String?,
) : ModifierNodeElement<ContentPainterNode>() {

    override fun create(): ContentPainterNode {
        val input = Input(imageLoader, request, modelEqualityDelegate)

        // Create the painter during modifier creation so we reuse the same painter object when the
        // modifier is being reused as part of the lazy layouts reuse flow.
        val painter = AsyncImagePainter(input)
        painter.transform = transform
        painter.onState = onState
        painter.contentScale = contentScale
        painter.filterQuality = filterQuality
        painter.previewHandler = previewHandler
        painter._input = input

        return ContentPainterNode(
            painter = painter,
            constraintSizeResolver = request.sizeResolver as? ConstraintsSizeResolver,
            alignment = alignment,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter,
            clipToBounds = clipToBounds,
            contentDescription = contentDescription,
        )
    }

    override fun update(node: ContentPainterNode) {
        val previousIntrinsics = node.painter.intrinsicSize
        val previousConstraintSizeResolver = node.constraintSizeResolver
        val input = Input(imageLoader, request, modelEqualityDelegate)
        val painter = node.painter
        painter.transform = transform
        painter.onState = onState
        painter.contentScale = contentScale
        painter.filterQuality = filterQuality
        painter.previewHandler = previewHandler
        painter._input = input

        val intrinsicsChanged = previousIntrinsics != painter.intrinsicSize

        node.alignment = alignment
        node.constraintSizeResolver = request.sizeResolver as? ConstraintsSizeResolver
        node.contentScale = contentScale
        node.alpha = alpha
        node.colorFilter = colorFilter
        node.clipToBounds = clipToBounds

        if (node.contentDescription != contentDescription) {
            node.contentDescription = contentDescription
            node.invalidateSemantics()
        }

        val constraintSizeResolverChanged =
            previousConstraintSizeResolver != node.constraintSizeResolver

        // Only remeasure if intrinsics have changed.
        if (intrinsicsChanged || constraintSizeResolverChanged) {
            node.invalidateMeasurement()
        }

        // Redraw because one of the node properties has changed.
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "content"
        properties["request"] = request
        properties["imageLoader"] = imageLoader
        properties["modelEqualityDelegate"] = modelEqualityDelegate
        properties["transform"] = transform
        properties["onState"] = onState
        properties["filterQuality"] = filterQuality
        properties["alignment"] = alignment
        properties["contentScale"] = contentScale
        properties["alpha"] = alpha
        properties["colorFilter"] = colorFilter
        properties["clipToBounds"] = clipToBounds
        properties["previewHandler"] = previewHandler
        properties["contentDescription"] = contentDescription
    }
}

internal class ContentPainterNode(
    override val painter: AsyncImagePainter,
    alignment: Alignment,
    contentScale: ContentScale,
    alpha: Float,
    colorFilter: ColorFilter?,
    clipToBounds: Boolean,
    contentDescription: String?,
    constraintSizeResolver: ConstraintsSizeResolver?,
) : AbstractContentPainterNode(
    alignment = alignment,
    contentScale = contentScale,
    alpha = alpha,
    colorFilter = colorFilter,
    clipToBounds = clipToBounds,
    contentDescription = contentDescription,
    constraintSizeResolver = constraintSizeResolver,
) {

    override fun onAttach() {
        painter.scope = coroutineScope
        painter.onRemembered()
    }

    override fun onDetach() {
        painter.onForgotten()
    }

    override fun onReset() {
        // Clear the current input here as `ModifierNodeElement.update` will be called with the
        // new input when it's reused. If we don't clear it here, we might restart the request for
        // the old input, as `Modifier.Node.onAttach()` is called before modifier element update.
        painter._input = null
    }
}

/**
 * A custom [paint] modifier used by [SubcomposeAsyncImage].
 *
 * Ideally [SubcomposeAsyncImage] should use [ContentPainterElement] as well, however
 * [SubcomposeAsyncImageContent] exposes the fact that we have to create a painter during the
 * composition as part of its API.
 */
internal data class SubcomposeContentPainterElement(
    private val painter: Painter,
    private val alignment: Alignment,
    private val contentScale: ContentScale,
    private val alpha: Float,
    private val colorFilter: ColorFilter?,
    private val clipToBounds: Boolean,
    private val contentDescription: String?,
) : ModifierNodeElement<SubcomposeContentPainterNode>() {

    override fun create(): SubcomposeContentPainterNode {
        return SubcomposeContentPainterNode(
            painter = painter,
            alignment = alignment,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter,
            clipToBounds = clipToBounds,
            contentDescription = contentDescription,
        )
    }

    override fun update(node: SubcomposeContentPainterNode) {
        val intrinsicsChanged = node.painter.intrinsicSize != painter.intrinsicSize

        node.painter = painter
        node.alignment = alignment
        node.contentScale = contentScale
        node.alpha = alpha
        node.colorFilter = colorFilter
        node.clipToBounds = clipToBounds

        if (node.contentDescription != contentDescription) {
            node.contentDescription = contentDescription
            node.invalidateSemantics()
        }

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
        properties["clipToBounds"] = clipToBounds
        properties["contentDescription"] = contentDescription
    }
}

internal class SubcomposeContentPainterNode(
    override var painter: Painter,
    alignment: Alignment,
    contentScale: ContentScale,
    alpha: Float,
    colorFilter: ColorFilter?,
    clipToBounds: Boolean,
    contentDescription: String?,
) : AbstractContentPainterNode(
    alignment = alignment,
    contentScale = contentScale,
    alpha = alpha,
    colorFilter = colorFilter,
    clipToBounds = clipToBounds,
    contentDescription = contentDescription,
    constraintSizeResolver = null,
)

internal abstract class AbstractContentPainterNode(
    var alignment: Alignment,
    var contentScale: ContentScale,
    var alpha: Float,
    var colorFilter: ColorFilter?,
    var clipToBounds: Boolean,
    var contentDescription: String?,
    var constraintSizeResolver: ConstraintsSizeResolver?,
) : Modifier.Node(), DrawModifierNode, LayoutModifierNode, SemanticsModifierNode {

    abstract val painter: Painter

    override val shouldAutoInvalidate get() = false

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        constraintSizeResolver?.setConstraints(constraints)

        val placeable = measurable.measure(modifyConstraints(constraints))
        return layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int,
    ): Int {
        val constraints = Constraints(maxHeight = height)
        constraintSizeResolver?.setConstraints(constraints)

        return if (painter.intrinsicSize.isSpecified) {
            val modifiedConstraints = modifyConstraints(constraints)
            val layoutWidth = measurable.minIntrinsicWidth(height)
            max(modifiedConstraints.minWidth, layoutWidth)
        } else {
            measurable.minIntrinsicWidth(height)
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int,
    ): Int {
        val constraints = Constraints(maxHeight = height)
        constraintSizeResolver?.setConstraints(constraints)

        return if (painter.intrinsicSize.isSpecified) {
            val modifiedConstraints = modifyConstraints(constraints)
            val layoutWidth = measurable.maxIntrinsicWidth(height)
            max(modifiedConstraints.minWidth, layoutWidth)
        } else {
            measurable.maxIntrinsicWidth(height)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ): Int {
        val constraints = Constraints(maxWidth = width)
        constraintSizeResolver?.setConstraints(constraints)

        return if (painter.intrinsicSize.isSpecified) {
            val modifiedConstraints = modifyConstraints(constraints)
            val layoutHeight = measurable.minIntrinsicHeight(width)
            max(modifiedConstraints.minHeight, layoutHeight)
        } else {
            measurable.minIntrinsicHeight(width)
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int,
    ): Int {
        val constraints = Constraints(maxWidth = width)
        constraintSizeResolver?.setConstraints(constraints)

        return if (painter.intrinsicSize.isSpecified) {
            val modifiedConstraints = modifyConstraints(constraints)
            val layoutHeight = measurable.maxIntrinsicHeight(width)
            max(modifiedConstraints.minHeight, layoutHeight)
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
        val painter = painter
        val hasBoundedSize = constraints.hasBoundedWidth && constraints.hasBoundedHeight
        val intrinsicSize = painter.intrinsicSize
        if (intrinsicSize.isUnspecified) {
            // Changed from `PainterModifier`:
            // If AsyncImagePainter has no child painter, do not occupy the max constraints.
            if (!hasBoundedSize ||
                (painter is AsyncImagePainter && painter.state.value.painter == null)
            ) {
                return constraints
            } else {
                return constraints.copy(
                    minWidth = constraints.maxWidth,
                    minHeight = constraints.maxHeight,
                )
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

        withTransform({
            if (clipToBounds) {
                clipRect()
            }
            translate(dx.toFloat(), dy.toFloat())
        }) {
            with(painter) {
                draw(scaledSize, alpha, colorFilter)
            }
        }

        // Draw any child content on top of the painter.
        drawContent()
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        val contentDescription = this@AbstractContentPainterNode.contentDescription
        if (contentDescription != null) {
            this.contentDescription = contentDescription
            this.role = Role.Image
        }
    }
}
