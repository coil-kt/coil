# Getting Started

## Adding Dependencies

All of Coil's artifacts are published to `mavenCentral()`, so make sure that repository is declared in your project before adding any of the dependencies below.

### Direct declaration

The simplest way to add a Coil dependency is to declare it directly with its coordinates and version, as shown throughout this page:

```kotlin
implementation("io.coil-kt.coil3:coil-compose:3.7.0-SNAPSHOT")
```

### Version catalog

If your project uses a [Gradle version catalog](https://docs.gradle.org/current/userguide/version_catalogs.html#sec:accessing-catalog) (a `libs.versions.toml` file under `gradle/`), declare Coil's version and libraries there instead of hardcoding them in each module's build script:

```toml
[versions]
coil = "3.7.0-SNAPSHOT"

[libraries]
coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }
coil-network-okhttp = { group = "io.coil-kt.coil3", name = "coil-network-okhttp", version.ref = "coil" }
```

Then reference the generated accessors in your module's `build.gradle.kts`:

```kotlin
implementation(libs.coil.compose)
implementation(libs.coil.network.okhttp)
```

This keeps every Coil version in one place and lets Gradle's IDE tooling autocomplete and validate the coordinates for you.

### Published version catalog

Some libraries go a step further and publish their own [version catalog artifact](https://docs.gradle.org/current/userguide/version_catalogs.html#sec:importing-published-catalog), so consumers don't even need to write out the `[versions]`/`[libraries]` entries themselves — they just import the catalog and get type-safe accessors for every module the library publishes.

You import it in `settings.gradle.kts`: need to change claude

```kotlin
dependencyResolutionManagement {
    versionCatalogs {
        create("coilLibs") {
            from("io.coil-kt.coil3:coil-version-catalog:3.7.0-SNAPSHOT")
        }
    }
}
```

and then reference its modules directly in `build.gradle.kts`:

```kotlin
dependencies {
    implementation(coilLibs.coil)
    implementation(coilLibs.coil.core)
    implementation(coilLibs.coil.compose)
}
```

### Coil's BOM

Coil also publishes a bill of materials (BOM), `io.coil-kt.coil3:coil-bom`, which pins compatible versions for all of Coil's artifacts. Importing it with `platform()` lets you omit the version on every individual Coil dependency, so they can't drift out of sync with each other:

```kotlin
implementation(platform("io.coil-kt.coil3:coil-bom:3.7.0-SNAPSHOT"))
implementation("io.coil-kt.coil3:coil-compose")
implementation("io.coil-kt.coil3:coil-network-okhttp")
```

You can combine this with a version catalog by declaring the BOM as a platform entry and depending on it the same way:

```toml
[versions]
coil = "3.7.0-SNAPSHOT"

[libraries]
coil-bom = { group = "io.coil-kt.coil3", name = "coil-bom", version.ref = "coil" }
coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose" }
coil-network-okhttp = { group = "io.coil-kt.coil3", name = "coil-network-okhttp" }
```

```kotlin
implementation(platform(libs.coil.bom))
implementation(libs.coil.compose)
implementation(libs.coil.network.okhttp)
```

## Platform-Specific Setup

## Compose UI

A typical Compose UI project will want to import:

```kotlin
implementation("io.coil-kt.coil3:coil-compose:3.7.0-SNAPSHOT")
implementation("io.coil-kt.coil3:coil-network-okhttp:3.7.0-SNAPSHOT")
```

After that's imported you can load images from the network using `AsyncImage`:

```kotlin
AsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = null,
)
```

!!! Note
    If you use Compose Multiplatform, you'll need to use Ktor instead of OkHttp. See [here](network.md#ktor-network-engines) for how to do that.

## Android Views

If you use Android Views instead of Compose UI import:

```kotlin
implementation("io.coil-kt.coil3:coil:3.7.0-SNAPSHOT")
implementation("io.coil-kt.coil3:coil-network-okhttp:3.7.0-SNAPSHOT")
```

After that's imported you can load images from the network using the `ImageView.load` extension function:

```kotlin
imageView.load("https://example.com/image.jpg")
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

## Images

To support multiplatform rendering, Coil 3.x uses a custom `coil3.Image` class. It replaces Android's `Drawable`, but is fully interoperable with it:

```kotlin
val drawable = image.asDrawable(resources)
val image = drawable.asImage()
```

Coil also defines a `coil3.Bitmap` class, which is a type alias for `android.graphics.Bitmap` on Android or `org.jetbrains.skia.Bitmap` on non-Android platforms:

```kotlin
val bitmap = image.toBitmap()
val image = bitmap.asImage()
```

It's also interoperable with Compose UI's `Painter` class. This extension function requires importing the `coil-compose-core` artifact:

```kotlin
val painter = image.asPainter()
```

!!! Note
    `Painter`s can't be converted to `Image`s as painters can only be rendered inside a composition whereas `Image`s must be able to be rendered on any `Canvas`.

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
