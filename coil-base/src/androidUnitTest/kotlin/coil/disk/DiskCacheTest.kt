package coil.disk

import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DiskCacheTest {

    private lateinit var diskCache: DiskCache

    @Before
    fun before() {
        diskCache = DiskCache.Builder()
            .directory(File("build/cache"))
            .build()
    }

    @After
    fun after() {
        diskCache.clear()
        diskCache.fileSystem.deleteRecursively(diskCache.directory) // Ensure we start fresh.
    }

    @Test
    fun `can read and write empty`() {
        diskCache.openSnapshot("test").use { assertNull(it) }
        diskCache.openEditor("test")?.use { /* Empty edit to create the file on disk. */ }
        diskCache.openSnapshot("test").use { assertNotNull(it) }
    }

    @Test
    fun `can read and write data`() {
        assertEquals(0, diskCache.size)
        diskCache.openSnapshot("test").use { assertNull(it) }

        diskCache.openEditor("test")!!.use { editor ->
            diskCache.fileSystem.write(editor.metadata) {
                writeDecimalLong(12345).writeByte('\n'.code)
            }
            diskCache.fileSystem.write(editor.data) {
                writeDecimalLong(54321).writeByte('\n'.code)
            }
        }

        assertTrue(diskCache.size > 0)

        diskCache.openSnapshot("test")!!.use { snapshot ->
            assertEquals(
                12345,
                diskCache.fileSystem.read(snapshot.metadata) { readUtf8LineStrict().toLong() }
            )
            assertEquals(
                54321,
                diskCache.fileSystem.read(snapshot.data) { readUtf8LineStrict().toLong() }
            )
        }
    }

    @Test
    fun `can remove singular entries`() {
        diskCache.openEditor("test1")!!.use { /* Empty edit to create the file on disk. */ }
        diskCache.openEditor("test2")!!.use { /* Empty edit to create the file on disk. */ }
        assertTrue(diskCache.remove("test1"))
        diskCache.openSnapshot("test1").use { assertNull(it) }
        diskCache.openSnapshot("test2").use { assertNotNull(it) }
    }

    @Test
    fun `can clear all entries`() {
        diskCache.openEditor("test1")!!.use { /* Empty edit to create the file on disk. */ }
        diskCache.openEditor("test2")!!.use { /* Empty edit to create the file on disk. */ }
        diskCache.clear()
        diskCache.openSnapshot("test1").use { assertNull(it) }
        diskCache.openSnapshot("test2").use { assertNull(it) }
    }

    private inline fun <T : DiskCache.Editor?, R> T.use(block: (T) -> R): R {
        try {
            return block(this).also { this?.commit() }
        } catch (e: Exception) {
            this?.abort()
            throw e
        }
    }
}
