package coil.util

import java.io.File
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath

internal actual fun defaultFileSystem(): FileSystem = FileSystem.SYSTEM

internal actual fun FileSystem.createTempFile(directory: Path): Path {
    return File.createTempFile("tmp", null, directory.toFile()).toOkioPath()
}
