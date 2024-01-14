package coil3.network.ktor.internal

import io.ktor.utils.io.ByteReadChannel
import okio.BufferedSink
import okio.FileSystem
import okio.Path

/** Write a [ByteReadChannel] to [sink] using streaming. */
internal expect suspend fun ByteReadChannel.writeTo(sink: BufferedSink)

/** Write a [ByteReadChannel] to [path] natively. */
internal expect suspend fun ByteReadChannel.writeTo(fileSystem: FileSystem, path: Path)
