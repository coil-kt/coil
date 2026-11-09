package coil3.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

class FileSystemsTest {

    private val fileSystem = AtomicMoveUnsupportedFileSystem(
        FakeFileSystem().apply { emulateWindows() },
    )
    private val directory = "C:\\cache".toPath()

    @Test
    fun moveFileFallsBackToCopyAndDeleteIfAtomicMoveIsUnsupported() {
        fileSystem.createDirectories(directory)
        val source = directory / "journal.tmp"
        val target = directory / "journal"
        fileSystem.write(source) { writeUtf8("journal") }

        fileSystem.moveFile(source, target)

        assertFalse(fileSystem.exists(source))
        assertEquals("journal", fileSystem.read(target) { readUtf8() })
    }

    @Test
    fun moveFileOverwritesAnExistingTargetIfAtomicMoveIsUnsupported() {
        fileSystem.createDirectories(directory)
        val source = directory / "journal.tmp"
        val target = directory / "journal"
        fileSystem.write(source) { writeUtf8("new") }
        fileSystem.write(target) { writeUtf8("old value that is longer") }

        fileSystem.moveFile(source, target)

        assertFalse(fileSystem.exists(source))
        assertEquals("new", fileSystem.read(target) { readUtf8() })
    }

    @Test
    fun moveFileThrowsTheOriginalExceptionIfTheFallbackAlsoFails() {
        fileSystem.createDirectories(directory)
        val source = directory / "journal.tmp"
        val target = directory / "journal"

        val exception = assertFailsWith<IOException> {
            fileSystem.moveFile(source, target)
        }

        assertEquals("atomic move is not supported", exception.message)
        assertFalse(fileSystem.exists(target))
    }

    @Test
    fun moveFileDoesNotCopyIfAtomicMoveSucceeds() {
        val fileSystem = FakeFileSystem().apply { emulateUnix() }
        fileSystem.createDirectories("/cache".toPath())
        val source = "/cache/journal.tmp".toPath()
        val target = "/cache/journal".toPath()
        fileSystem.write(source) { writeUtf8("journal") }

        fileSystem.moveFile(source, target)

        assertTrue(fileSystem.exists(target))
        assertFalse(fileSystem.exists(source))
    }

    /** Emulates a file system that cannot move files atomically, e.g. Windows MSIX. */
    private class AtomicMoveUnsupportedFileSystem(
        delegate: FileSystem,
    ) : ForwardingFileSystem(delegate) {
        override fun atomicMove(source: Path, target: Path) {
            throw IOException("atomic move is not supported")
        }
    }
}
