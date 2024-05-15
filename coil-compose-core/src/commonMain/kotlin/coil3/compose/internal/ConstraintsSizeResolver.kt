package coil3.compose.internal

import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import coil3.size.Size
import coil3.size.SizeResolver
import kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

/**
 * A [SizeResolver] that computes the size from the constraints passed during the layout phase.
 */
internal class ConstraintsSizeResolver : SizeResolver, LayoutModifier {
    private val currentConstraints = MutableSharedFlow<Constraints>(
        replay = 1,
        onBufferOverflow = DROP_OLDEST,
    )

    override suspend fun size(): Size {
        return currentConstraints.first { !it.isZero }.toSize()
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        // Cache the current constraints.
        currentConstraints.tryEmit(constraints)

        // Measure and layout the content.
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }

    fun setConstraints(constraints: Constraints) {
        currentConstraints.tryEmit(constraints)
    }
}
