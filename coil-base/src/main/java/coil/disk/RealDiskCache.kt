package coil.disk

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
        valueCount = 2
    )

    override val size get() = cache.size()

    override fun openReader(key: String): DiskCache.Reader? {
        return cache[key.hash()]?.let(::RealReader)
    }

    override fun openWriter(key: String): DiskCache.Writer? {
        return cache.edit(key.hash())?.let(::RealWriter)
    }

    override fun remove(key: String): Boolean {
        return cache.remove(key.hash())
    }

    override fun clear() {
        cache.evictAll()
    }

    private fun String.hash() = encodeUtf8().sha256().hex()

    private class RealReader(private val snapshot: DiskLruCache.Snapshot) : DiskCache.Reader {

        override val metadata get() = snapshot.file(ENTRY_METADATA)
        override val data get() = snapshot.file(ENTRY_DATA)

        override fun close() = snapshot.close()
        override fun closeAndOpenWriter() = snapshot.closeAndEdit()?.let(::RealWriter)
    }

    private class RealWriter(private val editor: DiskLruCache.Editor) : DiskCache.Writer {

        override val metadata get() = editor.file(ENTRY_METADATA)
        override val data get() = editor.file(ENTRY_DATA)

        override fun commit() = editor.commit()
        override fun commitAndOpenReader() = editor.commitAndGet()?.let(::RealReader)
        override fun abort() = editor.abort()
    }

    companion object {
        private const val ENTRY_METADATA = 0
        private const val ENTRY_DATA = 1
    }
}
