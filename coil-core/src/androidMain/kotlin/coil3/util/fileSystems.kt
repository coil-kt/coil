package coil3.util

import android.os.StatFs
import okio.FileSystem
import okio.Path

internal actual fun FileSystem.remainingFreeSpaceBytes(directory: Path): Long {
    val stats = StatFs(directory.toFile().apply { mkdir() }.absolutePath)
    return stats.blockCountLong * stats.blockSizeLong
}
