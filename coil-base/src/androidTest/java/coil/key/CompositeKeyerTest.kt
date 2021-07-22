package coil.key

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.request.Options
import coil.util.copyAssetToFile
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CompositeKeyerTest {

    private lateinit var context: Context

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun fileCacheKeyWithLastModified() {
        val file = context.copyAssetToFile("normal.jpg")
        val keyer = CompositeKeyer(addLastModifiedToFileCacheKey = true)

        file.setLastModified(1234L)
        val firstKey = keyer.key(file, Options(context))

        file.setLastModified(4321L)
        val secondKey = keyer.key(file, Options(context))

        assertNotEquals(secondKey, firstKey)
    }

    @Test
    fun fileCacheKeyWithoutLastModified() {
        val file = context.copyAssetToFile("normal.jpg")
        val keyer = CompositeKeyer(addLastModifiedToFileCacheKey = false)

        file.setLastModified(1234L)
        val firstKey = keyer.key(file, Options(context))

        file.setLastModified(4321L)
        val secondKey = keyer.key(file, Options(context))

        assertEquals(secondKey, firstKey)
    }
}
