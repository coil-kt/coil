# Getting Started

## Artifacts

Coil has several artifacts published to `mavenCentral()`:

* `io.coil-kt.coil3:coil`: The default artifact which depends on `io.coil-kt.coil3:coil-core`. It includes a singleton `ImageLoader` and related extension functions.
* `io.coil-kt.coil3:coil-core`: A subset of `io.coil-kt.coil3:coil` which **does not** include the singleton `ImageLoader` and related extension functions.
* `io.coil-kt.coil3:coil-compose`: The default [Compose](https://www.jetbrains.com/compose-multiplatform/) artifact which depends on `io.coil-kt.coil3:coil` and `io.coil-kt.coil3:coil-compose-core`. It includes overloads for `AsyncImage`, `rememberAsyncImagePainter`, and `SubcomposeAsyncImage` that use the singleton `ImageLoader`.
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

## Quick Start

A standard Android Compose UI project will want to import:

```kotlin
implementation("io.coil-kt.coil3:coil-compose:3.0.0-rc01")
implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.0-rc01")
```

After that's imported you can load images from the network using `AsyncImage`:

```kotlin
AsyncImage(
    model = "https://www.example.com/image.jpg",
    contentDescription = null,
)
```

If you want to add some shared configuration to your `AsyncImage`s, use `setSingletonImageLoader` to set a custom `ImageLoader`:

```kotlin
// Make sure this is set near the entrypoint of your app.
setSingletonImageLoaderFactory { context ->
    ImageLoader.Builder(context)
        .crossfade(true)
        .build()
}
```

!!!
    If you use Compose Multiplatform, you'll need to use Ktor for download network images. See [here](network.md) for how to do that.