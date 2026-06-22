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
}
