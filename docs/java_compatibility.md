# Java Compatibility

Coil's API was designed to be Kotlin-first. It leverages Kotlin language features such as inlined lambdas, receiver params, default arguments, and extension functions, which are not available in Java.

Also, suspend functions cannot be implemented in Java. This means custom [Transformations](transformations.md), [Size Resolvers](../api/coil-base/coil.size/-size-resolver), [Fetchers](../image_pipeline/#fetchers), and [Decoders](../image_pipeline/#decoders) **must** be implemented in Kotlin.

With these limitations in mind, here is the recommended way to execute `load` requests from Java:

```java
LoadRequest request = ImageLoaders.newLoadBuilder(imageLoader, context)
        .data("https://www.example.com/image.jpg")
        .crossfade(true)
        .target(imageView)
        .build();
imageLoader.load(request);
```

If you're using the default `ImageLoader`, you can get it via `Coil.imageLoader(context)`.

!!! Note
    You should not use the [API extension functions](../getting_started/#extension-functions) in Java. Instead, you should create `Request` objects manually like above.

`suspend` functions cannot be easily called from Java. Thus, to get an image synchronously you'll have to create a wrapper function for `get`:

```kotlin
object ImageLoaderCompat {
    @JvmStatic
    @WorkerThread
    fun getBlocking(
        imageLoader: ImageLoader,
        request: GetRequest
    ): Drawable = runBlocking { imageLoader.get(request) }
}
```

Then call the `ImageLoaderCompat` function from Java:

```java
GetRequest request = ImageLoaders.newGetBuilder(imageLoader)
        .data("https://www.example.com/image.jpg")
        .size(1080, 1920)
        .build();
Drawable drawable = ImageLoaderCompat.getBlocking(imageLoader, request);
```

!!! Note
    `ImageLoaderCompat.getBlocking` will block the current thread instead of suspending. Do not call this from the main thread.
