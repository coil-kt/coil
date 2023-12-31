# Image Requests

`ImageRequest`s are [value objects](https://publicobject.com/2019/06/10/value-objects-service-objects-and-glue/) that provide all the necessary information for an [ImageLoader](image_loaders.md) to load an image.

`ImageRequest`s can be created using a builder:

```kotlin
val request = ImageRequest.Builder(context)
    .data("https://example.com/image.jpg")
    .crossfade(true)
    .target(imageView)
    .build()
```

Once you've created a request pass it to an `ImageLoader` to enqueue/execute it:

```kotlin
imageLoader.enqueue(request)
```

See the [API documentation](/coil/api/coil-core/coil3.request/-image-request/) for more information.
