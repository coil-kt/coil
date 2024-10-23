# Network URLs

By default, Coil 3.x does not include support for network URLs. This is to avoid forcing a large networking dependency on users who want to use their own networking solution or do not need network URL support (e.g. only loading images from disk).

To add support for fetching images from the network import **only one of the following**:

```kotlin
implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.0-rc01") // Only available on Android/JVM.
implementation("io.coil-kt.coil3:coil-network-ktor2:3.0.0-rc01")
implementation("io.coil-kt.coil3:coil-network-ktor3:3.0.0-rc01")
```

If you use OkHttp, that's it. Once imported, network URLs like `https://www.example.com/image.jpg` will automatically be supported. If you use Ktor, you need to add supported engines for each platform:

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

## Cache-Control support

By default, Coil 3.x does not respect `Cache-Control` headers and always saves a response to its disk cache.

`io.coil-kt.coil3:coil-network-cache-control` includes a `CacheStrategy` implementation that ensures that `NetworkFetcher` respects a network response's [`Cache-Control` header](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cache-Control).

Pass `CacheControlCacheStrategy` to your `NetworkFetcher` then register the custom `NetworkFetcher` in your `ImageLoader`:

```kotlin
KtorNetworkFetcher(
    cacheStrategy = CacheControlCacheStrategy(),
)
```
