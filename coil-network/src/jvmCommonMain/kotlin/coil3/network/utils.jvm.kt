package coil3.network

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.nio.copyTo
import java.io.RandomAccessFile
import okio.BufferedSink
import okio.FileSystem
import okio.Path

internal actual suspend fun ByteReadChannel.writeTo(sink: BufferedSink) {
    copyTo(sink)
}

internal actual suspend fun ByteReadChannel.writeTo(fileSystem: FileSystem, path: Path) {
    if (fileSystem === FileSystem.SYSTEM) {
        // Fast path: normal jvm File, write to FileChannel directly.
        RandomAccessFile(path.toFile(), "rw").use { file ->
            val copied = copyTo(file.channel)
            file.setLength(copied) // truncate tail that could remain from the previously written data
        }
    } else {
        // Slow path: cannot guarantee a "real" file.
        fileSystem.write(path) {
            copyTo(this)
        }
    }
}
