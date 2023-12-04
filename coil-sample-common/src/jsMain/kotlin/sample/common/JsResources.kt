package sample.common

import kotlinx.browser.window
import kotlinx.coroutines.await
import okio.Buffer
import okio.Source
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array

class JsResources : Resources {
    override suspend fun open(path: String): Source {
        val resPath = "./$path"
        val response = window.fetch(resPath).await()
        check(response.ok) { "unknown path: $resPath" }
        val byteArray = response.arrayBuffer().await().toByteArray()
        return Buffer().apply { write(byteArray) }
    }
}

private fun ArrayBuffer.toByteArray(): ByteArray {
    return Int8Array(this, 0, byteLength).unsafeCast<ByteArray>()
}
