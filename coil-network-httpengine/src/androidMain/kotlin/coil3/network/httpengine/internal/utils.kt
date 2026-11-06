package coil3.network.httpengine.internal

import coil3.network.NetworkRequestBody
import okio.Buffer

internal suspend fun NetworkRequestBody.readByteArray(): ByteArray {
    val buffer = Buffer()
    writeTo(buffer)
    return buffer.readByteArray()
}
