package coil3.disk

import kotlin.test.assertEquals
import okio.fakefilesystem.FakeFileSystem
import org.junit.Test

class DiskCacheAndroidTest {

    @Test
    fun checkMaxSize() {
        val minimumMaxSizeBytes = 100L
        val maximumMaxSizeBytes = 200L
        val maxSizePercent = 0.5

        val diskCache = DiskCache.Builder()
            .directory(FakeFileSystem().workingDirectory)
            .maxSizePercent(maxSizePercent)
            .minimumMaxSizeBytes(minimumMaxSizeBytes)
            .maximumMaxSizeBytes(maximumMaxSizeBytes)
            .build()

        assertEquals(maximumMaxSizeBytes, diskCache.maxSize)
    }
}
