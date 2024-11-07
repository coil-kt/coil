# Extending the Image Pipeline

Android supports many [image formats](https://developer.android.com/guide/topics/media/media-formats#image-formats) out of the box, however there are also plenty of formats it does not (e.g. GIF, SVG, MP4, etc.)

Fortunately, [ImageLoader](image_loaders.md)s support pluggable components to add new cache layers, new data types, new fetching behavior, new image encodings, or otherwise overwrite the base image loading behavior. Coil's image pipeline consists of five main parts that are executed in the following order: [Interceptors](/coil/api/coil-core/coil3.intercept/-interceptor), [Mappers](/coil/api/coil-core/coil3.map/-mapper), [Keyers](/coil/api/coil-core/coil3.key/-keyer), [Fetchers](/coil/api/coil-core/coil3.fetch/-fetcher), and [Decoders](/coil/api/coil-core/coil3.decode/-decoder).

Custom components must be added to the `ImageLoader` when constructing it through its [ComponentRegistry](/coil/api/coil-core/coil3/-component-registry):

```kotlin
val imageLoader = ImageLoader.Builder(context)
    .components {
        add(CustomCacheInterceptor())
        add(ItemMapper())
        add(HttpUrlKeyer())
        add(CronetFetcher.Factory())
        add(GifDecoder.Factory())
    }
    .build()
```

## Interceptors

Interceptors allow you to observe, transform, short circuit, or retry requests to an `ImageLoader`'s image engine. For example, you can add a custom cache layer like so:

```kotlin
class CustomCacheInterceptor(
    private val context: Context,
    private val cache: LruCache<String, Image>,
) : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val value = cache.get(chain.request.data.toString())
        if (value != null) {
            return SuccessResult(
                image = value.bitmap.toImage(),
                request = chain.request,
                dataSource = DataSource.MEMORY_CACHE,
            )
        }
        return chain.proceed(chain.request)
    }
}
```

Interceptors are an advanced feature that let you wrap an `ImageLoader`'s image pipeline with custom logic. Their design is heavily based on [OkHttp's `Interceptor` interface](https://square.github.io/okhttp/interceptors/#interceptors).

See [Interceptor](/coil/api/coil-core/coil3.intercept/-interceptor) for more information.

## Mappers

Mappers allow you to add support for custom data types. For instance, say we get this model from our server:

```kotlin
data class Item(
    val id: Int,
    val imageUrl: String,
    val price: Int,
    val weight: Double
)
```

We could write a custom mapper to map it to its URL, which will be handled later in the pipeline:

```kotlin
class ItemMapper : Mapper<Item, String> {
    override fun map(data: Item, options: Options) = data.imageUrl
}
```

After registering it when building our `ImageLoader` (see above), we can safely load an `Item`:

```kotlin
val request = ImageRequest.Builder(context)
    .data(item)
    .target(imageView)
    .build()
imageLoader.enqueue(request)
```

See [Mapper](/coil/api/coil-core/coil3.map/-mapper) for more information.

## Keyers

Keyers convert data into a portion of a cache key. This value is used as `MemoryCache.Key.key` when/if this request's output is written to the `MemoryCache`.

See [Keyers](/coil/api/coil-core/coil3.key/-keyer) for more information.

## Fetchers

Fetchers translate data (e.g. URL, URI, File, etc.) into either an `ImageSource` or an `Image`. They typically convert the input data into a format that can then be consumed by a `Decoder`. Use this interface to add support for custom fetching mechanisms (e.g. Cronet, custom URI schemes, etc.)

See [Fetcher](/coil/api/coil-core/coil3.fetch/-fetcher) for more information.

!!! Note
    If you add a `Fetcher` that uses a custom data type, you need to also need to provide a custom `Keyer` to ensure results from requests that use it are memory cacheable. For example, `Fetcher.Factory<MyDataType>` will need to add a `Keyer<MyDataType`.

## Decoders

Decoders read an `ImageSource` and return an `Image`. Use this interface to add support for custom file formats (e.g. GIF, SVG, TIFF, etc.).

See [Decoder](/coil/api/coil-core/coil3.decode/-decoder) for more information.

## Chaining components

A useful property of Coil's image loader components is that they can be chained internally. For example, say you need to perform a network request to get the image URL that will be loaded.

First, let's create a custom data type that only our fetcher will handle:

```kotlin
data class PartialUrl(
    val baseUrl: String,
)
```

Then let's create our custom `Fetcher` that will get the image URL and delegate to the internal network fetcher:

```kotlin
class PartialUrlFetcher(
    private val callFactory: Call.Factory,
    private val partialUrl: PartialUrl,
    private val options: Options,
    private val imageLoader: ImageLoader,
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val request = Request.Builder()
            .url(partialUrl.baseUrl)
            .build()
        val response = callFactory.newCall(request).await()

        // Read the image URL.
        val imageUrl: String = readImageUrl(response.body)

        // This will delegate to the internal network fetcher.
        val data = imageLoader.components.map(imageUrl, options)
        val output = imageLoader.components.newFetcher(data, options, imageLoader)
        val (fetcher) = checkNotNull(output) { "no supported fetcher" }
        return fetcher.fetch()
    }

    class Factory(
        private val callFactory: Call.Factory = OkHttpClient(),
    ) : Fetcher.Factory<PartialUrl> {
        override fun create(data: PartialUrl, options: Options, imageLoader: ImageLoader): Fetcher {
            return PartialUrlFetcher(callFactory, data, options, imageLoader)
        }
    }
}
```

Finally all we have to do is register the `Fetcher` in our `ComponentRegistry` and pass a `PartialUrl` as our `model`/`data`:

```kotlin
AsyncImage(
    model = PartialUrl("https://example.com/image.jpg"),
    contentDescription = null,
)
```

This pattern can similarly be applied to `Mapper`s, `Keyer`s, and `Decoder`s.
