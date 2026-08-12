@file:OptIn(ExperimentalWasmJsInterop::class)

package coil3.fetch

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.Promise
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.w3c.fetch.Response

internal actual suspend fun fetchResponse(url: String): Response =
    fetch(url).await<Response>()

internal actual suspend fun Response.readArrayBuffer(): ArrayBuffer = arrayBuffer().await<ArrayBuffer>()

private fun fetch(url: String): Promise<Response> = js("fetch(url)")
