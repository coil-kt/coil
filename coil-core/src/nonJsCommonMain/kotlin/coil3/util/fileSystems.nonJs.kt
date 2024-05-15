package coil3.util

import okio.FileSystem
import okio.SYSTEM

internal actual fun defaultFileSystem(): FileSystem = FileSystem.SYSTEM
