package coil3.memory

import coil3.Image
import coil3.memory.MemoryCache.Key
import coil3.test.utils.FakeImage
import coil3.util.forEachIndices
import kotlin.experimental.ExperimentalNativeApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalNativeApi::class)
class WeakMemoryCacheTest {

    private val weakMemoryCache = WeakReferenceMemoryCache()
    private val references = mutableSetOf<Image>()

    @Test
    fun `can retrieve cached value`() {
        val key = Key("key")
        val image = reference(FakeImage())
        val extras = mapOf("test" to 4)
        val size = image.size

        weakMemoryCache.set(key, image, extras, size)
        val value = weakMemoryCache.get(key)

        assertNotNull(value)
        assertEquals(image, value.image)
        assertEquals(extras, value.extras)
    }

    @Test
    fun `can hold multiple values`() {
        val image1 = reference(FakeImage())
        weakMemoryCache.set(Key("key1"), image1, emptyMap(), 100)

        val image2 = reference(FakeImage())
        weakMemoryCache.set(Key("key2"), image2, emptyMap(), 100)

        val image3 = reference(FakeImage())
        weakMemoryCache.set(Key("key3"), image3, emptyMap(), 100)

        assertEquals(image1, weakMemoryCache.get(Key("key1"))?.image)
        assertEquals(image2, weakMemoryCache.get(Key("key2"))?.image)
        assertEquals(image3, weakMemoryCache.get(Key("key3"))?.image)

        weakMemoryCache.garbageCollect(image2)

        assertEquals(image1, weakMemoryCache.get(Key("key1"))?.image)
        assertNull(weakMemoryCache.get(Key("key2")))
        assertEquals(image3, weakMemoryCache.get(Key("key3"))?.image)
    }

    @Test
    fun `empty references are removed from cache`() {
        val key = Key("key")
        val image = reference(FakeImage())

        weakMemoryCache.set(key, image, emptyMap(), 100)
        weakMemoryCache.garbageCollect(image)

        assertNull(weakMemoryCache.get(key))
    }

    @Test
    fun `bitmaps with same key are retrieved by size descending`() {
        val image1 = reference(FakeImage())
        val image2 = reference(FakeImage())
        val image3 = reference(FakeImage())
        val image4 = reference(FakeImage())
        val image5 = reference(FakeImage())
        val image6 = reference(FakeImage())
        val image7 = reference(FakeImage())
        val image8 = reference(FakeImage())

        weakMemoryCache.set(Key("key"), image1, emptyMap(), 1)
        weakMemoryCache.set(Key("key"), image3, emptyMap(), 3)
        weakMemoryCache.set(Key("key"), image5, emptyMap(), 5)
        weakMemoryCache.set(Key("key"), image7, emptyMap(), 7)
        weakMemoryCache.set(Key("key"), image8, emptyMap(), 8)
        weakMemoryCache.set(Key("key"), image4, emptyMap(), 4)
        weakMemoryCache.set(Key("key"), image6, emptyMap(), 6)
        weakMemoryCache.set(Key("key"), image2, emptyMap(), 2)

        assertEquals(image8, weakMemoryCache.get(Key("key"))?.image)
        weakMemoryCache.garbageCollect(image8)

        assertEquals(image7, weakMemoryCache.get(Key("key"))?.image)
        weakMemoryCache.garbageCollect(image7)

        assertEquals(image6, weakMemoryCache.get(Key("key"))?.image)
        weakMemoryCache.garbageCollect(image6)

        assertEquals(image5, weakMemoryCache.get(Key("key"))?.image)
        weakMemoryCache.garbageCollect(image5)

        assertEquals(image4, weakMemoryCache.get(Key("key"))?.image)
        weakMemoryCache.garbageCollect(image4)

        assertEquals(image3, weakMemoryCache.get(Key("key"))?.image)
        weakMemoryCache.garbageCollect(image3)

        assertEquals(image2, weakMemoryCache.get(Key("key"))?.image)
        weakMemoryCache.garbageCollect(image2)

        assertEquals(image1, weakMemoryCache.get(Key("key"))?.image)
        weakMemoryCache.garbageCollect(image1)

        // All the values are invalidated.
        assertNull(weakMemoryCache.get(Key("key")))
    }

    @Test
    fun `cleanUp clears all collected values`() {
        val image1 = reference(FakeImage())
        weakMemoryCache.set(Key("key1"), image1, emptyMap(), 100)

        val image2 = reference(FakeImage())
        weakMemoryCache.set(Key("key2"), image2, emptyMap(), 100)

        val image3 = reference(FakeImage())
        weakMemoryCache.set(Key("key3"), image3, emptyMap(), 100)

        weakMemoryCache.garbageCollect(image1)
        assertNull(weakMemoryCache.get(Key("key1")))

        weakMemoryCache.garbageCollect(image3)
        assertNull(weakMemoryCache.get(Key("key3")))

        assertEquals(3, weakMemoryCache.keys.size)

        weakMemoryCache.cleanUp()

        assertEquals(1, weakMemoryCache.keys.size)

        assertNull(weakMemoryCache.get(Key("key1")))
        assertEquals(image2, weakMemoryCache.get(Key("key2"))?.image)
        assertNull(weakMemoryCache.get(Key("key3")))
    }

    @Test
    fun `value is removed after invalidate is called`() {
        val key = Key("1")
        val image = FakeImage()
        weakMemoryCache.set(key, image, emptyMap(), image.size)
        weakMemoryCache.remove(key)

        assertNull(weakMemoryCache.get(key))
    }

    /**
     * Clears [image]'s weak reference without removing its entry from the cache.
     */
    private fun WeakReferenceMemoryCache.garbageCollect(image: Image) {
        cache.values.forEach { values ->
            values.forEachIndices { value ->
                if (value.image.get() === image) {
                    value.image.clear()
                    return
                }
            }
        }
    }

    /**
     * Hold a strong reference to the image for the duration of the test
     * to prevent it from being garbage collected.
     */
    private fun reference(value: Image): Image {
        references += value
        return value
    }
}
