package coil3.disk

import okio.FileSystem

/** Default to an empty disk cache on JS because we don't have a valid [FileSystem]. */
internal actual fun singletonDiskCache(): DiskCache? = null
