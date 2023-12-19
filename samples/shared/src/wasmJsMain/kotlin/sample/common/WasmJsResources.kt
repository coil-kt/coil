package sample.common

import kotlinx.browser.window
import kotlinx.coroutines.await
import okio.Buffer
import okio.Source
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.fetch.Response

class WasmJsResources : Resources {

    override val root: String
        get() = ""

    override suspend fun open(path: String): Source {
        val response = window.fetch("./$path").await<Response>()
        check(response.ok) { "unknown path: $path" }
        val byteArray = response.arrayBuffer().await<ArrayBuffer>().toByteArray()
        return Buffer().apply { write(byteArray) }
    }
}

private fun ArrayBuffer.toByteArray(): ByteArray {
    val int8Array = Int8Array(this, 0, byteLength)
    return ByteArray(int8Array.length, int8Array::get)
}
