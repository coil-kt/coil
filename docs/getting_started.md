# Getting Started

## Artifacts

Coil has 9 artifacts published to `mavenCentral()`:

* `io.coil-kt:coil`: The default artifact which depends on `io.coil-kt:coil-base`, creates a singleton `ImageLoader`, and includes the `ImageView` extension functions.
* `io.coil-kt:coil-base`: A subset of `io.coil-kt:coil` which **does not** include the singleton `ImageLoader` and the `ImageView` extension functions.
* `io.coil-kt:coil-compose`: Includes support for [Jetpack Compose](https://developer.android.com/jetpack/compose).
* `io.coil-kt:coil-compose-base`: A subset of `io.coil-kt:coil-compose` which does not include functions that depend on the singleton `ImageLoader`.
* `io.coil-kt:coil-gif`: Includes two [decoders](/coil/api/coil-core/coil3.decode/-decoder) to support decoding GIFs. See [GIFs](gifs.md) for more details.
* `io.coil-kt:coil-svg`: Includes a [decoder](/coil/api/coil-core/coil3.decode/-decoder) to support decoding SVGs. See [SVGs](svgs.md) for more details.
* `io.coil-kt:coil-video`: Includes a [decoder](/coil/api/coil-core/coil3.decode/-decoder) to support decoding frames from [any of Android's supported video formats](https://developer.android.com/guide/topics/media/media-formats#video-codecs). See [videos](videos.md) for more details.
* `io.coil-kt:coil-test`: Includes classes to support testing with `ImageLoader`s. See [Testing](testing.md) for more details.
* `io.coil-kt:coil-bom`: Includes a [bill of materials](https://docs.gradle.org/7.2/userguide/platforms.html#sub:bom_import). Importing `coil-bom` allows you to depend on other Coil artifacts without specifying a version.

## Image Loaders

[`ImageLoader`](image_loaders.md)s are service classes that execute [`ImageRequest`](image_requests.md)s. `ImageLoader`s handle caching, data fetching, image decoding, request management, bitmap pooling, memory management, and more.

The default Coil artifact (`io.coil-kt:coil`) includes the singleton `ImageLoader`, which can be accessed using an extension function: `context.imageLoader`.

The singleton `ImageLoader` can be configured by implementing `ImageLoaderFactory` on your `Application` class:

```kotlin
class MyApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(true)
            .build()
    }
}
```

Implementing `ImageLoaderFactory` is optional. If you don't, Coil will lazily create an `ImageLoader` with the default values.

Check out [the full documentation](image_loaders.md) for more info.

## Image Requests

[`ImageRequest`](image_requests.md)s are value classes that are executed by [`ImageLoader`](image_loaders.md)s. They describe where an image should be loaded from, how it should be loaded, and any extra parameters. An `ImageLoader` has two methods that can execute a request:

- `enqueue`: Enqueues the `ImageRequest` to be executed asynchronously on a background thread.
- `execute`: Executes the `ImageRequest` in the current coroutine and returns an [`ImageResult`](/coil/api/coil-core/coil3.request/-image-result).

All requests should set `data` (i.e. url, uri, file, drawable resource, etc.). This is what the `ImageLoader` will use to decide where to fetch the image data from. If you do not set `data`, it will default to [`NullRequestData`](/coil/api/coil-core/coil3.request/-null-request-data).

Additionally, you likely want to set a `target` when enqueuing a request. It's optional, but the `target` is what will receive the loaded placeholder/success/error drawables. Executed requests return an `ImageResult` which has the success/error drawable.

Here's an example:

```kotlin
// enqueue
val request = ImageRequest.Builder(context)
    .data("https://example.com/image.jpg")
    .target(imageView)
    .build()
val disposable = imageLoader.enqueue(request)

// execute
val request = ImageRequest.Builder(context)
    .data("https://example.com/image.jpg")
    .build()
val result = imageLoader.execute(request)
```

## ImageView Extension Functions

The `io.coil-kt:coil` artifact provides a set of `ImageView` extension functions. Here's an example for loading a URL into an `ImageView`:

```kotlin
imageView.load("https://example.com/image.jpg")
```

The above call is equivalent to:

```kotlin
val imageLoader = imageView.context.imageLoader
val request = ImageRequest.Builder(imageView.context)
    .data("https://example.com/image.jpg")
    .target(imageView)
    .build()
imageLoader.enqueue(request)
```

`ImageView.load` calls can be configured with an optional trailing lambda parameter:

```kotlin
imageView.load("https://example.com/image.jpg") {
    crossfade(true)
    placeholder(R.drawable.image)
    transformations(CircleCropTransformation())
}
```

See the docs [here](/coil/api/coil/coil3/load) for more information.

## Supported Data Types

The base data types that are supported by all `ImageLoader` instances are:

* String
* HttpUrl
* Uri (`android.resource`, `content`, `file`, `http`, and `https` schemes)
* File
* @DrawableRes Int
* Drawable
* Bitmap
* ByteArray
* ByteBuffer

## Supported Image Formats

All `ImageLoader`s support the following non-animated file types:

* BMP
* JPEG
* PNG
* WebP
* HEIF (Android 8.0+)
* AVIF (Android 12.0+)

Additionally, Coil has extension libraries for the following file types:

* `coil-gif`: GIF, animated WebP (Android 9.0+), animated HEIF (Android 11.0+)
* `coil-svg`: SVG
* `coil-video`: Static video frames from any [video codec supported by Android](https://developer.android.com/guide/topics/media/media-formats#video-codecs)

## Preloading

To preload an image into memory, enqueue or execute an `ImageRequest` without a `Target`:

```kotlin
val request = ImageRequest.Builder(context)
    .data("https://example.com/image.jpg")
    // Optional, but setting a ViewSizeResolver will conserve memory by limiting the size the image should be preloaded into memory at.
    .size(ViewSizeResolver(imageView))
    .build()
imageLoader.enqueue(request)
```

To preload a network image only into the disk cache:

```kotlin
val request = ImageRequest.Builder(context)
    .data("https://example.com/image.jpg")
    // Disable reading from/writing to the memory cache.
    .memoryCachePolicy(CachePolicy.DISABLED)
    // Set a custom `Decoder.Factory` that skips the decoding step.
    .decoderFactory { _, _, _ ->
        Decoder { DecodeResult(ColorDrawable(Color.BLACK), false) }
    }
    .build()
imageLoader.enqueue(request)
```

## Cancelling Requests

`ImageRequest`s will be automatically cancelled in the following cases:

- `request.lifecycle` reaches the `DESTROYED` state.
- `request.target` is a `ViewTarget` and its `View` is detached.

Additionally, `ImageLoader.enqueue` returns a [Disposable](/coil/api/coil-core/coil3.request/-disposable/), which can be used to dispose the request (which cancels it and frees its associated resources):

```kotlin
val disposable = imageView.load("https://example.com/image.jpg")

// Cancel the request.
disposable.dispose()
```
