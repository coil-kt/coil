package coil3.util

import okio.FileSystem
import okio.Path

internal actual fun FileSystem.remainingFreeSpaceBytes(directory: Path): Long {
    // TODO: Figure out how to compute remaining free space.
    return 4L * 1024 * 1024 * 1024 // 4 GB
}
