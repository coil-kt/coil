# Image Loaders

Image Loaders are [service objects](https://publicobject.com/2019/06/10/value-objects-service-objects-and-glue/) that execute `Request`s. They handle caching, image decoding, request management, bitmap pooling, memory management, and more.

New instances can be created like so:

```kotlin
val imageLoader = ImageLoader(context)
```

Similar to [Requests](requests.md), `ImageLoader`s can be configured by using a builder:

```kotlin
val imageLoader = ImageLoader.Builder(context)
    .availableMemoryPercentage(0.25)
    .bitmapPoolPercentage(0.5)
    .crossfade(true)
    .build()
```

Internally, this constructs a `RealImageLoader` using [ImageLoaderBuilder](../api/coil-base/coil/-image-loader-builder).

## Caching

Each `ImageLoader` keeps a memory cache of recently loaded `BitmapDrawable`s as well as a reusable pool of `Bitmap`s.

`ImageLoader`s rely on `OkHttpClient` to handle disk caching. **By default, every `ImageLoader` is already set up for disk caching** and will set a max cache size of between 10-250MB depending on the remaining space on the user's device.

However, if you set a custom `OkHttpClient`, you'll need to add the disk cache yourself. To get a `Cache` instance that's optimized for Coil, you can use [`CoilUtils.createDefaultCache`](../api/coil-base/coil.util/-coil-utils/create-default-cache/). Optionally, you can create your own `Cache` instance with a different size + location. Here's an example:

```kotlin
val imageLoader = ImageLoader.Builder(context)
    .okHttpClient {
        OkHttpClient.Builder()
            .cache(CoilUtils.createDefaultCache(context))
            .build()
    }
    .build()
```

## Singleton vs. Dependency Injection

Coil performs best when you create a single `ImageLoader` and share it throughout your app. This is because each `ImageLoader` has its own memory cache and bitmap pool.

If you use a dependency injector like [Dagger](https://github.com/google/dagger), then you should create a single `ImageLoader` instance and inject it throughout your app.

However, if you'd prefer a singleton the `io.coil-kt:coil` artifact provides a default `ImageLoader` instance that can be accessed with `Coil.imageLoader(context)`. [Read here](../getting_started/#singleton) for how to initialize the singleton `ImageLoader` instance.

!!! Note
    Use the `io.coil-kt:coil-base` artifact if you are using dependency injection.

## Shutdown

When you're done with an `ImageLoader` you should call `ImageLoader.shutdown()`. This releases all resources and observers used by the image loader and stops any new requests from being executed. Failure to call `ImageLoader.shutdown()` can leak the observers used internally.

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

    override fun execute(request: LoadRequest): RequestDisposable {
        // Always call onStart before onSuccess.
        request.target?.onStart(drawable)
        request.target?.onSuccess(drawable)
        return disposable
    }

    override suspend fun execute(request: GetRequest) = drawable

    override fun clearMemory() {}

    override fun shutdown() {}
}
```

This is perfect for screenshot and instrumentation tests where you want consistent rendering behavior.
