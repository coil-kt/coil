package coil3.fetch

import kotlin.js.Promise
import org.w3c.fetch.Response

internal actual fun fetchResponse(url: String): Promise<Response> = js("fetch(url)")
