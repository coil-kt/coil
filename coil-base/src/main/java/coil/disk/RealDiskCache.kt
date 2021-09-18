package coil.disk

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

    override val keys get() = cache.keys()

    override fun remove(key: String): Boolean {
        return cache.remove(key.toDiskCacheKey())
    }

    override fun clear() {
        cache.evictAll()
    }

    private fun String.toDiskCacheKey() = encodeUtf8().md5().hex()

    companion object {
        private const val ENTRY_METADATA = 0
        private const val ENTRY_BODY = 1
        private const val ENTRY_COUNT = 2
    }
}
