package coil3.compose.internal

import androidx.compose.ui.unit.Constraints
import kotlin.test.Test
import kotlin.test.assertEquals

class ConstrainSizeTest {

    @Test
    fun constrainWidth_clampsWithinBounds() {
        val constraints = Constraints(minWidth = 10, maxWidth = 100)
        assertEquals(10f, constraints.constrainWidth(5f))
        assertEquals(50f, constraints.constrainWidth(50f))
        assertEquals(100f, constraints.constrainWidth(150f))
    }

    @Test
    fun constrainHeight_clampsWithinBounds() {
        val constraints = Constraints(minHeight = 10, maxHeight = 100)
        assertEquals(10f, constraints.constrainHeight(5f))
        assertEquals(50f, constraints.constrainHeight(50f))
        assertEquals(100f, constraints.constrainHeight(150f))
    }

    // Inverted constraints (min > max) can reach the content modifier during transient layout
    // states, e.g. a collapsing parent. They must clamp to max, not throw. See #3459.
    @Test
    fun constrainWidth_doesNotThrowWhenInverted() {
        assertEquals(0f, invertedConstraints().constrainWidth(160f))
    }

    @Test
    fun constrainHeight_doesNotThrowWhenInverted() {
        assertEquals(0f, invertedConstraints().constrainHeight(160f))
    }

    // The public Constraints factory validates min <= max, so build the inverted value through
    // Compose's internal non-validating packer instead.
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    private fun invertedConstraints() = androidx.compose.ui.unit.createConstraints(
        minWidth = 160,
        maxWidth = 0,
        minHeight = 160,
        maxHeight = 0,
    )
}
