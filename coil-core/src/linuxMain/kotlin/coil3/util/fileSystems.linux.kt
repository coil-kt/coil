package coil3.util

import okio.FileSystem
import okio.Path

internal actual fun FileSystem.remainingFreeSpaceBytes(directory: Path): Long {
    // TODO: Compute real free disk space on Linux using statvfs().
    return 4L * 1024 * 1024 * 1024 // 4 GB
}

