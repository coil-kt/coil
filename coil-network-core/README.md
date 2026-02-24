# Network Images

By default, Coil 3.x does not include support for loading images from the network. This is to avoid forcing a large networking dependency on users who want to use their own networking solution or do not need network URL support (e.g. only loading images from disk).

To add support for fetching images from the network import **only one of the following**:

```kotlin
implementation("io.coil-kt.coil3:coil-network-okhttp:3.4.0") // Only available on Android/JVM.
implementation("io.coil-kt.coil3:coil-network-ktor2:3.4.0")
implementation("io.coil-kt.coil3:coil-network-ktor3:3.4.0")
```

If you use OkHttp, that's it. Once imported, network URLs like `https://example.com/image.jpg` will automatically be supported. If you use Ktor, you need to add supported engines for each platform (see below).

## Ktor network engines

If you depend on `coil-network-ktor2` or `coil-network-ktor3` you need to import a [Ktor engine](https://ktor.io/docs/client-engines.html) for each platform (except Javascript). Here's a quickstart set of engines:

```kotlin
androidMain {
    dependencies {
        implementation("io.ktor:ktor-client-android:<ktor-version>")
    }
}
appleMain {
    dependencies {
        implementation("io.ktor:ktor-client-darwin:<ktor-version>")
    }
}
jvmMain {
    dependencies {
        implementation("io.ktor:ktor-client-java:<ktor-version>")
    }
}
```

If you want to use a custom networking library, you can import `io.coil-kt.coil3:coil-network-core`, implement `NetworkClient`, and register `NetworkFetcher` with your custom `NetworkClient` in your `ImageLoader`.

## Using a custom OkHttpClient

If you use `io.coil-kt.coil3:coil-network-okhttp` You can specify a custom `OkHttpClient` when creating your `ImageLoader`:

```kotlin
val imageLoader = ImageLoader.Builder(context)
    .components {
        add(
            OkHttpNetworkFetcherFactory(
                callFactory = {
                    OkHttpClient()
                }
            )
        )
    }
    .build()
```

!!! Note
    If you already have a built `OkHttpClient`, use [`newBuilder()`](https://square.github.io/okhttp/5.x/okhttp/okhttp3/-ok-http-client/#customize-your-client-with-newbuilder) to build a new client that shares resources with the original.

## Cache-Control support

By default, Coil 3.x does not respect `Cache-Control` headers and always saves a response to its disk cache.

`io.coil-kt.coil3:coil-network-cache-control` includes a `CacheStrategy` implementation that ensures that `NetworkFetcher` respects a network response's [`Cache-Control` header](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cache-Control).

Pass `CacheControlCacheStrategy` to your `NetworkFetcher` then register the custom `NetworkFetcher` in your `ImageLoader`:

```kotlin
OkHttpNetworkFetcherFactory(
    cacheStrategy = { CacheControlCacheStrategy() },
)
```

!!! Note
    You need to enable `coreLibraryDesugaring` to support Android API level 25 or below. Follow the docs [here](https://developer.android.com/studio/write/java8-support#library-desugaring) to enable it.

#### Headers

Headers can be added to your image requests in one of two ways. You can set headers for a single request:

```kotlin
val headers = NetworkHeaders.Builder()
    .set("Cache-Control", "no-cache")
    .build()
val request = ImageRequest.Builder(context)
    .data("https://example.com/image.jpg")
    .httpHeaders(headers)
    .target(imageView)
    .build()
imageLoader.execute(request)
```

Or you can create an OkHttp [`Interceptor`](https://square.github.io/okhttp/interceptors/) that sets headers for every request executed by your `ImageLoader`:

```kotlin
class RequestHeaderInterceptor(
    private val name: String,
    private val value: String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val headers = Headers.Builder()
            .set("Cache-Control", "no-cache")
            .build()
        val request = chain.request().newBuilder()
            .headers(headers)
            .build()
        return chain.proceed(request)
    }
}

val imageLoader = ImageLoader.Builder(context)
    .components {
        add(
            OkHttpNetworkFetcher(
                callFactory = {
                    OkHttpClient.Builder()
                        // This header will be added to every image request.
                        .addNetworkInterceptor(RequestHeaderInterceptor("Cache-Control", "no-cache"))
                        .build()
                },
            )
        )
    }
    .build()
```
