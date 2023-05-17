package coil.disk

import okio.FileSystem
import okio.Path

internal actual fun FileSystem.remainingFreeSpaceBytes(directory: Path): Long {
    // TODO: Figure out how to compute remaining free space.
    return 4_294_967_296
}
