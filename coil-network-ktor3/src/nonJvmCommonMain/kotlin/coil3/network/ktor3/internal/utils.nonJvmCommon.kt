package coil3.network.ktor3.internal

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.asByteWriteChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.io.okio.asKotlinxIoRawSink
import okio.BufferedSink
import okio.FileSystem
import okio.Path

internal actual suspend fun ByteReadChannel.writeTo(sink: BufferedSink) {
    copyAndClose(sink.asKotlinxIoRawSink().asByteWriteChannel())
}

internal actual suspend fun ByteReadChannel.writeTo(fileSystem: FileSystem, path: Path) {
    fileSystem.write(path) {
        writeTo(this)
    }
}
