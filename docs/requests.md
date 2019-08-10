# Requests

Requests are [value objects](https://publicobject.com/2019/06/10/value-objects-service-objects-and-glue/) that provide all the necessary information for an [ImageLoader](image_loaders.md) to execute an image request.

Prefer using the [type-safe `load` and `get` extension functions](../getting_started/#extension-functions) to execute requests, however they can also be created manually:

```kotlin
val url = "https://www.example.com/image.jpg"
val request = LoadRequest(context, url, imageLoader.defaults) {
    crossfade(true)
}
```

See the [API doc](../api/coil-base/coil.request/-request/) for more information.
