package coil3.network

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.nio.copyTo
import okio.BufferedSink

internal actual suspend fun ByteReadChannel.writeTo(sink: BufferedSink) {
    copyTo(sink)
}
