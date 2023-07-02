package coil.util

import okio.FileHandle
import okio.FileMetadata
import okio.FileNotFoundException
import okio.FileSystem
import okio.Path
import okio.Sink
import okio.Source

internal actual fun defaultFileSystem(): FileSystem = EmptyReadOnlyFileSystem

/** An empty, read-only file system that throws if any write methods are called. */
private object EmptyReadOnlyFileSystem : FileSystem() {

    override fun atomicMove(source: Path, target: Path) {
        throwWriteIsUnsupported()
    }

    override fun canonicalize(path: Path): Path {
        throwFileNotFound(path)
    }

    override fun createDirectory(dir: Path, mustCreate: Boolean) {
        throwWriteIsUnsupported()
    }

    override fun createSymlink(source: Path, target: Path) {
        throwWriteIsUnsupported()
    }

    override fun delete(path: Path, mustExist: Boolean) {
        throwWriteIsUnsupported()
    }

    override fun list(dir: Path): List<Path> {
        throwFileNotFound(dir)
    }

    override fun listOrNull(dir: Path): List<Path>? {
        return null
    }

    override fun metadataOrNull(path: Path): FileMetadata? {
        return null
    }

    override fun openReadOnly(file: Path): FileHandle {
        return EmptyFileHandle
    }

    override fun openReadWrite(file: Path, mustCreate: Boolean, mustExist: Boolean): FileHandle {
        if (mustCreate) throwWriteIsUnsupported()
        if (mustExist) throwFileNotFound(file)
        return EmptyFileHandle
    }

    override fun sink(file: Path, mustCreate: Boolean): Sink {
        throwWriteIsUnsupported()
    }

    override fun appendingSink(file: Path, mustExist: Boolean): Sink {
        throwWriteIsUnsupported()
    }

    override fun source(file: Path): Source {
        throwFileNotFound(file)
    }

    private fun throwWriteIsUnsupported(): Nothing {
        throw UnsupportedOperationException("FileSystem write methods are unsupported on Kotlin JS.")
    }

    private fun throwFileNotFound(path: Path): Nothing {
        throw FileNotFoundException("No such file: $path")
    }

    private object EmptyFileHandle : FileHandle(readWrite = false) {

        override fun protectedSize(): Long = 0

        override fun protectedResize(size: Long) {}

        override fun protectedRead(
            fileOffset: Long,
            array: ByteArray,
            arrayOffset: Int,
            byteCount: Int,
        ): Int = -1

        override fun protectedWrite(
            fileOffset: Long,
            array: ByteArray,
            arrayOffset: Int,
            byteCount: Int,
        ) {}

        override fun protectedClose() {}

        override fun protectedFlush() {}
    }
}
