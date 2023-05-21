package coil.disk

import coil.disk.DiskCache.Editor
import coil.disk.DiskCache.Snapshot
import kotlinx.coroutines.CoroutineDispatcher
import okio.ByteString.Companion.encodeUtf8
import okio.FileSystem
import okio.Path

internal class RealDiskCache(
    override val maxSize: Long,
    override val directory: Path,
    override val fileSystem: FileSystem,
    cleanupDispatcher: CoroutineDispatcher
) : DiskCache {

    private val cache = DiskLruCache(
        fileSystem = fileSystem,
        directory = directory,
        cleanupDispatcher = cleanupDispatcher,
        maxSize = maxSize,
        appVersion = 1,
        valueCount = 2,
    )

    override val size get() = cache.size()

    override fun openSnapshot(key: String): Snapshot? {
        return cache[key.hash()]?.let(::RealSnapshot)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun get(key: String) = openSnapshot(key)

    override fun openEditor(key: String): Editor? {
        return cache.edit(key.hash())?.let(::RealEditor)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun edit(key: String) = openEditor(key)

    override fun remove(key: String): Boolean {
        return cache.remove(key.hash())
    }

    override fun clear() {
        cache.evictAll()
    }

    private fun String.hash() = encodeUtf8().sha256().hex()

    private class RealSnapshot(private val snapshot: DiskLruCache.Snapshot) : Snapshot {

        override val metadata get() = snapshot.file(ENTRY_METADATA)
        override val data get() = snapshot.file(ENTRY_DATA)

        override fun close() = snapshot.close()
        override fun closeAndOpenEditor() = snapshot.closeAndEdit()?.let(::RealEditor)
        @Suppress("OVERRIDE_DEPRECATION")
        override fun closeAndEdit() = closeAndOpenEditor()
    }

    private class RealEditor(private val editor: DiskLruCache.Editor) : Editor {

        override val metadata get() = editor.file(ENTRY_METADATA)
        override val data get() = editor.file(ENTRY_DATA)

        override fun commit() = editor.commit()
        override fun commitAndOpenSnapshot() = editor.commitAndGet()?.let(::RealSnapshot)
        @Suppress("OVERRIDE_DEPRECATION")
        override fun commitAndGet() = commitAndOpenSnapshot()
        override fun abort() = editor.abort()
    }

    companion object {
        private const val ENTRY_METADATA = 0
        private const val ENTRY_DATA = 1
    }
}
