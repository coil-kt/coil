# Image Loaders

Image Loaders are [service objects](https://publicobject.com/2019/06/10/value-objects-service-objects-and-glue/) that handle image requests with `load` and `get`. They handle caching, image decoding, request management, bitmap pooling, memory management, and more.

New instances can be created like so:

```kotlin
val imageLoader = ImageLoader(context)
```

Similar to [Requests](requests.md), `Image Loader`s can be configured with an optional trailing lambda param:

```kotlin
val imageLoader = ImageLoader(context) {
    availableMemoryPercentage(0.5)
    bitmapPoolPercentage(0.5)
    crossfade(true)
}
```

Internally, this constructs a `RealImageLoader` using [ImageLoaderBuilder](../api/coil-base/coil/-image-loader-builder).

## Singleton vs. Dependency Injection

Ideally, you should construct and inject your `ImageLoader` instance(s) using dependency injection. This will scale well as your app grows and it is the best way to manage multiple `ImageLoader` instances.

However, for simple use cases the Coil artifact provides a default `ImageLoader` instance that can be accessed with `Coil.loader()`. Both `ImageView.load` and `Coil.load` use the default `ImageLoader` instance as a default parameter:

```kotlin
inline fun ImageView.load(
    url: String?,
    imageLoader: ImageLoader = Coil.loader(),
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable {
    return imageLoader.load(context, url) {
        target(this@load)
        builder()
    }
}
```

The `ImageView` extension function can be called with a specific `ImageLoader` like so:

```kotlin
imageView.load("https://www.example.com/image.jpg", imageLoader) {
    crossfade(true)
    placeholder(R.drawable.image)
    transformations(CircleCropTransformation())
}
```

The default `ImageLoader` is instantiated lazily and can be replaced with `Coil.setDefaultImageLoader`.

!!! Note
    Use the `io.coil-kt:coil-base` artifact if you are using dependency injection.

## Testing

`ImageLoader` is an interface, which you can replace with a fake implementation.

For instance, you could inject a fake `ImageLoader` implementation which always returns the same `Drawable` synchronously:

```kotlin
val fakeImageLoader = object : ImageLoader {
    
    private val drawable = ColorDrawable(Color.BLACK)
    
    private val disposable = object : RequestDisposable {
        override fun isDisposed() = true
        override fun dispose() {}
    }
    
    override val defaults = DefaultRequestOptions()

    override fun load(request: LoadRequest): RequestDisposable {
        // Always call onStart before onSuccess.
        request.target?.onStart(drawable)
        request.target?.onSuccess(drawable)
        return disposable
    }

    override suspend fun get(request: GetRequest) = drawable

    override fun clearMemory() {}

    override fun shutdown() {}
}
```

This is perfect for screenshot and instrumentation tests where you want consistent rendering behavior.
