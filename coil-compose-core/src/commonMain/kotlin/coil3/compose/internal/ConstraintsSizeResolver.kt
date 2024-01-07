package coil3.compose.internal

import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import coil3.size.Size
import coil3.size.SizeResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull

/**
 * A [SizeResolver] that computes the size from the constraints passed during the layout phase.
 */
internal class ConstraintsSizeResolver : SizeResolver, LayoutModifier {

    private val currentConstraints = MutableStateFlow(ZeroConstraints)

    override suspend fun size(): Size {
        return currentConstraints
            .mapNotNull(Constraints::toSizeOrNull)
            .first()
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        // Cache the current constraints.
        currentConstraints.value = constraints

        // Measure and layout the content.
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }

    fun setConstraints(constraints: Constraints) {
        currentConstraints.value = constraints
    }
}
