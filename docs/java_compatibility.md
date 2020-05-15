# Java Compatibility

Coil's API is designed to be Kotlin-first. It leverages Kotlin language features such as inlined lambdas, receiver params, default arguments, and extension functions, which are not available in Java.

Importantly, suspend functions cannot be implemented in Java. This means custom [Transformations](transformations.md), [Size Resolvers](../api/coil-base/coil.size/-size-resolver), [Fetchers](../image_pipeline/#fetchers), and [Decoders](../image_pipeline/#decoders) **must** be implemented in Kotlin.

Despite these limitations, most of Coil's API is Java compatible. For instance, the syntax to launch a `LoadRequest` is almost the same in Java and Kotlin:

```java
LoadRequest request = LoadRequest.builder(context)
    .data("https://www.example.com/image.jpg")
    .crossfade(true)
    .target(imageView)
    .build();
imageLoader.execute(request)
```

!!! Note
    `ImageView.load` extension functions cannot be used from Java. Use the `LoadRequest.Builder` API instead.

`suspend` functions cannot be easily called from Java. Thus, to get an image synchronously you'll have to create a wrapper function to execute `GetRequest`s:

```kotlin
@file:JvmName("ImageLoaders")

@WorkerThread
fun ImageLoader.executeBlocking(request: GetRequest): RequestResult {
    return runBlocking { execute(request) }
}
```

Then call the `ImageLoaders` function from Java:

```java
GetRequest request = GetRequest.builder(context)
    .data("https://www.example.com/image.jpg")
    .size(1080, 1920)
    .build();
Drawable drawable = ImageLoaders.executeBlocking(imageLoader, request).getDrawable();
```

!!! Note
    `ImageLoaders.executeBlocking` will block the current thread instead of suspending. Do not call this from the main thread.
