package coil.collection

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class LinkedMultimapTest {

    private lateinit var multimap: LinkedMultimap<Key, Any>

    @Before
    fun before() {
        multimap = LinkedMultimap()
    }

    @Test
    fun `get when empty returns null`() {
        val key = Key("key", 1, 1)
        assertNull(multimap.removeLast(key))
    }

    @Test
    fun `can add and remove a value`() {
        val key = Key("key", 1, 1)
        val expected = Any()

        multimap.add(key, expected)

        assertEquals(expected, multimap.removeLast(key))
    }

    @Test
    fun `can add and remove more than one value for a given key`() {
        val key = Key("key", 1, 1)
        val value = 20
        val numToAdd = 10

        for (i in 0 until numToAdd) {
            multimap.add(key, value)
        }

        for (i in 0 until numToAdd) {
            assertEquals(value, multimap.removeLast(key))
        }
    }

    @Test
    fun `least recently retrieved key is least recently used`() {
        val firstKey = Key("key", 1, 1)
        val firstValue = 10
        multimap.add(firstKey, firstValue)
        multimap.add(firstKey, firstValue)

        val secondKey = Key("key", 2, 2)
        val secondValue = 20
        multimap.add(secondKey, secondValue)

        multimap.removeLast(firstKey)

        assertEquals(secondValue, multimap.removeLast())
    }

    @Test
    fun `adding an entry does not make it the most recently used`() {
        val firstKey = Key("key", 1, 1)
        val firstValue = 10

        multimap.add(firstKey, firstValue)
        multimap.add(firstKey, firstValue)

        multimap.removeLast(firstKey)

        val secondValue = 20
        multimap.add(Key("key", 2, 2), secondValue)

        assertEquals(secondValue, multimap.removeLast())
    }

    private data class Key(
        private val key: String?,
        private val width: Int,
        private val height: Int
    )
}
