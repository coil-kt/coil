# Getting Started

## Artifacts

Coil has 5 artifacts published to `mavenCentral()`:

* `io.coil-kt:coil`: The default artifact which depends on `io.coil-kt:coil-base` and includes the `Coil` singleton and the `ImageView` extension functions.
* `io.coil-kt:coil-base`: The base artifact which **does not** include the `Coil` singleton and the `ImageView` extension functions.
* `io.coil-kt:coil-gif`: Includes two [decoders](../api/coil-base/coil.decode/-decoder) to support decoding GIFs. See [GIFs](gifs.md) for more details.
* `io.coil-kt:coil-svg`: Includes a [decoder](../api/coil-base/coil.decode/-decoder) to support decoding SVGs. See [SVGs](svgs.md) for more details.
* `io.coil-kt:coil-video`: Includes two [fetchers](../api/coil-base/coil.fetch/-fetcher) to support fetching and decoding frames from [any of Android's supported video formats](https://developer.android.com/guide/topics/media/media-formats#video-codecs). See [videos](videos.md) for more details.

You should depend on `io.coil-kt:coil-base` and **not** `io.coil-kt:coil` if either of the following is true:

- You are writing a library that depends on Coil. This is to avoid opting your users into the singleton.
- You want to use dependency injection to inject your [ImageLoader](image_loaders.md) instance(s).

If you need [transformations](transformations.md) that aren't part of the base Coil artifact, check out the third-party `coil-transformations` library hosted [here](https://github.com/Commit451/coil-transformations).

## Java 8

Coil requires Java 8 bytecode. To enable Java 8 [desugaring by D8](https://developer.android.com/studio/write/java8-support) add the following to your Gradle build script:

Gradle (`.gradle`):

```groovy
android {
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile).all {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
```

Gradle Kotlin DSL (`.gradle.kts`):

```kotlin
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
```

## Image Loaders

[`ImageLoader`](image_loaders.md)s are service classes that execute [`ImageRequest`](image_requests.md)s. `ImageLoader`s handle caching, data fetching, image decoding, request management, bitmap pooling, memory management, and more. New instances can be created and configured using a builder:

```kotlin
val imageLoader = ImageLoader.Builder(context)
    .availableMemoryPercentage(0.25)
    .crossfade(true)
    .build()
```

Coil performs best when you create a single `ImageLoader` and share it throughout your app. This is because each `ImageLoader` has its own memory cache, bitmap pool, and network observer.

It's recommended, though not required, to call [`shutdown`](../api/coil-base/coil/-image-loader/shutdown/) when you've finished using an image loader. Calling `shutdown` preemptively frees its memory and cleans up any observers. If you only create and use a single `ImageLoader`, you do not need to shut it down as it will be freed when your app is killed.

## Image Requests

[`ImageRequest`](image_requests.md)s are value classes that are executed by [`ImageLoader`](image_loaders.md)s. They describe where an image should be loaded from, how it should be loaded, and any extra parameters. An `ImageLoader` has two methods that can execute a request:

- `enqueue`: Enqueues the `ImageRequest` to be executed asynchronously on a background thread.
- `execute`: Executes the `ImageRequest` in the current coroutine and returns an [`ImageResult`](../api/coil-base/coil/request/-image-result).

All requests should set `data` (i.e. url, uri, file, drawable resource, etc.). This is what the `ImageLoader` will use to decide where to fetch the image data from. If you do not set `data`, it will default to [`NullRequestData`](../api/coil-base/coil/request/-null-request-data).

Additionally, you likely want to set a `target` when enqueuing a request. It's optional, but the `target` is what will receive the loaded placeholder/success/error drawables. Executed requests return an `ImageResult` which has the success/error drawable.

Here's an example:

```kotlin
// enqueue
val request = ImageRequest.Builder(context)
    .data("https://www.example.com/image.jpg")
    .target(imageView)
    .build()
val disposable = imageLoader.enqueue(request)

// execute
val request = ImageRequest.Builder(context)
    .data("https://www.example.com/image.jpg")
    .build()
val result = imageLoader.execute(request)
```

## Singleton

If you are using the `io.coil-kt:coil` artifact, you can set a default [`ImageLoader`](image_loaders.md) instance by either:

- Implementing `ImageLoaderFactory` on your `Application` class (prefer this method):

```kotlin
class MyApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .okHttpClient {
                OkHttpClient.Builder()
                    .cache(CoilUtils.createDefaultCache(context))
                    .build()
            }
            .build()
    }
}
```

**Or** calling `Coil.setImageLoader`:

```kotlin
val imageLoader = ImageLoader.Builder(context)
    .crossfade(true)
    .okHttpClient {
        OkHttpClient.Builder()
            .cache(CoilUtils.createDefaultCache(context))
            .build()
    }
    .build()
Coil.setImageLoader(imageLoader)
```

The default `ImageLoader` can be retrieved like so:

```kotlin
val imageLoader = Coil.imageLoader(context)
```

Setting a default `ImageLoader` is optional. If you don't set one, Coil will lazily create an `ImageLoader` with the default values.

If you're using the `io.coil-kt:coil-base` artifact, you should create your own `ImageLoader` instance(s) and inject them throughout your app with dependency injection. [Read more about dependency injection here](../image_loaders/#singleton-vs-dependency-injection).

!!! Note
    If you set a custom `OkHttpClient`, you must set a `cache` implementation or the `ImageLoader` will have no disk cache. A default Coil cache instance can be created using [`CoilUtils.createDefaultCache`](../api/coil-base/coil.util/-coil-utils/create-default-cache/).

## ImageView Extension Functions

The `io.coil-kt:coil` artifact provides a set of type-safe `ImageView` extension functions. Here's an example for loading a URL into an `ImageView`:

```kotlin
imageView.load("https://www.example.com/image.jpg")
```

The above call is equivalent to:

```kotlin
val imageLoader = Coil.imageLoader(context)
val request = ImageRequest.Builder(imageView.context)
    .data("https://www.example.com/image.jpg")
    .target(imageView)
    .build()
imageLoader.enqueue(request)
```

`ImageView.load` calls can be configured with an optional trailing lambda parameter:

```kotlin
imageView.load("https://www.example.com/image.jpg") {
    crossfade(true)
    placeholder(R.drawable.image)
    transformations(CircleCropTransformation())
}
```

See the docs [here](../api/coil-singleton/coil/) and [here](../api/coil-base/coil/) for more information.

## Supported Data Types

The base data types that are supported by all `ImageLoader` instances are:

* String (mapped to a Uri)
* HttpUrl
* Uri (`android.resource`, `content`, `file`, `http`, and `https` schemes only)
* File
* @DrawableRes Int
* Drawable
* Bitmap

## Preloading

To preload an image into memory, enqueue or execute an `ImageRequest` without a `Target`:

```kotlin
val request = ImageRequest.Builder(context)
    .data("https://www.example.com/image.jpg")
    // Optional, but setting a ViewSizeResolver will conserve memory by limiting the size the image should be preloaded into memory at.
    .size(ViewSizeResolver(imageView))
    .build()
imageLoader.enqueue(request)
```

To preload a network image only into the disk cache, disable the memory cache for the request:

```kotlin
val request = ImageRequest.Builder(context)
    .data("https://www.example.com/image.jpg")
    .memoryCachePolicy(CachePolicy.DISABLED)
    .build()
imageLoader.enqueue(request)
```

## Cancelling Requests

`ImageRequest`s will be automatically cancelled in the following cases:

- `request.lifecycle` reaches the `DESTROYED` state.
- `request.target` is a `ViewTarget` and its `View` is detached.

Additionally, `ImageLoader.enqueue` returns a [Disposable](../api/coil-base/coil.request/-disposable), which can be used to dispose the request (which cancels it and frees its associated resources):

```kotlin
val disposable = imageView.load("https://www.example.com/image.jpg")

// Cancel the request.
disposable.dispose()
```

## Image Sampling

Suppose you have an image that is 500x500 on disk, but you only need to load it into memory at 100x100 to be displayed in a view. Coil will load the image into memory, but what happens now if you need the image at 500x500? There's still more "quality" to read from disk, but the image is already loaded into memory at 100x100. Ideally, we would use the 100x100 image as a placeholder while we read the image from disk at 500x500.

This is exactly what Coil does and **Coil handles this process automatically for all BitmapDrawables**. Paired with `crossfade(true)`, this can create a visual effect where the image detail appears to fade in, similar to a [progressive JPEG](https://www.liquidweb.com/kb/what-is-a-progressive-jpeg/).

Here's what it looks like in the sample app:

<p style="text-align: center;">
    <video width="360" height="640" autoplay loop muted playsinline>
        <source src="../images/crossfade.mp4" type="video/mp4">
    </video>
</p>

*Images in the list have intentionally been loaded with very low detail and the crossfade is slowed down to highlight the visual effect.*

## Bitmap Pooling

Similar to Glide and Fresco, Coil supports bitmap pooling. Bitmap pooling is a technique to re-use Bitmap objects once they are no longer in use (i.e. when a View is detached, a Fragment's view is destroyed, etc.). This can significantly improve memory performance (especially on pre-Oreo devices), however it creates several API limitations.

To make this optimization as seamless and transparent to the consumer as possible, [Targets](targets.md) must opt-in to bitmap pooling. To opt in, implement `PoolableViewTarget`; this requires the target to release all references to its current `Drawable` when its next lifecycle method is called.

See [PoolableViewTarget](../api/coil-base/coil.target/-poolable-view-target) for more information.

!!! Note
    Do not use `ImageView.getDrawable` if the `ImageView`'s `Drawable` has been set through Coil's `ImageViewTarget`. Instead, either load the image using a custom `Target` or copy underlying `Bitmap` using `Bitmap.copy`.
