package coil3.util

import coil3.disk.DiskCache
import coil3.test.utils.RobolectricTest
import kotlin.test.assertEquals
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.robolectric.shadows.ShadowStatFs

/** Regression tests: https://github.com/coil-kt/coil/issues/3622 */
class FileSystemsTest : RobolectricTest() {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var directory: Path

    @Before
    fun before() {
        val file = temporaryFolder.newFolder()
        directory = file.toOkioPath()
        // 64 GiB total, 400 MiB free including reserved blocks, and 200 MiB available.
        ShadowStatFs.registerStats(file.absolutePath, 16_777_216, 102_400, 51_200)
    }

    @Test
    fun `remaining free space excludes used and reserved blocks`() {
        assertEquals(200L * 1024 * 1024, FileSystem.SYSTEM.remainingFreeSpaceBytes(directory))
    }

    @Test
    fun `disk cache max size percent uses available space`() {
        val diskCache = DiskCache.Builder()
            .directory(directory)
            .maxSizePercent(0.02)
            .build()
        try {
            assertEquals(10L * 1024 * 1024, diskCache.maxSize)
        } finally {
            diskCache.shutdown()
        }
    }
}
