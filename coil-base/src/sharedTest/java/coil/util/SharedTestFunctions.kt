@file:Suppress("EXPERIMENTAL_API_USAGE", "unused")

package coil.util

import android.content.Context
import android.graphics.Bitmap
import coil.DefaultRequestOptions
import coil.decode.Options
import coil.request.CachePolicy
import coil.request.GetRequest
import coil.request.GetRequestBuilder
import coil.request.LoadRequest
import coil.request.LoadRequestBuilder
import coil.request.Parameters
import coil.size.Scale
import okhttp3.Headers
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.buffer
import okio.source

fun createMockWebServer(context: Context, vararg images: String): MockWebServer {
    return MockWebServer().apply {
        images.forEach { image ->
            val buffer = Buffer()
            context.assets.open(image).source().buffer().readAll(buffer)
            enqueue(MockResponse().setBody(buffer))
        }
        start()
    }
}

fun createOptions(): Options {
    return Options(
        config = Bitmap.Config.ARGB_8888,
        colorSpace = null,
        scale = Scale.FILL,
        allowRgb565 = false,
        headers = Headers.Builder().build(),
        parameters = Parameters.Builder().build(),
        networkCachePolicy = CachePolicy.ENABLED,
        diskCachePolicy = CachePolicy.ENABLED
    )
}

inline fun createGetRequest(
    builder: GetRequestBuilder.() -> Unit = {}
): GetRequest = GetRequestBuilder(DefaultRequestOptions()).data(Any()).apply(builder).build()

inline fun createLoadRequest(
    context: Context,
    builder: LoadRequestBuilder.() -> Unit = {}
): LoadRequest = LoadRequestBuilder(context, DefaultRequestOptions()).data(Any()).apply(builder).build()
