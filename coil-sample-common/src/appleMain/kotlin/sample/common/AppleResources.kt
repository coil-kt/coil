package sample.common

import okio.FileSystem
import okio.Path.Companion.toPath
import okio.Source
import platform.Foundation.NSBundle

class AppleResources : Resources {
    override suspend fun open(path: String): Source {
        val file = NSBundle.mainBundle.resourcePath!!.toPath() / "compose-resources" / path
        return FileSystem.SYSTEM.openReadOnly(file).source()
    }
}
