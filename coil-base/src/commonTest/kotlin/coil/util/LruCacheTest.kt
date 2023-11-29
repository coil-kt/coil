package coil.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LruCacheTest {

    @Test
    fun putAndGet() {
        val lruCache = LruCache<String, Int>(maxSize = 3)
        lruCache.put("one", 1)
        lruCache.put("two", 2)
        lruCache.put("three", 3)

        assertEquals(1, lruCache["one"])
        assertEquals(2, lruCache["two"])
        assertEquals(3, lruCache["three"])
    }

    @Test
    fun putUpdatesValue() {
        val lruCache = LruCache<String, Int>(maxSize = 3)
        lruCache.put("one", 1)
        lruCache.put("one", 10)

        assertEquals(10, lruCache["one"])
    }

    @Test
    fun getNonexistentKey() {
        val lruCache = LruCache<String, Int>(maxSize = 3)

        assertNull(lruCache["nonexistent"])
    }

    @Test
    fun evictionOnExceedingMaxSize() {
        val lruCache = LruCache<String, Int>(maxSize = 2)
        lruCache.put("one", 1)
        lruCache.put("two", 2)
        lruCache.put("three", 3)

        assertNull(lruCache["one"])
        assertEquals(2, lruCache["two"])
        assertEquals(3, lruCache["three"])
    }

    @Test
    fun lruOrderMaintained() {
        val lruCache = LruCache<String, Int>(maxSize = 3)
        lruCache.put("one", 1)
        lruCache.put("two", 2)
        lruCache.put("three", 3)

        // Access "one" to make it the most recently accessed.
        lruCache["one"]

        lruCache.put("four", 4)

        assertNull(lruCache["two"])
        assertEquals(1, lruCache["one"])
        assertEquals(3, lruCache["three"])
        assertEquals(4, lruCache["four"])
    }

    @Test
    fun clearCache() {
        val lruCache = LruCache<String, Int>(maxSize = 3)
        lruCache.put("one", 1)
        lruCache.put("two", 2)

        lruCache.clear()

        assertNull(lruCache["one"])
        assertNull(lruCache["two"])
    }
}
