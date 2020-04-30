package coil.collection

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class SparseIntArraySetTest {

    private lateinit var set: SparseIntArraySet

    @Before
    fun before() {
        set = SparseIntArraySet(0)
    }

    @Test
    fun `contains when empty returns false`() {
        assertFalse(set.contains(100))
    }

    @Test
    fun `can add element`() {
        assertTrue(set.add(1))
        assertEquals(1, set.size)
    }

    @Test
    fun `can remove element`() {
        set.add(1)
        set.add(2)
        set.add(3)
        assertTrue(set.contains(2))

        assertTrue(set.remove(2))
        assertFalse(set.contains(2))
    }

    @Test
    fun `can add and remove the same element more than once`() {
        set.add(1)
        assertTrue(set.contains(1))

        set.remove(1)
        assertFalse(set.contains(1))

        set.add(1)
        assertTrue(set.contains(1))
    }

    @Test
    fun `clear empties the collection`() {
        set.add(1)
        set.add(2)
        set.add(3)
        assertEquals(3, set.size)

        set.clear()
        assertEquals(0, set.size)
        assertFalse(set.contains(1))
        assertFalse(set.contains(2))
        assertFalse(set.contains(3))
    }

    @Test
    fun `adding the same element does not increment size`() {
        assertTrue(set.add(100))
        assertEquals(1, set.size)
        assertFalse(set.add(100))
        assertEquals(1, set.size)
        assertTrue(set.add(200))
        assertEquals(2, set.size)
    }

    @Test
    fun `element at index and index of element are in sync`() {
        set.add(1)
        set.add(2)
        set.add(3)
        set.add(5)
        set.add(6)

        assertEquals(3, set.indexOfElement(5))
        assertEquals(5, set.elementAt(3))

        set.add(4)

        assertEquals(4, set.indexOfElement(5))
        assertEquals(5, set.elementAt(4))

        set.remove(2)

        assertEquals(3, set.indexOfElement(5))
        assertEquals(5, set.elementAt(3))
    }

    @Test
    fun `internal array values are copied properly`() {
        val numValues = 1000
        val values = (0 until numValues)

        // Insert the values in a random order.
        for (value in values.shuffled()) {
            set.add(value)
        }

        assertEquals(numValues, set.size)
        for (value in values) {
            assertTrue(set.contains(value), "Set does not contain $value.")
        }

        // Remove a random half of the values in a random order.
        val shuffledValues = values.shuffled()
        val removedValues = shuffledValues.subList(0, numValues / 2)
        for (value in removedValues.shuffled()) {
            set.remove(value)
        }

        assertEquals(numValues / 2, set.size)

        val keptValues = shuffledValues.subList(numValues / 2, numValues)
        for (value in keptValues.shuffled()) {
            assertTrue(set.contains(value), "Set does not contain $value.")
        }
    }
}
