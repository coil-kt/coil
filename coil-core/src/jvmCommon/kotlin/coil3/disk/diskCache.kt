package coil3.disk

import java.io.File
import okio.Path.Companion.toOkioPath

/**
 * Set the [directory] where the cache stores its data.
 *
 * IMPORTANT: It is an error to have two [DiskCache] instances active in the same
 * directory at the same time as this can corrupt the disk cache.
 */
fun DiskCache.Builder.directory(directory: File) = directory(directory.toOkioPath())
