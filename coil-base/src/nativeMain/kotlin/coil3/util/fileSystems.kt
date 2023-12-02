package coil3.util

import okio.FileSystem

internal actual fun defaultFileSystem(): FileSystem = FileSystem.SYSTEM
