# Requests

Requests are [value objects](https://publicobject.com/2019/06/10/value-objects-service-objects-and-glue/) that provide all the necessary information for an [ImageLoader](image_loaders.md) to execute an image request.

Requests can be created using a builder:

```kotlin
val request = LoadRequest.Builder(context, imageLoader)
    .data("https://www.example.com/image.jpg")
    .crossfade(true)
    .target(imageView)
    .build()
```

Once you've created a request call `launch` to execute it:

```kotlin
request.launch()
```

Internally, this calls `imageLoader.load(request)` on the `imageLoader` that was provided in the `LoadRequest.Builder` constructor.

See the [API documentation](../api/coil-base/coil.request/-request/) for more information.
