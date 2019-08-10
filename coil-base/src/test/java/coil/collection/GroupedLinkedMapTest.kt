package coil.collection

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class GroupedLinkedMapTest {

    private lateinit var map: GroupedLinkedMap<Key, Any>

    @Before
    fun before() {
        map = GroupedLinkedMap()
    }

    @Test
    fun `get when empty returns null`() {
        val key = Key("key", 1, 1)
        assertNull(map[key])
    }

    @Test
    fun `can add and remove a value`() {
        val key = Key("key", 1, 1)
        val expected = Any()

        map[key] = expected

        assertEquals(expected, map[key])
    }

    @Test
    fun `can add and remove more than one value for a given key`() {
        val key = Key("key", 1, 1)
        val value = 20
        val numToAdd = 10

        for (i in 0 until numToAdd) {
            map[key] = value
        }

        for (i in 0 until numToAdd) {
            assertEquals(value, map[key])
        }
    }

    @Test
    fun `least recently retrieved key is least recently used`() {
        val firstKey = Key("key", 1, 1)
        val firstValue = 10
        map[firstKey] = firstValue
        map[firstKey] = firstValue

        val secondKey = Key("key", 2, 2)
        val secondValue = 20
        map[secondKey] = secondValue

        map[firstKey]

        assertEquals(secondValue, map.removeLast())
    }

    @Test
    fun `adding an entry does not make it the most recently used`() {
        val firstKey = Key("key", 1, 1)
        val firstValue = 10

        map[firstKey] = firstValue
        map[firstKey] = firstValue

        map[firstKey]

        val secondValue = 20
        map[Key("key", 2, 2)] = secondValue

        assertEquals(secondValue, map.removeLast())
    }

    private data class Key(
        private val key: String?,
        private val width: Int,
        private val height: Int
    )
}
