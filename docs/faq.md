# FAQ

Have a question that isn't part of the FAQ? Check [StackOverflow](https://stackoverflow.com/questions/tagged/coil) with the tag #coil. We monitor that tag for any new questions.

## Can Coil be used with Java projects or mixed Kotlin/Java projects?

Yes! [Read more here](java_compatibility.md).

## How do I preload an image?

[Read here](../getting_started/#preloading).

## How do I set up disk caching?

Coil relies on [OkHttp](https://square.github.io/okhttp/)'s disk cache. **By default, each `ImageLoader` is already set up for disk caching** and will set a max cache size of between 10-250MB depending on the remaining space on the user's device.

Here's how to set a custom cache size (and a custom `OkHttpClient`) for your `ImageLoader`:

```kotlin
val cacheDirectory = context.cacheDir
val cacheSize = Int.MAX_VALUE
val cache = Cache(cacheDirectory, cacheSize)
val client = OkHttpClient.Builder().cache(cache).build()
val imageloader = ImageLoader(context) {
    okHttpClient(client)
}
```

If you're using the Coil singleton, you can then replace its `ImageLoader` instance like so:

```kotlin
Coil.setDefaultImageLoader(imageLoader)
```
