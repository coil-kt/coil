package coil3.compose.internal

import androidx.compose.ui.unit.Constraints
import kotlin.test.Test
import kotlin.test.assertEquals

// Inverted constraints (min > max) can transiently reach the content modifier and must clamp to
// max, not throw. See #3459.
class ConstrainSizeInvertedTest {

    @Test
    fun constrainWidth_clampsToMaxWhenInverted() {
        assertEquals(0f, invertedConstraints().constrainWidth(160f))
    }

    @Test
    fun constrainHeight_clampsToMaxWhenInverted() {
        assertEquals(0f, invertedConstraints().constrainHeight(160f))
    }

    // The public factory rejects min > max, so build the value by reflecting into Compose's
    // internal packer; a direct reference would need an @Suppress that CI's -Werror rejects.
    private fun invertedConstraints(): Constraints {
        val packed = Class.forName("androidx.compose.ui.unit.ConstraintsKt")
            .getMethod(
                "createConstraints",
                Int::class.java, Int::class.java, Int::class.java, Int::class.java,
            )
            .invoke(null, 160, 0, 160, 0) as Long
        return Class.forName("androidx.compose.ui.unit.Constraints")
            .getMethod("box-impl", Long::class.javaPrimitiveType)
            .invoke(null, packed) as Constraints
    }
}
