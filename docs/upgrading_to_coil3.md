# Upgrading to Coil 3.x

!!! Note
    Coil 3 is currently in alpha and its API and behaviour may change between releases. Coil 3 alphas are not guaranteed to be binary or source compatible with each other.

Coil 3 is the next major version of Coil that supports [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) and includes performance and API improvements. This document provides a high-level overview of the main changes from Coil 2 to Coil 3 and highlights any breaking or important changes. It does not cover every binary incompatible change or small behaviour change.

Using Coil 3 in a Compose Multiplatform project? Check out the [`samples`](https://github.com/coil-kt/coil/tree/3.x/samples/compose) repository for examples.

## Maven Coordinates and Package Name

Coil's Maven coordinates were updated to `io.coil-kt.coil3` and its package name was updated to `coil3`. This allows Coil 3 to run side by side with Coil 2 without binary compatibility issues. For example, `io.coil-kt:coil:2.6.0` is now `io.coil-kt.coil3:coil:3.0.0-alpha01`.

The `coil-base` and `coil-compose-base` artifacts were renamed to `coil-core` and `coil-compose-core` respectively to align with the naming conventions used by Coroutines, Ktor, and AndroidX.

## Multiplatform

Coil 3 is now a Kotlin Multiplatform library that supports Android, JVM, iOS, macOS, Javascript, and WASM.

On Android, Coil uses the standard graphics classes to render images. On non-Android platforms, Coil uses [Skiko](https://github.com/JetBrains/skiko) to render images. Skiko is a set of Kotlin bindings that wrap the [Skia](https://github.com/google/skia) graphics engine developed by Google.

As part of decoupling from the Android SDK, a number of API changes were made. Notably:

- `Drawable` was replaced with a custom `Image` class. Use `Drawable.asCoilImage()` and `Image.asDrawable()` to convert between the classes on Android. On non-Android platforms use `Bitmap.asCoilImage()` and `Image.asBitmap()`.
    - The `Image` API is experimental (especially on non-Android platforms) and is likely to change.
- Android's `android.net.Uri` class was replaced a multiplatform `coil3.Uri` class. Any instances of `android.net.Uri` that are used as `ImageRequest.data` will be mapped to `coil3.Uri` before being fetched/decoded.
- Usages of `Context` were replaced with `PlatformContext`. `PlatformContext` is a type alias for `Context` on Android and can be accessed using `PlatformContext.INSTANCE` on non-Android platforms.
- The `Coil` class was renamed to `SingletonImageLoader`.

The `coil-gif` and `coil-video` artifacts continue to be Android-only as they rely on specific Android decoders and libraries.

## Compose

The `coil-compose` artifact's APIs are mostly unchanged. You can continue using `AsyncImage`, `SubcomposeAsyncImage`, and `rememberAsyncImagePainter` the same way as with Coil 2.x. Additionally, this methods have been updated to be [restartable and skippable](https://developer.android.com/jetpack/compose/performance/stability) which should improve their performance.

!!! Note
    If you use Coil on a JVM (non-Android) platform, you'll need to add a dependency on a [coroutines main dispatcher](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-dispatchers/-main.html). On desktop you likely want to import `org.jetbrains.kotlinx:kotlinx-coroutines-swing`.

## Network Images

**IMPORTANT** Coil's network image support was extracted out of `coil-core`. To load images from a network URL in Coil 3.0 you'll need to import a separate artifact, either:

- Import `coil-network-okhttp` if you prefer using [OkHttp](https://square.github.io/okhttp/). OkHttp is Android/JVM-only.

```kotlin
implementation("io.coil-kt.coil3:coil:[coil-version]")
implementation("io.coil-kt.coil3:coil-network-okhttp:[coil-version]")
```

- Import `coil-network-ktor` and a [Ktor engine](https://ktor.io/docs/http-client-engines.html) if you prefer using [Ktor](https://ktor.io/).

```kotlin
implementation("io.coil-kt.coil3:coil:[coil-version]")
implementation("io.coil-kt.coil3:coil-network-ktor:[coil-version]")
implementation("io.ktor:ktor-client-android:[ktor-version]")
```

Check out the [`samples`](https://github.com/coil-kt/coil/tree/3.x/samples/compose) repository for examples.

**IMPORTANT**: `Cache-Control` header support is no longer enabled by default. In subsequent alphas, it will be possible to re-enable it, but it will be opt-in. `NetworkFetcher.Factory` now also supports custom `CacheStrategy` implementations to allow custom cache resolution behaviour.

## Extras

Coil 2's `Parameters` API was replaced by `Extras`. `Extras` don't require a string key and instead rely on identity equality. `Extras` don't support modifying the memory cache key. Instead, use `ImageRequest.memoryCacheKeyExtra` if your extra affects the memory cache key.
