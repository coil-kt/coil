package coil3.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LruMutableMapTest {

    @Test
    fun putAndGet() {
        val map = LruMutableMap<String, Int>()
        map["one"] = 1
        map["two"] = 2
        map["three"] = 3

        assertEquals(1, map["one"])
        assertEquals(2, map["two"])
        assertEquals(3, map["three"])
    }

    @Test
    fun putUpdatesValue() {
        val map = LruMutableMap<String, Int>()
        map["one"] = 1
        map["one"] = 10

        assertEquals(10, map["one"])
    }

    @Test
    fun getNonexistentKey() {
        val map = LruMutableMap<String, Int>()

        assertNull(map["nonexistent"])
    }

    @Test
    fun lruOrderMaintained() {
        val map = LruMutableMap<String, Int>()
        map["one"] = 1
        map["two"] = 2
        map["three"] = 3

        assertEquals(listOf("one", "two", "three"), map.keys.toList())
        assertEquals(listOf(1, 2, 3), map.values.toList())

        // Access "three" to make it the most recently accessed.
        map["one"]

        assertEquals(listOf("two", "three", "one"), map.keys.toList())
        assertEquals(listOf(2, 3, 1), map.values.toList())
    }
}
