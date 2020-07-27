# Image Requests

`ImageRequests` are [value objects](https://publicobject.com/2019/06/10/value-objects-service-objects-and-glue/) that provide all the necessary information for an [ImageLoader](image_loaders.md) to load an image.

`ImageRequests` can be created using a builder:

```kotlin
val request = ImageRequest.Builder(context)
    .data("https://www.example.com/image.jpg")
    .crossfade(true)
    .target(imageView)
    .build()
```

Once you've created a request pass it to an `ImageLoader` to enqueue/execute it:

```kotlin
imageLoader.enqueue(request)
```

See the [API documentation](../api/coil-base/coil.request/-request/) for more information.
