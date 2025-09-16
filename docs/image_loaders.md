# Image Loaders

`ImageLoader`s are [service objects](https://publicobject.com/2019/06/10/value-objects-service-objects-and-glue/) that execute [`ImageRequest`](image_requests.md)s. They handle caching, data fetching, image decoding, request management, memory management, and more.

Coil performs best when you create a single `ImageLoader` and share it throughout your app. This is because each `ImageLoader` has its own memory cache, disk cache, and `OkHttpClient`.

## Singleton

The default `io.coil-kt.coil3:coil` artifact comes with a singleton `ImageLoader`. Coil creates this `ImageLoader` lazily. It can be configured a number of ways:

```kotlin
// The setSafe method ensures that it won't overwrite an
// existing image loader that's been created.
SingletonImageLoader.setSafe {
    ImageLoader.Builder(context)
        .crossfade(true)
        .build()
}

// An alias of SingletonImageLoader.setSafe that's useful for
// Compose Multiplatform apps.
setSingletonImageLoaderFactory { context ->
    ImageLoader.Builder(context)
        .crossfade(true)
        .build()
}

// Should only be used in tests. If you call this method
// multiple times it will create multiple image loaders.
SingletonImageLoader.setUnsafe {
    ImageLoader.Builder(context)
        .crossfade(true)
        .build()
}

// On Android you can implement SingletonImageLoader.Factory on your
// Application class to have it create the singleton image loader.
class CustomApplication : SingletonImageLoader.Factory {
    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }
}
```

**In all cases ensure the above methods should be invoked as soon as possible when your app starts (i.e. inside `Application.onCreate` or inside `MainActivity.onCreate` if your app is only a single `Activity`.)**

## Dependency injection

If you have a larger app or want to manage your own `ImageLoaders` you can depend on `io.coil-kt.coil3:coil-core` instead of `io.coil-kt.coil3:coil`.

This route makes scoping the lifecycle of a fake `ImageLoader` much easier and will overall make testing easier.

## Caching

Each `ImageLoader` keeps a memory cache of recently decoded `Bitmap`s as well as a disk cache for any images loaded from the Internet. Both can be configured when creating an `ImageLoader`:

```kotlin
val imageLoader = ImageLoader.Builder(context)
    .memoryCache {
        MemoryCache.Builder()
            .maxSizePercent(context, 0.25)
            .build()
    }
    .diskCache {
        DiskCache.Builder()
            .directory(context.cacheDir.resolve("image_cache"))
            .maxSizePercent(0.02)
            .build()
    }
    .build()
```

You can access items in the memory and disk caches using their keys, which are returned in an `ImageResult` after an image is loaded. The `ImageResult` is returned by `ImageLoader.execute` or in `ImageRequest.Listener.onSuccess` and `ImageRequest.Listener.onError`.
