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

    override val size get() = cache.size()

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

    private fun String.hash() = encodeUtf8().sha256().hex()

    private class RealSnapshot(private val snapshot: DiskLruCache.Snapshot) : Snapshot {

        override val metadata get() = snapshot.entry.cleanFiles[ENTRY_METADATA].toFile()
        override val data get() = snapshot.entry.cleanFiles[ENTRY_BODY].toFile()

        override fun close() = snapshot.close()
    }

    private class RealEditor(private val editor: DiskLruCache.Editor): Editor {

        override val metadata get() = editor.entry.dirtyFiles[ENTRY_METADATA].toFile()
        override val data get() = editor.entry.dirtyFiles[ENTRY_BODY].toFile()

        override fun commit() = editor.commit()
        override fun commitAndGet() = editor.commitAndGet()?.let(::RealSnapshot)
        override fun abort() = editor.abort()
    }

    companion object {
        private const val ENTRY_METADATA = 0
        private const val ENTRY_BODY = 1
    }
}
