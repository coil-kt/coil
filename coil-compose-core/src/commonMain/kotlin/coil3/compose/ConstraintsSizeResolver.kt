package coil3.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import coil3.compose.internal.ZeroConstraints
import coil3.compose.internal.toSize
import coil3.size.Size
import coil3.size.SizeResolver
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Create a [ConstraintsSizeResolver] and remember it.
 */
@Composable
fun rememberConstraintsSizeResolver(): ConstraintsSizeResolver {
    return remember { ConstraintsSizeResolver() }
}

/**
 * A [SizeResolver] that computes the size from the constraints passed during the layout phase
 * or from [setConstraints].
 */
@Stable
class ConstraintsSizeResolver : SizeResolver, LayoutModifier {
    private var latestConstraints = ZeroConstraints
    private var continuations = mutableListOf<Continuation<Unit>>()

    override suspend fun size(): Size {
        if (latestConstraints.isZero) {
            var continuation: Continuation<Unit>? = null
            try {
                suspendCancellableCoroutine<Unit> {
                    continuation = it
                    continuations.add(it)
                }
            } finally {
                continuations.remove(continuation)
            }
        }
        return latestConstraints.toSize()
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        setConstraints(constraints)

        // Measure and layout the content.
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }

    fun setConstraints(constraints: Constraints) {
        // Cache the latest constraints.
        latestConstraints = constraints
        if (!constraints.isZero) {
            val c = continuations
            if (c.isNotEmpty()) {
                continuations = mutableListOf()
                c.forEach { it.resume(Unit) }
            }
        }
    }
}
