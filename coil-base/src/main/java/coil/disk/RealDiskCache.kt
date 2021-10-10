package coil.disk

import coil.disk.DiskCache.Editor
import coil.disk.DiskCache.Snapshot
import okio.ByteString.Companion.encodeUtf8
import okio.ExperimentalFileSystem
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.io.File

@OptIn(ExperimentalFileSystem::class)
internal class RealDiskCache(
    override val maxSize: Long,
    override val directory: File
) : DiskCache {

    private val cache = DiskLruCache(
        fileSystem = FileSystem.SYSTEM,
        directory = directory.toOkioPath(),
        maxSize = maxSize,
        appVersion = 1,
        valueCount = 2
    )

    override val size get(): Long = cache.size()

    override fun get(key: String): Snapshot? {
        return cache[key.hash()]?.let(::RealSnapshot)
    }

    override fun edit(key: String): Editor? {
        return cache.edit(key.hash())?.let(::RealEditor)
    }

    override fun remove(key: String): Boolean {
        return cache.remove(key.hash())
    }

    override fun clear() {
        cache.evictAll()
    }

    private fun String.hash(): String = encodeUtf8().sha256().hex()

    private class RealSnapshot(private val snapshot: DiskLruCache.Snapshot) : Snapshot {

        override val metadata get(): File = snapshot.entry.cleanFiles[ENTRY_METADATA].toFile()
        override val data get(): File = snapshot.entry.cleanFiles[ENTRY_DATA].toFile()

        override fun close(): Unit = snapshot.close()
        override fun closeAndEdit(): Editor? = snapshot.closeAndEdit()?.let(::RealEditor)
    }

    private class RealEditor(private val editor: DiskLruCache.Editor) : Editor {

        override val metadata get(): File = editor.entry.dirtyFiles[ENTRY_METADATA].toFile()
        override val data get(): File = editor.entry.dirtyFiles[ENTRY_DATA].toFile()

        override fun commit(): Unit = editor.commit()
        override fun commitAndGet(): Snapshot? = editor.commitAndGet()?.let(::RealSnapshot)
        override fun abort(): Unit = editor.abort()
    }

    companion object {
        private const val ENTRY_METADATA = 0
        private const val ENTRY_DATA = 1
    }
}
