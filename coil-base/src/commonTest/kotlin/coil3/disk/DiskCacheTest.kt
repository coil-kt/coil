package coil3.disk

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import okio.fakefilesystem.FakeFileSystem
import okio.use

class DiskCacheTest {

    private lateinit var fileSystem: FakeFileSystem
    private lateinit var diskCache: DiskCache

    @BeforeTest
    fun before() {
        fileSystem = FakeFileSystem()
        diskCache = DiskCache.Builder()
            .directory(fileSystem.workingDirectory)
            .fileSystem(fileSystem)
            .maxSizeBytes(Long.MAX_VALUE)
            .build()
    }

    @AfterTest
    fun after() {
        diskCache.shutdown()
    }

    @Test
    fun canReadAndWriteEmpty() {
        diskCache.openSnapshot("test").use { assertNull(it) }
        diskCache.openEditor("test")?.use { /* Empty edit to create the file on disk. */ }
        diskCache.openSnapshot("test").use { assertNotNull(it) }
    }

    @Test
    fun canReadAndWriteData() {
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
    fun canRemoveSingularEntries() {
        diskCache.openEditor("test1")!!.use { /* Empty edit to create the file on disk. */ }
        diskCache.openEditor("test2")!!.use { /* Empty edit to create the file on disk. */ }
        assertTrue(diskCache.remove("test1"))
        diskCache.openSnapshot("test1").use { assertNull(it) }
        diskCache.openSnapshot("test2").use { assertNotNull(it) }
    }

    @Test
    fun canClearAllEntries() {
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
