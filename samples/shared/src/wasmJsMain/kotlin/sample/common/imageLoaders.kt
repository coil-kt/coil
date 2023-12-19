package sample.common

import coil3.disk.DiskCache

// We can't write to the file system from a web browser.
internal actual fun newDiskCache(): DiskCache? {
    return null
}
