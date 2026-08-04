package coil3.util

import okio.FileSystem
import okio.Path
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSNumber

internal actual fun FileSystem.remainingFreeSpaceBytes(directory: Path): Long {
    val attributes = NSFileManager.defaultManager
        .attributesOfFileSystemForPath(directory.toString(), error = null)
    return (attributes?.get(NSFileSystemFreeSize) as? NSNumber)?.longLongValue
        ?: (4L * 1024 * 1024 * 1024) // 4 GB fallback if unavailable
}

