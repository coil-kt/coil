# Cache-Control

`coil-network-cache-control` includes a `CacheStrategy` implementation that ensures that `NetworkFetcher` respects a network response's [`Cache-Control` header](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cache-Control).

Pass this to your `NetworkFetcher` to use it:

```kotlin
KtorNetworkFetcher(
    cacheStrategy = CacheControlCacheStrategy(),
)
```
