# Java Compatibility

Coil's API is designed to be Kotlin-first. It leverages Kotlin language features such as inlined lambdas, receiver params, default arguments, and extension functions, which are not available in Java.

Importantly, suspend functions cannot be implemented in Java. This means custom [Transformations](transformations.md), [Size Resolvers](../api/coil-base/coil.size/-size-resolver), [Fetchers](../image_pipeline/#fetchers), and [Decoders](../image_pipeline/#decoders) **must** be implemented in Kotlin.

With these limitations in mind, Coil is still mostly Java compatible. The syntax to launch a `LoadRequest` is the same in Java and Kotlin:

```java
imageLoader.load(context)
    .data("https://www.example.com/image.jpg")
    .crossfade(true)
    .target(imageView)
    .launch();
```

If you're using the `io.coil-kt:coil` artifact, you can use `Coil.load(context)`.

`suspend` functions cannot be easily called from Java. Thus, to get an image synchronously you'll have to create a wrapper function for `get`:

```kotlin
object ImageLoaderCompat {
    @JvmStatic
    @WorkerThread
    fun getBlocking(
        imageLoader: ImageLoader,
        request: GetRequest
    ): Drawable = runBlocking { imageLoader.launch(request) }
}
```

Then call the `ImageLoaderCompat` function from Java:

```java
GetRequest request = imageLoader.get()
    .data("https://www.example.com/image.jpg")
    .size(1080, 1920)
    .build();
Drawable drawable = ImageLoaderCompat.getBlocking(imageLoader, request);
```

!!! Note
    `ImageLoaderCompat.getBlocking` will block the current thread instead of suspending. Do not call this from the main thread.
