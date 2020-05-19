package coil.collection

import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(Parameterized::class)
class LinkedMultimapTest(private val sorted: Boolean) {

    companion object {
        @JvmStatic @Parameters fun sorted() = listOf(true, false)
    }

    private lateinit var map: LinkedMultimap<Key, Any>

    @Before
    fun before() {
        map = LinkedMultimap(sorted)
    }

    @Test
    fun `get when empty returns null`() {
        val key = Key("key", 1, 1)
        assertNull(map.removeLast(key))
    }

    @Test
    fun `can add and remove a value`() {
        val key = Key("key", 1, 1)
        val expected = Any()

        map.add(key, expected)

        assertEquals(expected, map.removeLast(key))
    }

    @Test
    fun `can add and remove more than one value for a given key`() {
        val key = Key("key", 1, 1)
        val value = 20
        val numToAdd = 10

        for (i in 0 until numToAdd) {
            map.add(key, value)
        }

        for (i in 0 until numToAdd) {
            assertEquals(value, map.removeLast(key))
        }
    }

    @Test
    fun `least recently retrieved key is least recently used`() {
        val firstKey = Key("key", 1, 1)
        val firstValue = 10
        map.add(firstKey, firstValue)
        map.add(firstKey, firstValue)

        val secondKey = Key("key", 2, 2)
        val secondValue = 20
        map.add(secondKey, secondValue)

        map.removeLast(firstKey)

        assertEquals(secondValue, map.removeLast())
    }

    @Test
    fun `adding an entry does not make it the most recently used`() {
        val firstKey = Key("key", 1, 1)
        val firstValue = 10

        map.add(firstKey, firstValue)
        map.add(firstKey, firstValue)

        map.removeLast(firstKey)

        val secondValue = 20
        map.add(Key("key", 2, 2), secondValue)

        assertEquals(secondValue, map.removeLast())
    }

    @Test
    fun `sorted map - ceilingKey returns least key greater than input key`() {
        // ceilingKey throws an exception if the map is not sorted.
        assumeTrue(sorted)

        val map = LinkedMultimap<Int, Int>(sorted = true)
        map.add(3, 2)
        map.add(8, 4)
        map.add(5, 9)
        map.add(4, 9)
        map.add(1, 1)

        assertNull(map.ceilingKey(9))
        assertEquals(5, map.ceilingKey(5))
        assertEquals(3, map.ceilingKey(2))
    }

    private data class Key(
        private val key: String?,
        private val width: Int,
        private val height: Int
    ) : Comparable<Key> {
        override fun compareTo(other: Key) = (width * height) - (other.width * other.height)
    }
}
