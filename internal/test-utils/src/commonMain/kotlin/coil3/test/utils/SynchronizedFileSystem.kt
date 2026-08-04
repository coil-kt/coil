package coil3.test.utils

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import okio.Buffer
import okio.FileHandle
import okio.FileMetadata
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.ForwardingSource
import okio.Path
import okio.Sink
import okio.Source

/** Serializes access to [delegate], including returned file handles, sources, and sinks. */
internal class SynchronizedFileSystem(delegate: FileSystem) : ForwardingFileSystem(delegate) {
    private val lock = SynchronizedObject()

    override fun canonicalize(path: Path): Path = synchronized(lock) {
        super.canonicalize(path)
    }

    override fun metadataOrNull(path: Path): FileMetadata? = synchronized(lock) {
        super.metadataOrNull(path)
    }

    override fun list(dir: Path): List<Path> = synchronized(lock) {
        super.list(dir)
    }

    override fun listOrNull(dir: Path): List<Path>? = synchronized(lock) {
        super.listOrNull(dir)
    }

    override fun listRecursively(dir: Path, followSymlinks: Boolean): Sequence<Path> {
        return synchronized(lock) {
            super.listRecursively(dir, followSymlinks).toList()
        }.asSequence()
    }

    override fun openReadOnly(file: Path): FileHandle = synchronized(lock) {
        SynchronizedFileHandle(super.openReadOnly(file), lock)
    }

    override fun openReadWrite(
        file: Path,
        mustCreate: Boolean,
        mustExist: Boolean,
    ): FileHandle = synchronized(lock) {
        SynchronizedFileHandle(super.openReadWrite(file, mustCreate, mustExist), lock)
    }

    override fun source(file: Path): Source = synchronized(lock) {
        SynchronizedSource(super.source(file), lock)
    }

    override fun sink(file: Path, mustCreate: Boolean): Sink = synchronized(lock) {
        SynchronizedSink(super.sink(file, mustCreate), lock)
    }

    override fun appendingSink(file: Path, mustExist: Boolean): Sink = synchronized(lock) {
        SynchronizedSink(super.appendingSink(file, mustExist), lock)
    }

    override fun createDirectory(dir: Path, mustCreate: Boolean) = synchronized(lock) {
        super.createDirectory(dir, mustCreate)
    }

    override fun atomicMove(source: Path, target: Path) = synchronized(lock) {
        super.atomicMove(source, target)
    }

    override fun delete(path: Path, mustExist: Boolean) = synchronized(lock) {
        super.delete(path, mustExist)
    }

    override fun createSymlink(source: Path, target: Path) = synchronized(lock) {
        super.createSymlink(source, target)
    }

    override fun close() = synchronized(lock) {
        super.close()
    }
}

private class SynchronizedFileHandle(
    private val delegate: FileHandle,
    private val fileSystemLock: SynchronizedObject,
) : FileHandle(delegate.readWrite) {
    override fun protectedRead(
        fileOffset: Long,
        array: ByteArray,
        arrayOffset: Int,
        byteCount: Int,
    ): Int = synchronized(fileSystemLock) {
        delegate.read(fileOffset, array, arrayOffset, byteCount)
    }

    override fun protectedWrite(
        fileOffset: Long,
        array: ByteArray,
        arrayOffset: Int,
        byteCount: Int,
    ) = synchronized(fileSystemLock) {
        delegate.write(fileOffset, array, arrayOffset, byteCount)
    }

    override fun protectedFlush() = synchronized(fileSystemLock) {
        delegate.flush()
    }

    override fun protectedResize(size: Long) = synchronized(fileSystemLock) {
        delegate.resize(size)
    }

    override fun protectedSize(): Long = synchronized(fileSystemLock) {
        delegate.size()
    }

    override fun protectedClose() = synchronized(fileSystemLock) {
        delegate.close()
    }
}

private class SynchronizedSource(
    delegate: Source,
    private val lock: SynchronizedObject,
) : ForwardingSource(delegate) {
    override fun read(sink: Buffer, byteCount: Long): Long = synchronized(lock) {
        super.read(sink, byteCount)
    }

    override fun close() = synchronized(lock) {
        super.close()
    }
}

private class SynchronizedSink(
    private val delegate: Sink,
    private val lock: SynchronizedObject,
) : Sink {
    override fun write(source: Buffer, byteCount: Long) = synchronized(lock) {
        delegate.write(source, byteCount)
    }

    override fun flush() = synchronized(lock) {
        delegate.flush()
    }

    override fun timeout() = delegate.timeout()

    override fun close() = synchronized(lock) {
        delegate.close()
    }
}
