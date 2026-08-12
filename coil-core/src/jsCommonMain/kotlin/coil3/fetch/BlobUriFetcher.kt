@file:OptIn(ExperimentalWasmJsInterop::class)

package coil3.fetch

import coil3.ImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.request.Options
import kotlin.js.ExperimentalWasmJsInterop
import okio.Buffer
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.fetch.Response

/**
 * Fetches images from blob URLs created by `URL.createObjectURL` on the web
 * (e.g. "blob:http://localhost:8081/a3953f30-e6ef-47b6-8750-731ce8afe0f8").
 */
internal class BlobUriFetcher(
    private val uri: Uri,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val response = fetchResponse(uri.toString())
        val arrayBuffer = response.readArrayBuffer()
        val int8Array = Int8Array(arrayBuffer)

        return SourceFetchResult(
            source = ImageSource(
                source = Buffer().apply {
                    write(ByteArray(int8Array.length) { int8Array[it] })
                },
                fileSystem = options.fileSystem,
            ),
            mimeType = response.headers.get("content-type"),
            dataSource = DataSource.MEMORY,
        )
    }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(
            data: Uri,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher? {
            if (!data.toString().startsWith("blob:")) return null
            return BlobUriFetcher(data, options)
        }
    }
}

internal expect suspend fun fetchResponse(url: String): Response

internal expect suspend fun Response.readArrayBuffer(): ArrayBuffer
