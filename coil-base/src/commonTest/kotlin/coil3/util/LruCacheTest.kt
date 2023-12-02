package coil3.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LruCacheTest {

    @Test
    fun putAndGet() {
        val cache = LruCache<String, Int>(maxSize = 3)
        cache.put("one", 1)
        cache.put("two", 2)
        cache.put("three", 3)

        assertEquals(1, cache["one"])
        assertEquals(2, cache["two"])
        assertEquals(3, cache["three"])
    }

    @Test
    fun putUpdatesValue() {
        val cache = LruCache<String, Int>(maxSize = 3)
        cache.put("one", 1)
        cache.put("one", 10)

        assertEquals(10, cache["one"])
    }

    @Test
    fun getNonexistentKey() {
        val cache = LruCache<String, Int>(maxSize = 3)

        assertNull(cache["nonexistent"])
    }

    @Test
    fun evictionOnExceedingMaxSize() {
        val cache = LruCache<String, Int>(maxSize = 2)
        cache.put("one", 1)
        cache.put("two", 2)
        cache.put("three", 3)

        assertNull(cache["one"])
        assertEquals(2, cache["two"])
        assertEquals(3, cache["three"])
    }

    @Test
    fun lruOrderMaintained() {
        val cache = LruCache<String, Int>(maxSize = 3)
        cache.put("one", 1)
        cache.put("two", 2)
        cache.put("three", 3)

        // Access "one" to make it the most recently accessed.
        cache["one"]

        cache.put("four", 4)

        assertNull(cache["two"])
        assertEquals(1, cache["one"])
        assertEquals(3, cache["three"])
        assertEquals(4, cache["four"])
    }

    @Test
    fun clearCache() {
        val cache = LruCache<String, Int>(maxSize = 3)
        cache.put("one", 1)
        cache.put("two", 2)

        cache.clear()

        assertNull(cache["one"])
        assertNull(cache["two"])
    }

    @Test
    fun customSizeOf() {
        val cache = object : LruCache<String, Int>(maxSize = 4) {
            override fun sizeOf(key: String, value: Int) = value.toLong()
        }
        cache.put("one", 1)
        cache.put("two", 2)
        cache.put("three", 3)

        assertNull(cache["one"])
        assertNull(cache["two"])
        assertEquals(3, cache["three"])
    }

    @Test
    fun entryRemovedIsInvoked() {
        var isInvoked = false
        val cache = object : LruCache<String, Int>(maxSize = 2) {
            override fun entryRemoved(key: String, oldValue: Int, newValue: Int?) {
                assertFalse(isInvoked)
                assertEquals("one", key)
                assertNull(newValue)
                isInvoked = true
            }
        }
        cache.put("one", 1)
        cache.put("two", 2)
        cache.put("three", 3)

        assertTrue(isInvoked)
        assertNull(cache["one"])
        assertEquals(2, cache["two"])
        assertEquals(3, cache["three"])
    }
}
