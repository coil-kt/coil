# Requests

Requests are [value objects](https://publicobject.com/2019/06/10/value-objects-service-objects-and-glue/) that provide all the necessary information for an [ImageLoader](image_loaders.md) to execute an image request.

Requests can be created using a builder:

```kotlin
val request = LoadRequest.Builder(context, imageLoader.defaults)
    .data("https://www.example.com/image.jpg")
    .crossfade(true)
    .build()
```

Once you've created a request, pass it to an `ImageLoader` to execute it:

```kotlin
imageLoader.load(request)
```

Optionally, you can use the [type-safe `load` and `get` extension functions](../getting_started/#extension-functions) to create and execute requests.

See the [API documentation](../api/coil-base/coil.request/-request/) for more information.
