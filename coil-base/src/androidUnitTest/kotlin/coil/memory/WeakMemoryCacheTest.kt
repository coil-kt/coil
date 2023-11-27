package coil.memory

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.Image
import coil.asCoilImage
import coil.memory.MemoryCache.Key
import coil.util.createBitmap
import coil.util.forEachIndices
import coil.util.toDrawable
import kotlin.experimental.ExperimentalNativeApi
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21, 28])
class WeakMemoryCacheTest {

    private lateinit var context: Context
    private lateinit var weakMemoryCache: WeakReferenceMemoryCache
    private lateinit var references: MutableSet<Image>

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        weakMemoryCache = WeakReferenceMemoryCache()
        references = mutableSetOf()
    }

    @Test
    fun `can retrieve cached value`() {
        val key = Key("key")
        val image = reference(createBitmap().toDrawable(context).asCoilImage())
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
        val image1 = reference(createBitmap().toDrawable(context).asCoilImage())
        weakMemoryCache.set(Key("key1"), image1, emptyMap(), 100)

        val image2 = reference(createBitmap().toDrawable(context).asCoilImage())
        weakMemoryCache.set(Key("key2"), image2, emptyMap(), 100)

        val image3 = reference(createBitmap().toDrawable(context).asCoilImage())
        weakMemoryCache.set(Key("key3"), image3, emptyMap(), 100)

        assertEquals(image1, weakMemoryCache.get(Key("key1"))?.image)
        assertEquals(image2, weakMemoryCache.get(Key("key2"))?.image)
        assertEquals(image3, weakMemoryCache.get(Key("key3"))?.image)

        weakMemoryCache.clear(image2)

        assertEquals(image1, weakMemoryCache.get(Key("key1"))?.image)
        assertNull(weakMemoryCache.get(Key("key2")))
        assertEquals(image3, weakMemoryCache.get(Key("key3"))?.image)
    }

    @Test
    fun `empty references are removed from cache`() {
        val key = Key("key")
        val image = reference(createBitmap().toDrawable(context).asCoilImage())

        weakMemoryCache.set(key, image, emptyMap(), 100)
        weakMemoryCache.clear(image)

        assertNull(weakMemoryCache.get(key))
    }

    @Test
    fun `bitmaps with same key are retrieved by size descending`() {
        val image1 = reference(createBitmap().toDrawable(context).asCoilImage())
        val image2 = reference(createBitmap().toDrawable(context).asCoilImage())
        val image3 = reference(createBitmap().toDrawable(context).asCoilImage())
        val image4 = reference(createBitmap().toDrawable(context).asCoilImage())
        val image5 = reference(createBitmap().toDrawable(context).asCoilImage())
        val image6 = reference(createBitmap().toDrawable(context).asCoilImage())
        val image7 = reference(createBitmap().toDrawable(context).asCoilImage())
        val image8 = reference(createBitmap().toDrawable(context).asCoilImage())

        weakMemoryCache.set(Key("key"), image1, emptyMap(), 1)
        weakMemoryCache.set(Key("key"), image3, emptyMap(), 3)
        weakMemoryCache.set(Key("key"), image5, emptyMap(), 5)
        weakMemoryCache.set(Key("key"), image7, emptyMap(), 7)
        weakMemoryCache.set(Key("key"), image8, emptyMap(), 8)
        weakMemoryCache.set(Key("key"), image4, emptyMap(), 4)
        weakMemoryCache.set(Key("key"), image6, emptyMap(), 6)
        weakMemoryCache.set(Key("key"), image2, emptyMap(), 2)

        assertEquals(image8, weakMemoryCache.get(Key("key"))?.image)
        weakMemoryCache.clear(image8)

        assertEquals(image7, weakMemoryCache.get(Key("key"))?.image)
        weakMemoryCache.clear(image7)

        assertEquals(image6, weakMemoryCache.get(Key("key"))?.image)
        weakMemoryCache.clear(image6)

        assertEquals(image5, weakMemoryCache.get(Key("key"))?.image)
        weakMemoryCache.clear(image5)

        assertEquals(image4, weakMemoryCache.get(Key("key"))?.image)
        weakMemoryCache.clear(image4)

        assertEquals(image3, weakMemoryCache.get(Key("key"))?.image)
        weakMemoryCache.clear(image3)

        assertEquals(image2, weakMemoryCache.get(Key("key"))?.image)
        weakMemoryCache.clear(image2)

        assertEquals(image1, weakMemoryCache.get(Key("key"))?.image)
        weakMemoryCache.clear(image1)

        // All the values are invalidated.
        assertNull(weakMemoryCache.get(Key("key")))
    }

    @Test
    fun `cleanUp clears all collected values`() {
        val image1 = reference(createBitmap().toDrawable(context).asCoilImage())
        weakMemoryCache.set(Key("key1"), image1, emptyMap(), 100)

        val image2 = reference(createBitmap().toDrawable(context).asCoilImage())
        weakMemoryCache.set(Key("key2"), image2, emptyMap(), 100)

        val image3 = reference(createBitmap().toDrawable(context).asCoilImage())
        weakMemoryCache.set(Key("key3"), image3, emptyMap(), 100)

        weakMemoryCache.clear(image1)
        assertNull(weakMemoryCache.get(Key("key1")))

        weakMemoryCache.clear(image3)
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
        val image = createBitmap().toDrawable(context).asCoilImage()
        weakMemoryCache.set(key, image, emptyMap(), image.size)
        weakMemoryCache.remove(key)

        assertNull(weakMemoryCache.get(key))
    }

    /**
     * Clears [image]'s weak reference without removing its entry from
     * [WeakReferenceMemoryCache.clear]. This simulates garbage collection.
     */
    @OptIn(ExperimentalNativeApi::class)
    private fun WeakReferenceMemoryCache.clear(image: Image) {
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
