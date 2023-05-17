package coil.disk

import okio.FileSystem
import okio.Path

internal expect fun FileSystem.remainingFreeSpaceBytes(directory: Path): Long
