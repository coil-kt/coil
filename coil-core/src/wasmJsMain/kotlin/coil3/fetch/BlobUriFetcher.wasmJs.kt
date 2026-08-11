@file:OptIn(ExperimentalWasmJsInterop::class)

package coil3.fetch

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.Promise
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.fetch.Response

internal actual suspend fun fetchBlob(url: String): BlobFetchResult {
    val response = fetchResponse(url).await<Response>()
    val buffer = response.arrayBuffer().await<ArrayBuffer>()
    val int8 = Int8Array(buffer)
    return BlobFetchResult(
        bytes = ByteArray(int8.length) { int8[it] },
        mimeType = response.headers.get("content-type"),
    )
}

private fun fetchResponse(url: String): Promise<Response> = js("fetch(url)")
