# Getting Started

## Compose UI

A typical Compose UI project will want to import:

```kotlin
implementation("io.coil-kt.coil3:coil-compose:3.0.0")
implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.0")
```

!!! Note
    If you use Compose Multiplatform, you'll need to use Ktor to download network images. See [here](network.md#ktor-network-engines) for how to do that.

After that's imported you can load images from the network using `AsyncImage`:

```kotlin
AsyncImage(
    model = "https://www.example.com/image.jpg",
    contentDescription = null,
)
```

## Android Views

If you use Android Views instead of Compose UI import:

```kotlin
implementation("io.coil-kt.coil3:coil:3.0.0")
implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.0")
```

After that's imported you can load images from the network using the `ImageView.load` extension function:

```kotlin
imageView.load("https://www.example.com/image.jpg") {
    crossfade(true)
}
```

## Configuring the singleton ImageLoader

By default, Coil includes a singleton `ImageLoader`. The `ImageLoader` executes incoming `ImageRequest`s by fetching, decoding, caching, and returning the result. You don't need to configure your `ImageLoader`; if you don't Coil will create the singleton `ImageLoader` with the default configuration.

You can configure it a number of ways (**choose only one**):

- Call `setSingletonImageLoaderFactory` near the entrypoint to your app (the root `@Composable` of your app). **This works best for Compose Multiplatform apps.**

```kotlin
setSingletonImageLoaderFactory { context ->
    ImageLoader.Builder(context)
        .crossfade(true)
        .build()
}
```

- Implement `SingletonImageLoader.Factory` on your [`Application`](https://developer.android.com/reference/android/app/Application) in Android. **This works best for Android apps.**

```kotlin
class CustomApplication : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }
}
```

- Call `SingletonImageLoader.setSafe` near the entrypoint to your app (e.g. in `Application.onCreate` on Android). This is the most flexible.

```kotlin
SingletonImageLoader.setSafe { context ->
    ImageLoader.Builder(context)
        .crossfade(true)
        .build()
}
```

!!! Note
    If you are writing a library that depends on Coil you should NOT get/set the singleton `ImageLoader`. Instead, you should depend on `io.coil-kt.coil3:coil-core`, create your own `ImageLoader`, and pass it around manually. If you set the singleton `ImageLoader` in your library you could be overwriting the `ImageLoader` set by the app using your library if they also use Coil.

## Artifacts

Here's a list of the main artifacts Coil has published to `mavenCentral()`:

* `io.coil-kt.coil3:coil`: The default artifact which depends on `io.coil-kt.coil3:coil-core`. It includes a singleton `ImageLoader` and related extension functions.
* `io.coil-kt.coil3:coil-core`: A subset of `io.coil-kt.coil3:coil` which **does not** include the singleton `ImageLoader` and related extension functions.
* `io.coil-kt.coil3:coil-compose`: The default [Compose UI](https://www.jetbrains.com/compose-multiplatform/) artifact which depends on `io.coil-kt.coil3:coil` and `io.coil-kt.coil3:coil-compose-core`. It includes overloads for `AsyncImage`, `rememberAsyncImagePainter`, and `SubcomposeAsyncImage` that use the singleton `ImageLoader`.
* `io.coil-kt.coil3:coil-compose-core`: A subset of `io.coil-kt.coil3:coil-compose` which does not include functions that depend on the singleton `ImageLoader`.
* `io.coil-kt.coil3:coil-network-okhttp`: Includes support for fetching images from the network using [OkHttp](https://github.com/square/okhttp).
* `io.coil-kt.coil3:coil-network-ktor2`: Includes support for fetching images from the network using [Ktor 2](https://github.com/ktorio/ktor).
* `io.coil-kt.coil3:coil-network-ktor3`: Includes support for fetching images from the network using [Ktor 3](https://github.com/ktorio/ktor).
* `io.coil-kt.coil3:coil-network-cache-control`: Includes support for respecting [`Cache-Control` headers](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cache-Control) when fetching images from the network.
* `io.coil-kt.coil3:coil-gif`: Includes two [decoders](/coil/api/coil-core/coil3.decode/-decoder) to support decoding GIFs. See [GIFs](gifs.md) for more details.
* `io.coil-kt.coil3:coil-svg`: Includes a [decoder](/coil/api/coil-core/coil3.decode/-decoder) to support decoding SVGs. See [SVGs](svgs.md) for more details.
* `io.coil-kt.coil3:coil-video`: Includes a [decoder](/coil/api/coil-core/coil3.decode/-decoder) to support decoding frames from [any of Android's supported video formats](https://developer.android.com/guide/topics/media/media-formats#video-codecs). See [videos](videos.md) for more details.
* `io.coil-kt.coil3:coil-test`: Includes classes to support testing. See [testing](testing.md) for more details.
* `io.coil-kt.coil3:coil-bom`: Includes a [bill of materials](https://docs.gradle.org/7.2/userguide/platforms.html#sub:bom_import). Importing `coil-bom` allows you to depend on other Coil artifacts without specifying a version.
