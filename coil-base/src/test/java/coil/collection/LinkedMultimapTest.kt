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
        assertNull(multimap[key])
    }

    @Test
    fun `can add and remove a value`() {
        val key = Key("key", 1, 1)
        val expected = Any()

        multimap[key] = expected

        assertEquals(expected, multimap[key])
    }

    @Test
    fun `can add and remove more than one value for a given key`() {
        val key = Key("key", 1, 1)
        val value = 20
        val numToAdd = 10

        for (i in 0 until numToAdd) {
            multimap[key] = value
        }

        for (i in 0 until numToAdd) {
            assertEquals(value, multimap[key])
        }
    }

    @Test
    fun `least recently retrieved key is least recently used`() {
        val firstKey = Key("key", 1, 1)
        val firstValue = 10
        multimap[firstKey] = firstValue
        multimap[firstKey] = firstValue

        val secondKey = Key("key", 2, 2)
        val secondValue = 20
        multimap[secondKey] = secondValue

        multimap[firstKey]

        assertEquals(secondValue, multimap.removeLast())
    }

    @Test
    fun `adding an entry does not make it the most recently used`() {
        val firstKey = Key("key", 1, 1)
        val firstValue = 10

        multimap[firstKey] = firstValue
        multimap[firstKey] = firstValue

        multimap[firstKey]

        val secondValue = 20
        multimap[Key("key", 2, 2)] = secondValue

        assertEquals(secondValue, multimap.removeLast())
    }

    private data class Key(
        private val key: String?,
        private val width: Int,
        private val height: Int
    )
}
