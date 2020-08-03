# Extending the Image Pipeline

Android supports many [image formats](https://developer.android.com/guide/topics/media/media-formats) out of the box, however there are also plenty of formats it does not (e.g. GIF, SVG, TIFF, etc.)

Fortunately, [ImageLoader](image_loaders.md)s support pluggable components to add new cache layers, new data types, new fetching behavior, new image encodings, or otherwise overwrite the base image loading behavior. Coil's image pipeline consists of four main parts: [Interceptors](../api/coil-base/coil.interceptor/-interceptor/), [Mappers](../api/coil-base/coil.map/-mapper), [Fetchers](../api/coil-base/coil.fetch/-fetcher), and [Decoders](../api/coil-base/coil.decode/-decoder).

Custom components must be added to the `ImageLoader` when constructing it through its [ComponentRegistry](../api/coil-base/coil/-component-registry):

```kotlin
val imageLoader = ImageLoader.Builder(context)
    .componentRegistry {
        add(CustomCacheInterceptor())
        add(ItemMapper())
        add(CronetFetcher())
        add(GifDecoder())
    }
    .build()
```

## Interceptors

Interceptors allow you to observe, transform, short circuit, or retry requests to an [ImageLoader]'s image engine. For example, you can add a custom cache layer like so:

```kotlin
class CustomCacheInterceptor(
    private val context: Context,
    private val cache: LruCache<String, Drawable>
) : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val value = cache.get(chain.request.data.toString())
        if (value != null) {
            return SuccessResult(
                drawable = value.bitmap.toDrawable(context),
                request = chain.request,
                metadata = TODO()
            )
        }
        return chain.proceed(chain.request)
    }
}
```

Interceptors are an advanced feature that let you wrap an `ImageLoader`'s image pipeline with custom logic. Their design is heavily based on [OkHttp's `Interceptor` interface](https://square.github.io/okhttp/interceptors/#interceptors).

See [Interceptor](../api/coil-base/coil.intercept/-interceptor) for more information.

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
    override fun map(data: Item) = data.imageUrl
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

See [Mapper](../api/coil-base/coil.map/-mapper) for more information.

## Fetchers

Fetchers translate data into either a `BufferedSource` or a `Drawable`.

See [Fetcher](../api/coil-base/coil.fetch/-fetcher) for more information.

## Decoders

Decoders read a `BufferedSource` as input and return a `Drawable`. Use this interface to add support for custom file formats (e.g. GIF, SVG, TIFF, etc.).

See [Decoder](../api/coil-base/coil.decode/-decoder) for more information.

!!! Note
    Decoders are responsible for closing the `BufferedSource` when finished. This allows custom decoders to return a `Drawable` while still reading the source. This can be useful to support file types such as [progressive JPEG](https://www.liquidweb.com/kb/what-is-a-progressive-jpeg/) where there is incremental information to show.
