package coil.disk

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlin.test.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test

class DiskCacheAndroidTest {

    private lateinit var context: Context
    private lateinit var directory: File

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        directory = context.filesDir.resolve("test_dir")
    }

    @After
    fun after() {
        directory.delete() // Ensure we start fresh.
    }

    @Test
    fun checkMaxSize() {
        val minimumMaxSizeBytes = 100L
        val maximumMaxSizeBytes = 200L
        val maxSizePercent = 0.5

        val diskCache = DiskCache.Builder()
            .directory(directory)
            .maxSizePercent(maxSizePercent)
            .minimumMaxSizeBytes(minimumMaxSizeBytes)
            .maximumMaxSizeBytes(maximumMaxSizeBytes)
            .build()

        assertEquals(maximumMaxSizeBytes, diskCache.maxSize)
    }
}
