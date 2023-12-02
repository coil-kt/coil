package coil3.util

import okio.FileSystem
import okio.Path

internal actual fun FileSystem.remainingFreeSpaceBytes(directory: Path): Long {
    return directory.toFile().freeSpace
}
