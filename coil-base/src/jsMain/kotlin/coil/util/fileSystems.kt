package coil.util

import okio.FileSystem
import okio.NodeJsFileSystem

internal actual fun defaultFileSystem(): FileSystem = NodeJsFileSystem
