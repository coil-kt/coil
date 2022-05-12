# Image Loaders

`ImageLoader`s are [service objects](https://publicobject.com/2019/06/10/value-objects-service-objects-and-glue/) that execute [`ImageRequest`](image_requests.md)s. They handle caching, data fetching, image decoding, request management, bitmap pooling, memory management, and more. New instances can be created and configured using a builder:

```kotlin
val imageLoader = ImageLoader.Builder(context)
    .crossfade(true)
    .build()
```

Coil performs best when you create a single `ImageLoader` and share it throughout your app. This is because each `ImageLoader` has its own memory cache, disk cache, and `OkHttpClient`.

## Caching

Each `ImageLoader` keeps a memory cache of recently decoded `Bitmap`s as well as a disk cache for any images loaded from the Internet. Both can be configured when creating an `ImageLoader`:

```kotlin
val imageLoader = ImageLoader.Builder(context)
    .memoryCache {
        MemoryCache.Builder(context)
            .maxSizePercent(0.25)
            .build()
    }
    .diskCache {
        DiskCache.Builder(context)
            .directory(context.cacheDir.resolve("image_cache"))
            .maxSizePercent(0.02)
            .build()
    }
    .build()
```

You can access items in the memory and disk caches using their keys, which are returned in an `ImageResult` after an image is loaded. The `ImageResult` is returned by `ImageLoader.execute` or in `ImageRequest.Listener.onSuccess` and `ImageRequest.Listener.onError`.

!!! Note
    Coil 1.x relied on OkHttp's disk cache. Coil 2.x has its own disk cache and **should not** use OkHttp's `Cache`.

## Singleton vs. Dependency Injection

The default Coil artifact (`io.coil-kt:coil`) includes the singleton `ImageLoader`, which can be accessed using an extension function: `context.imageLoader`.

Coil performs best when you have a single `ImageLoader` that's shared throughout your app. This is because each `ImageLoader` has its own set of resources.

The singleton `ImageLoader` can be configured by implementing `ImageLoaderFactory` on your `Application` class.

Optionally, you can create your own `ImageLoader` instance(s) and inject them using a dependency injector like [Dagger](https://github.com/google/dagger). If you do that, depend on `io.coil-kt:coil-base` as that artifact doesn't create the singleton `ImageLoader`.

## Testing

`ImageLoader` is an interface, which you can replace with a fake implementation.

For instance, you could create a fake `ImageLoader` implementation which always returns the same `Drawable` synchronously:

```kotlin
class FakeImageLoader(private val context: Context) : ImageLoader {

    override val defaults = DefaultRequestOptions()
    override val components = ComponentRegistry()
    override val memoryCache: MemoryCache? get() = null
    override val diskCache: DiskCache? get() = null

    override fun enqueue(request: ImageRequest): Disposable {
        // Always call onStart before onSuccess.
        request.target?.onStart(request.placeholder)
        val result = ColorDrawable(Color.BLACK)
        request.target?.onSuccess(result)
        return object : Disposable {
            override val job = CompletableDeferred(newResult(request, result))
            override val isDisposed get() = true
            override fun dispose() {}
        }
    }

    override suspend fun execute(request: ImageRequest): ImageResult {
        return newResult(request, ColorDrawable(Color.BLACK))
    }

    private fun newResult(request: ImageRequest, drawable: Drawable): SuccessResult {
        return SuccessResult(
            drawable = drawable,
            request = request,
            dataSource = DataSource.MEMORY_CACHE
        )
    }

    override fun newBuilder() = throw UnsupportedOperationException()

    override fun shutdown() {}
}
```

This is perfect for screenshot and instrumentation tests where you want consistent rendering behavior.
