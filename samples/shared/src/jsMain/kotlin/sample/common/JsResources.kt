package sample.common

import kotlinx.browser.window
import kotlinx.coroutines.await
import okio.Buffer
import okio.Source
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array

class JsResources : Resources {

    override val root: String
        get() = ""

    override suspend fun open(path: String): Source {
        val response = window.fetch("./$path").await()
        check(response.ok) { "unknown path: $path" }
        val byteArray = response.arrayBuffer().await().toByteArray()
        return Buffer().apply { write(byteArray) }
    }
}

private fun ArrayBuffer.toByteArray(): ByteArray {
    return Int8Array(this, 0, byteLength).unsafeCast<ByteArray>()
}
