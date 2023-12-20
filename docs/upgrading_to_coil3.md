# Upgrading to Coil 3.x

!!! Heads up!
    Coil 3 is currently in alpha and its API and behaviour may change between releases. Coil 3 alphas are not guaranteed to be binary or source compatible with each other.

Coil 3 is the next major version of Coil that supports [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) and includes performance and API improvements. This document provides a high-level overview of the main changes from Coil 2 to Coil 3 and highlights any breaking or important changes. It does not cover every binary incompatible change or small behaviour change.

Using Coil 3 in a Compose Multiplatform project? Check out the [`samples`](https://github.com/coil-kt/coil/tree/3.x/samples/compose) repository for examples.

## Maven Coordinates and Package Name

Coil's Maven coordinates were updated to `io.coil-kt.coil3` and its package name was updated to `coil3`. This allows Coil 3 to run side by side with Coil 2 without binary compatibility issues. For example, `io.coil-kt:coil:2.5.0` is now `io.coil-kt.coil3:coil:3.0.0-alpha01`.

The `coil-base` and `coil-compose-base` artifacts were renamed to `coil-core` and `coil-compose-core` respectively to align with the naming conventions used by Coroutines, Ktor, and AndroidX.

## Multiplatform

Coil 3 is now a Kotlin Multiplatform library that supports Android, JVM, iOS, macOS, and Javascript. WASM JS is also supported via a separate `3.0.0-alpha01-wasm` artifact. Once, [Ktor](https://ktor.io/) releases WASM support on Maven Central, Coil will merge WASM support into the main artifact.

On Android, Coil uses the standard graphics classes to render images. On non-Android platforms, Coil uses [Skiko](https://github.com/JetBrains/skiko) to render images. Skiko is a set of Kotlin bindings that wrap the [Skia](https://github.com/google/skia) graphics engine developed by Google.

As part of decoupling from the Android SDK, a number of API changes were made. Notably:

- `Drawable` was replaced with a custom `Image` class. Use `Drawable.asCoilImage()` and `Image.asDrawable()` to convert between the classes on Android. On non-Android platforms use `Bitmap.asCoilImage()` and `Image.asBitmap()`.
    - The `Image` API is experimental (especially on non-Android platforms) and is likely to change.
- Android's `android.net.Uri` class was replaced a multiplatform `coil3.Uri` class. Any instances of `android.net.Uri` that are used as `ImageRequest.data` will be mapped to `coil3.Uri` before being fetched/decoded.
- Usages of `Context` were replaced with `PlatformContext`. `PlatformContext` is a type alias for `Context` on Android and can be accessed using `PlatformContext.INSTANCE` on non-Android platforms.

The `coil-gif`, `coil-svg`, and `coil-video` artifacts continue to be Android-only as they rely on specific Android decoders and libraries.

## Network Images

Coil's network image fetcher was extracted into a separate artifact, `coil-network`, and now depends on [Ktor](https://ktor.io/) instead of [OkHttp](https://square.github.io/okhttp/).

**IMPORTANT**: `ImageLoader` no longer supports network URLs by default. You'll need to import `coil-network`, add the `NetworkFetcher.Factory()` to your `ImageLoader` manually and [import a Ktor engine](https://ktor.io/docs/http-client-engines.html). Here's how:

```kotlin
// Import coil-network and an HTTP client engine.
implementation("io.coil-kt.coil3:coil-network:3.0.0-alpha01")
implementation("io.ktor:ktor-client-okhttp:[ktor-version]")
```

If you use the singleton `ImageLoader` on Android, create a custom `ImageLoader` in your Application's `SingletonImageLoader.Factory`:

```kotlin
class Application : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(NetworkFetcher.Factory())
            }
            .build()
    }
}
```

On non-Android platforms, you can create a function that initializes the singleton `ImageLoader` and call it at the entrypoint to your app:

```kotlin
fun initializeSingletonImageLoader() {
    // Call the `initialize` variable to set the singleton image loader exactly once.
    initialize
}

private val initialize by lazy {
    SingletonImageLoader.set {
        ImageLoader.Builder(context)
            .components {
                add(NetworkFetcher.Factory())
            }
            .build()
    }
}
```

Here's an example for initializing the singleton `ImageLoader` inside a Compose Multiplatform iOS app:

```kotlin
fun MainViewController(): UIViewController {
    initializeSingletonImageLoader()

    return ComposeUIViewController {
        App()
    }
}
```

Check out the [`samples`](https://github.com/coil-kt/coil/tree/3.x/samples/compose) repository for examples.

**IMPORTANT**: `Cache-Control` header support is no longer enabled by default. In subsequent alphas, it will be possible to re-enable it, but it will be opt-in. `NetworkFetcher.Factory` now also supports custom `CacheStrategy` implementations to allow custom cache resolution behaviour.

## Compose Multiplatform

Coil's Compose API is largely unchanged, however it includes changes to ensure `AsyncImage`, `SubcomposeAsyncImage`, and `rememberAsyncImagePainter` are [restartable and skippable](https://developer.android.com/jetpack/compose/performance/stability). This should provide a performance benefit as those composable functions will often be entirely skipped when their parent composable recomposes.
