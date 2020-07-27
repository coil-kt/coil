# Image Loaders

`ImageLoader`s are [service objects](https://publicobject.com/2019/06/10/value-objects-service-objects-and-glue/) that execute [`ImageRequest`](image_requests.md)s. They handle caching, data fetching, image decoding, request management, bitmap pooling, memory management, and more. New instances can be created and configured using a builder:

```kotlin
val imageLoader = ImageLoader.Builder(context)
    .availableMemoryPercentage(0.25)
    .crossfade(true)
    .build()
```

Internally, this constructs a `RealImageLoader` using [ImageLoaderBuilder](../api/coil-base/coil/-image-loader-builder).

Coil performs best when you create a single `ImageLoader` and share it throughout your app. This is because each `ImageLoader` has its own memory cache, bitmap pool, and network observer.

It's recommended, though not required, to call [`shutdown`](../api/coil-base/coil/-image-loader/shutdown/) when you've finished using an image loader. This preemptively frees its memory and cleans up any observers. If you only create and use one `ImageLoader`, you do not need to shut it down as it will be freed when your app is killed.

## Caching

Each `ImageLoader` keeps a memory cache of recently decoded `Bitmap`s as well as a reusable pool of `Bitmap`s to decode into.

`ImageLoader`s rely on an `OkHttpClient` to handle disk caching. **By default, every `ImageLoader` is already set up for disk caching** and will set a max cache size of between 10-250MB depending on the remaining space on the user's device.

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

## Testing

`ImageLoader` is an interface, which you can replace with a fake implementation.

For instance, you could inject a fake `ImageLoader` implementation which always returns the same `Drawable` synchronously:

```kotlin
val fakeImageLoader = object : ImageLoader {
    
    private val drawable = ColorDrawable(Color.BLACK)

    private val disposable = object : RequestDisposable {
        override val isDisposed = true
        override fun dispose() {}
        override suspend fun await() {}
    }

    override val defaults = DefaultRequestOptions()

    override fun execute(request: LoadRequest): RequestDisposable {
        // Always call onStart before onSuccess.
        request.target?.onStart(drawable)
        request.target?.onSuccess(drawable)
        return disposable
    }

    override suspend fun execute(request: GetRequest): RequestResult {
        return SuccessResult(drawable, DataSource.MEMORY_CACHE)
    }

    override fun invalidate(key: String) {}

    override fun clearMemory() {}

    override fun shutdown() {}
}
```

This is perfect for screenshot and instrumentation tests where you want consistent rendering behavior.
