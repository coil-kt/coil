package coil3.fetch

import kotlin.js.Promise
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get

internal actual suspend fun fetchBlobBytes(url: String): ByteArray {
    val buffer: ArrayBuffer = fetchArrayBuffer(url).await()
    val int8 = Int8Array(buffer)
    return ByteArray(int8.length) { int8[it] }
}

private fun fetchArrayBuffer(url: String): Promise<ArrayBuffer> =
    js("fetch(url).then(function(response) { return response.arrayBuffer(); })")
