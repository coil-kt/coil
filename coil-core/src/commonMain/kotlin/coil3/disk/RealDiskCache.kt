package coil3.disk

import coil3.disk.DiskCache.Editor
import coil3.disk.DiskCache.Snapshot
import coil3.util.closeQuietly
import kotlinx.coroutines.CoroutineDispatcher
import okio.ByteString.Companion.encodeUtf8
import okio.FileSystem
import okio.Path

internal class RealDiskCache(
    override val maxSize: Long,
    override val directory: Path,
    override val fileSystem: FileSystem,
    cleanupDispatcher: CoroutineDispatcher,
) : DiskCache {

    private val cache = DiskLruCache(
        fileSystem = fileSystem,
        directory = directory,
        cleanupDispatcher = cleanupDispatcher,
        maxSize = maxSize,
        appVersion = 2,
        valueCount = 2,
    )

    override val size get() = cache.size()

    override fun openSnapshot(key: String): Snapshot? {
        return cache[key.hash()]?.let(::RealSnapshot)
    }

    override fun openEditor(key: String): Editor? {
        return cache.edit(key.hash())?.let(::RealEditor)
    }

    override fun remove(key: String): Boolean {
        return cache.remove(key.hash())
    }

    override fun clear() {
        cache.evictAll()
    }

    override fun shutdown() {
        cache.closeQuietly()
    }

    private fun String.hash() = encodeUtf8().sha256().hex()

    private class RealSnapshot(private val snapshot: DiskLruCache.Snapshot) : Snapshot {

        override val metadata get() = snapshot.file(ENTRY_METADATA)
        override val data get() = snapshot.file(ENTRY_DATA)

        override fun close() = snapshot.close()
        override fun closeAndOpenEditor() = snapshot.closeAndEdit()?.let(::RealEditor)
    }

    private class RealEditor(private val editor: DiskLruCache.Editor) : Editor {

        override val metadata get() = editor.file(ENTRY_METADATA)
        override val data get() = editor.file(ENTRY_DATA)

        override fun commit() = editor.commit()
        override fun commitAndOpenSnapshot() = editor.commitAndGet()?.let(::RealSnapshot)
        override fun abort() = editor.abort()
    }

    companion object {
        private const val ENTRY_METADATA = 0
        private const val ENTRY_DATA = 1
    }
}
