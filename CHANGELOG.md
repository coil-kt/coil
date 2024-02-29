# Changelog

## [3.0.0-alpha06] - February 29, 2024

- Downgrade Skiko to 0.7.93.
- [For the full list of important changes in 3.x, check out the upgrade guide.](https://coil-kt.github.io/coil/upgrading_to_coil3/)

## [3.0.0-alpha05] - February 28, 2024

- **New**: Support the `wasmJs` target.
- Create `DrawablePainter` and `DrawableImage` to support drawing `Image`s that aren't backed by a `Bitmap` on non-Android platforms.
    - The `Image` APIs are experimental and likely to change between alpha releases.
- Update `ContentPainterModifier` to implement `Modifier.Node`.
- Fix: Lazily register component callbacks and the network observer on a background thread. This fixes slow initialization that would typically occur on the main thread.
- Fix: Fix `ImageLoader.Builder.placeholder/error/fallback` not being used by `ImageRequest`.
- Update Compose to 1.6.0.
- Update Coroutines to 1.8.0.
- Update Okio to 3.8.0.
- Update Skiko to 0.7.94.
- [For the full list of important changes in 3.x, check out the upgrade guide.](https://coil-kt.github.io/coil/upgrading_to_coil3/)

## [2.6.0] - February 23, 2024

- Make `rememberAsyncImagePainter`, `AsyncImage`, and `SubcomposeAsyncImage` [restartable and skippable](https://developer.android.com/jetpack/compose/performance/stability#functions). This improves performance by avoiding recomposition unless one of the composable's arguments changes.
    - Add an optional `modelEqualityDelegate` argument to `rememberAsyncImagePainter`, `AsyncImage`, and `SubcomposeAsyncImage` to control whether the `model` will trigger a recomposition.
- Update `ContentPainterModifier` to implement `Modifier.Node`.
- Fix: Lazily register component callbacks and the network observer on a background thread. This fixes slow initialization that would typically occur on the main thread.
- Fix: Avoid relaunching a new image request in `rememberAsyncImagePainter`, `AsyncImage`, and `SubcomposeAsyncImage` if `ImageRequest.listener` or `ImageRequest.target` change.
- Fix: Don't observe the image request twice in `AsyncImagePainter`.
- Update Kotlin to 1.9.22.
- Update Compose to 1.6.1.
- Update Okio to 3.8.0.
- Update `androidx.collection` to 1.4.0.
- Update `androidx.lifecycle` to 2.7.0.

## [3.0.0-alpha04] - February 1, 2024

- **Breaking**: Remove `Lazy` from `OkHttpNetworkFetcherFactory` and `KtorNetworkFetcherFactory`'s public API.
- Expose `Call.Factory` instead of `OkHttpClient` in `OkHttpNetworkFetcherFactory`.
- Convert `NetworkResponseBody` to wrap a `ByteString`.
- Downgrade Compose to 1.5.12.
- [For the full list of important changes, check out the upgrade guide.](https://coil-kt.github.io/coil/upgrading_to_coil3/)

## [3.0.0-alpha03] - January 20, 2024

- **Breaking**: `coil-network` has been renamed to `coil-network-ktor`. Additionally, there is a new `coil-network-okhttp` artifact that depends on OkHttp and doesn't require specifying a Ktor engine.
    - Depending on which artifact you import you can reference the `Fetcher.Factory` manually using `KtorNetworkFetcherFactory` or `OkHttpNetworkFetcherFactory`.
- Support loading `NSUrl` on Apple platforms.
- Add `clipToBounds` parameter to `AsyncImage`.
- [For the full list of important changes, check out the upgrade guide.](https://coil-kt.github.io/coil/upgrading_to_coil3/)

## [3.0.0-alpha02] - January 10, 2024

- **Breaking**: `coil-gif`, `coil-network`, `coil-svg`, and `coil-video`'s packages have been updated so all their classes are part of `coil.gif`, `coil.network`, `coil.svg`, and `coil.video` respectively. This helps avoid class name conflicts with other artifacts.
- **Breaking**: `ImageDecoderDecoder` has been renamed to `AnimatedImageDecoder`.
- **New**: `coil-gif`, `coil-network`, `coil-svg`, and `coil-video`'s components are now automatically added to each `ImageLoader`'s `ComponentRegistry`.
    - To be clear, unlike `3.0.0-alpha01` **you do not need to manually add `NetworkFetcher.Factory()` to your `ComponentRegistry`**. Simply importing `io.coil-kt.coil3:coil-network:[version]` and [a Ktor engine](https://ktor.io/docs/http-client-engines.html#dependencies) is enough to load network images.
    - It's safe to also add these components to `ComponentRegistry` manually. Any manually added components take precedence over components that are added automatically.
    - If preferred, this behaviour can be disabled using `ImageLoader.Builder.serviceLoaderEnabled(false)`.
- **New**: Support `coil-svg` on all platforms. It's backed by [AndroidSVG](https://bigbadaboom.github.io/androidsvg/) on Android and [SVGDOM](https://api.skia.org/classSkSVGDOM.html) on non-Android platforms.
- Coil now uses Android's [`ImageDecoder`](https://developer.android.com/reference/android/graphics/ImageDecoder) API internally, which has performance benefits when decoding directly from a file, resource, or content URI.
- Fix: Multiple `coil3.Uri` parsing fixes.
- [For the full list of important changes, check out the upgrade guide.](https://coil-kt.github.io/coil/upgrading_to_coil3/)

## [3.0.0-alpha01] - December 30, 2023

- **New**: [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) support. Coil is now a Kotlin Multiplatform library that supports Android, JVM, iOS, macOS, and Javascript.
- Coil's Maven coordinates were updated to `io.coil-kt.coil3` and its imports were updated to `coil3`. This allows Coil 3 to run side by side with Coil 2 without binary compatibility issues. For example, `io.coil-kt:coil:[version]` is now `io.coil-kt.coil3:coil:[version]`.
- The `coil-base` and `coil-compose-base` artifacts were renamed to `coil-core` and `coil-compose-core` respectively to align with the naming conventions used by Coroutines, Ktor, and AndroidX.
- [For the full list of important changes, check out the upgrade guide.](https://coil-kt.github.io/coil/upgrading_to_coil3/)

## [2.5.0] - October 30, 2023

- **New**: Add `MediaDataSourceFetcher.Factory` to support decoding `MediaDataSource` implementations in `coil-video`. ([#1795](https://github.com/coil-kt/coil/pull/1795))
- Add the `SHIFT6m` device to the hardware bitmap blocklist. ([#1812](https://github.com/coil-kt/coil/pull/1812))
- Fix: Guard against painters that return a size with one unbounded dimension. ([#1826](https://github.com/coil-kt/coil/pull/1826))
- Fix: Disk cache load fails after `304 Not Modified` when cached headers include non-ASCII characters. ([#1839](https://github.com/coil-kt/coil/pull/1839))
- Fix: `FakeImageEngine` not updating the interceptor chain's request. ([#1905](https://github.com/coil-kt/coil/pull/1905))
- Update compile SDK to 34.
- Update Kotlin to 1.9.10.
- Update Coroutines to 1.7.3.
- Update `accompanist-drawablepainter` to 0.32.0.
- Update `androidx.annotation` to 1.7.0.
- Update `androidx.compose.foundation` to 1.5.4.
- Update `androidx.core` to 1.12.0.
- Update `androidx.exifinterface:exifinterface` to 1.3.6.
- Update `androidx.lifecycle` to 2.6.2.
- Update `com.squareup.okhttp3` to 4.12.0.
- Update `com.squareup.okio` to 3.6.0.

## [2.4.0] - May 21, 2023

- Rename `DiskCache` `get`/`edit` to `openSnapshot`/`openEditor`.
- Don't automatically convert `ColorDrawable` to `ColorPainter` in `AsyncImagePainter`.
- Annotate simple `AsyncImage` overloads with `@NonRestartableComposable`.
- Fix: Call `Context.cacheDir` lazily in `ImageSource`.
- Fix: Fix publishing `coil-bom`.
- Fix: Fix always setting bitmap config to `ARGB_8888` if hardware bitmaps are disabled.
- Update Kotlin to 1.8.21.
- Update Coroutines to 1.7.1.
- Update `accompanist-drawablepainter` to 0.30.1.
- Update `androidx.compose.foundation` to 1.4.3.
- Update `androidx.profileinstaller:profileinstaller` to 1.3.1.
- Update `com.squareup.okhttp3` to 4.11.0.

## [2.3.0] - March 25, 2023

- **New**: Introduce a new `coil-test` artifact, which includes `FakeImageLoaderEngine`. This class is useful for hardcoding image loader responses to ensure consistent and synchronous (from the main thread) responses in tests. See [here](https://coil-kt.github.io/coil/testing) for more info.
- **New**: Add [baseline profiles](https://developer.android.com/topic/performance/baselineprofiles/overview) to `coil-base` (child module of `coil`) and `coil-compose-base` (child module of `coil-compose`).
    - This improves Coil's runtime performance and should offer [better frame timings](https://github.com/coil-kt/coil/tree/main/coil-benchmark/benchmark_output.md) depending on how Coil is used in your app.
- Fix: Fix parsing `file://` URIs with encoded data. [#1601](https://github.com/coil-kt/coil/pull/1601)
- Fix: `DiskCache` now properly computes its maximum size if passed a directory that does not exist. [#1620](https://github.com/coil-kt/coil/pull/1620)
- Make `Coil.reset` public API. [#1506](https://github.com/coil-kt/coil/pull/1506)
- Enable Java default method generation. [#1491](https://github.com/coil-kt/coil/pull/1491)
- Update Kotlin to 1.8.10.
- Update `accompanist-drawablepainter` to 0.30.0.
- Update `androidx.annotation` to 1.6.0.
- Update `androidx.appcompat:appcompat-resources` to 1.6.1.
- Update `androidx.compose.foundation` to 1.4.0.
- Update `androidx.core` to 1.9.0.
- Update `androidx.exifinterface:exifinterface` to 1.3.6.
- Update `androidx.lifecycle` to 2.6.1.
- Update `okio` to 3.3.0.

## [2.2.2] - October 1, 2022

- Ensure an image loader is fully initialized before registering its system callbacks. [#1465](https://github.com/coil-kt/coil/pull/1465)
- Set the preferred bitmap config in `VideoFrameDecoder` on API 30+ to avoid banding. [#1487](https://github.com/coil-kt/coil/pull/1487)
- Fix parsing paths containing `#` in `FileUriMapper`. [#1466](https://github.com/coil-kt/coil/pull/1466)
- Fix reading responses with non-ascii headers from the disk cache. [#1468](https://github.com/coil-kt/coil/pull/1468)
- Fix decoding videos inside asset subfolders. [#1489](https://github.com/coil-kt/coil/pull/1489)
- Update `androidx.annotation` to 1.5.0.

## [2.2.1] - September 8, 2022

- Fix: `RoundedCornersTransformation` now properly scales the `input` bitmap.
- Remove dependency on the `kotlin-parcelize` plugin.
- Update compile SDK to 33.
- Downgrade `androidx.appcompat:appcompat-resources` to 1.4.2 to work around [#1423](https://github.com/coil-kt/coil/issues/1423).

## [2.2.0] - August 16, 2022

- **New**: Add `ImageRequest.videoFramePercent` to `coil-video` to support specifying the video frame as a percent of the video's duration.
- **New**: Add `ExifOrientationPolicy` to configure how `BitmapFactoryDecoder` treats EXIF orientation data.
- Fix: Don't throw an exception in `RoundedCornersTransformation` if passed a size with an undefined dimension.
- Fix: Read a GIF's frame delay as two unsigned bytes instead of one signed byte.
- Update Kotlin to 1.7.10.
- Update Coroutines to 1.6.4.
- Update Compose to 1.2.1.
- Update OkHttp to 4.10.0.
- Update Okio to 3.2.0.
- Update `accompanist-drawablepainter` to 0.25.1.
- Update `androidx.annotation` to 1.4.0.
- Update `androidx.appcompat:appcompat-resources` to 1.5.0.
- Update `androidx.core` to 1.8.0.

## [2.1.0] - May 17, 2022

- **New**: Support loading `ByteArray`s. ([#1202](https://github.com/coil-kt/coil/pull/1202))
- **New**: Support setting custom CSS rules for SVGs using `ImageRequest.Builder.css`. ([#1210](https://github.com/coil-kt/coil/pull/1210))
- Fix: Convert `GenericViewTarget`'s private methods to protected. ([#1273](https://github.com/coil-kt/coil/pull/1273))
- Update compile SDK to 32. ([#1268](https://github.com/coil-kt/coil/pull/1268))

## [2.0.0] - May 10, 2022

Coil 2.0.0 is a major iteration of the library and includes breaking changes. Check out the [upgrade guide](https://coil-kt.github.io/coil/upgrading/) for how to upgrade.

- **New**: Introduce `AsyncImage` in `coil-compose`. Check out [the documentation](https://coil-kt.github.io/coil/compose/) for more info.

```kotlin
// Display an image from the network.
AsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = null,
)

// Display an image from the network with a placeholder, circle crop, and crossfade animation.
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data("https://example.com/image.jpg")
        .crossfade(true)
        .build(),
    placeholder = painterResource(R.drawable.placeholder),
    contentDescription = stringResource(R.string.description),
    contentScale = ContentScale.Crop,
    modifier = Modifier.clip(CircleShape)
)
```

- **New**: Introduce a public `DiskCache` API.
    - Use `ImageLoader.Builder.diskCache` and `DiskCache.Builder` to configure the disk cache.
    - You should not use OkHttp's `Cache` with Coil 2.0. See [here](https://coil-kt.github.io/coil/upgrading_to_coil2/#disk-cache) for more info.
    - `Cache-Control` and other cache headers are still supported - except `Vary` headers, as the cache only checks that the URLs match. Additionally, only responses with a response code in the range [200..300) are cached.
    - Existing disk caches will be cleared when upgrading to 2.0.
- The minimum supported API is now 21.
- `ImageRequest`'s default `Scale` is now `Scale.FIT`.
    - This was changed to make `ImageRequest.scale` consistent with other classes that have a default `Scale`.
    - Requests with an `ImageViewTarget` still have their `Scale` auto-detected.
- Rework the image pipeline classes:
    - `Mapper`, `Fetcher`, and `Decoder` have been refactored to be more flexible.
    - `Fetcher.key` has been replaced with a new `Keyer` interface. `Keyer` creates the cache key from the input data.
    - Add `ImageSource`, which allows `Decoder`s to read `File`s directly using Okio's file system API.
- Rework the Jetpack Compose integration:
    - `rememberImagePainter` and `ImagePainter` have been renamed to `rememberAsyncImagePainter` and `AsyncImagePainter` respectively.
    - Deprecate `LocalImageLoader`. Check out the deprecation message for more info.
- Disable generating runtime not-null assertions.
    - If you use Java, passing null as a not-null annotated argument to a function will no longer throw a `NullPointerException` immediately. Kotlin's compile-time null safety guards against this happening.
    - This change allows the library's size to be smaller.
- `Size` is now composed of two `Dimension` values for its width and height. `Dimension` can either be a positive pixel value or `Dimension.Undefined`. See [here](https://coil-kt.github.io/coil/upgrading/#size-refactor) for more info.
- `BitmapPool` and `PoolableViewTarget` have been removed from the library.
- `VideoFrameFileFetcher` and `VideoFrameUriFetcher` have been removed from the library. Use `VideoFrameDecoder` instead, which supports all data sources.
- [`BlurTransformation`](https://github.com/coil-kt/coil/blob/845f39383f332428077c666e3567b954675ce248/coil-core/src/main/java/coil/transform/BlurTransformation.kt) and [`GrayscaleTransformation`](https://github.com/coil-kt/coil/blob/845f39383f332428077c666e3567b954675ce248/coil-core/src/main/java/coil/transform/GrayscaleTransformation.kt) are removed from the library. If you use them, you can copy their code into your project.
- Change `Transition.transition` to be a non-suspending function as it's no longer needed to suspend the transition until it completes.
- Add support for `bitmapFactoryMaxParallelism`, which restricts the maximum number of in-progress `BitmapFactory` operations. This value is 4 by default, which improves UI performance.
- Add support for `interceptorDispatcher`, `fetcherDispatcher`, `decoderDispatcher`, and `transformationDispatcher`.
- Add `GenericViewTarget`, which handles common `ViewTarget` logic.
- Add `ByteBuffer` to the default supported data types.
- `Disposable` has been refactored and exposes the underlying `ImageRequest`'s job.
- Rework the `MemoryCache` API.
- `ImageRequest.error` is now set on the `Target` if `ImageRequest.fallback` is null.
- `Transformation.key` is replaced with `Transformation.cacheKey`.
- Update Kotlin to 1.6.10.
- Update Compose to 1.1.1.
- Update OkHttp to 4.9.3.
- Update Okio to 3.0.0.

Changes from `2.0.0-rc03`:
- Convert `Dimension.Original` to be `Dimension.Undefined`.
    - This changes the semantics of the non-pixel dimension slightly to fix some edge cases ([example](https://github.com/coil-kt/coil/issues/1246)) in the size system.
- Load images with `Size.ORIGINAL` if ContentScale is None.
- Fix applying `ImageView.load` builder argument first instead of last.
- Fix not combining HTTP headers if response is not modified.

## [2.0.0-rc03] - April 11, 2022

- Remove the `ScaleResolver` interface.
- Convert `Size` constructors to functions.
- Change `Dimension.Pixels`'s `toString` to only be its pixel value.
- Guard against a rare crash in `SystemCallbacks.onTrimMemory`.
- Update Coroutines to 1.6.1.

## [2.0.0-rc02] - March 20, 2022

- Revert `ImageRequest`'s default size to be the size of the current display instead of `Size.ORIGINAL`.
- Fix `DiskCache.Builder` being marked as experimental. Only `DiskCache`'s methods are experimental.
- Fix case where loading an image into an `ImageView` with one dimension as `WRAP_CONTENT` would load the image at its original size instead of fitting it into the bounded dimension.
- Remove component functions from `MemoryCache.Key`, `MemoryCache.Value`, and `Parameters.Entry`.

## [2.0.0-rc01] - March 2, 2022

Significant changes since `1.4.0`:

- The minimum supported API is now 21.
- Rework the Jetpack Compose integration.
    - `rememberImagePainter` has been renamed to `rememberAsyncImagePainter`.
    - Add support for `AsyncImage` and `SubcomposeAsyncImage`. Check out [the documentation](https://coil-kt.github.io/coil/compose/) for more info.
    - Deprecate `LocalImageLoader`. Check out the deprecation message for more info.
- Coil 2.0 has its own disk cache implementation and no longer relies on OkHttp for disk caching.
    - Use `ImageLoader.Builder.diskCache` and `DiskCache.Builder` to configure the disk cache.
    - You **should not** use OkHttp's `Cache` with Coil 2.0 as the cache can be corrupted if a thread is interrupted while writing to it.
    - `Cache-Control` and other cache headers are still supported - except `Vary` headers, as the cache only checks that the URLs match. Additionally, only responses with a response code in the range [200..300) are cached.
    - Existing disk caches will be cleared when upgrading to 2.0.
- `ImageRequest`'s default `Scale` is now `Scale.FIT`.
    - This was changed to make `ImageRequest.scale` consistent with other classes that have a default `Scale`.
    - Requests with an `ImageViewTarget` still have their `Scale` auto-detected.
- `ImageRequest`'s default size is now `Size.ORIGINAL`.
- Rework the image pipeline classes:
    - `Mapper`, `Fetcher`, and `Decoder` have been refactored to be more flexible.
    - `Fetcher.key` has been replaced with a new `Keyer` interface. `Keyer` creates the cache key from the input data.
    - Add `ImageSource`, which allows `Decoder`s to read `File`s directly using Okio's file system API.
- Disable generating runtime not-null assertions.
    - If you use Java, passing null as a not-null annotated parameter to a function will no longer throw a `NullPointerException` immediately. Kotlin's compile-time null safety guards against this happening.
    - This change allows the library's size to be smaller.
- `Size` is now composed of two `Dimension` values for its width and height. `Dimension` can either be a positive pixel value or `Dimension.Original`.
- `BitmapPool` and `PoolableViewTarget` have been removed from the library.
- `VideoFrameFileFetcher` and `VideoFrameUriFetcher` are removed from the library. Use `VideoFrameDecoder` instead, which supports all data sources.
- [`BlurTransformation`](https://github.com/coil-kt/coil/blob/845f39383f332428077c666e3567b954675ce248/coil-core/src/main/java/coil/transform/BlurTransformation.kt) and [`GrayscaleTransformation`](https://github.com/coil-kt/coil/blob/845f39383f332428077c666e3567b954675ce248/coil-core/src/main/java/coil/transform/GrayscaleTransformation.kt) are removed from the library. If you use them, you can copy their code into your project.
- Change `Transition.transition` to be a non-suspending function as it's no longer needed to suspend the transition until it completes.
- Add support for `bitmapFactoryMaxParallelism`, which restricts the maximum number of in-progress `BitmapFactory` operations. This value is 4 by default, which improves UI performance.
- Add support for `interceptorDispatcher`, `fetcherDispatcher`, `decoderDispatcher`, and `transformationDispatcher`.
- Add `GenericViewTarget`, which handles common `ViewTarget` logic.
- Add `ByteBuffer` to the default supported data types.
- `Disposable` has been refactored and exposes the underlying `ImageRequest`'s job.
- Rework the `MemoryCache` API.
- `ImageRequest.error` is now set on the `Target` if `ImageRequest.fallback` is null.
- `Transformation.key` is replaced with `Transformation.cacheKey`.
- Update Kotlin to 1.6.10.
- Update Compose to 1.1.1.
- Update OkHttp to 4.9.3.
- Update Okio to 3.0.0.

Changes since `2.0.0-alpha09`:

- Remove the `-Xjvm-default=all` compiler flag.
- Fix failing to load image if multiple requests with must-revalidate/e-tag are executed concurrently.
- Fix `DecodeUtils.isSvg` returning false if there is a new line character after the `<svg` tag.
- Make `LocalImageLoader.provides` deprecation message clearer.
- Update Compose to 1.1.1.
- Update `accompanist-drawablepainter` to 0.23.1.

## [2.0.0-alpha09] - February 16, 2022

- Fix `AsyncImage` creating invalid constraints. ([#1134](https://github.com/coil-kt/coil/pull/1134))
- Add `ContentScale` argument to `AsyncImagePainter`. ([#1144](https://github.com/coil-kt/coil/pull/1144))
    - This should be set to the same value that's set on `Image` to ensure that the image is loaded at the correct size.
- Add `ScaleResolver` to support lazily resolving the `Scale` for an `ImageRequest`. ([#1134](https://github.com/coil-kt/coil/pull/1134))
    - `ImageRequest.scale` should be replaced by `ImageRequest.scaleResolver.scale()`.
- Update Compose to 1.1.0.
- Update `accompanist-drawablepainter` to 0.23.0.
- Update `androidx.lifecycle` to 2.4.1.

## [2.0.0-alpha08] - February 7, 2022

- Update `DiskCache` and `ImageSource` to use to Okio's `FileSystem` API. ([#1115](https://github.com/coil-kt/coil/pull/1115))

## [2.0.0-alpha07] - January 30, 2022

- Significantly improve `AsyncImage` performance and split `AsyncImage` into `AsyncImage` and `SubcomposeAsyncImage`. ([#1048](https://github.com/coil-kt/coil/pull/1048))
    - `SubcomposeAsyncImage` provides `loading`/`success`/`error`/`content` slot APIs and uses subcomposition which has worse performance.
    -  `AsyncImage` provides `placeholder`/`error`/`fallback` arguments to overwrite the `Painter` that's drawn when loading or if the request is unsuccessful. `AsyncImage` does not use subcomposition and has much better performance than `SubcomposeAsyncImage`.
    - Remove `AsyncImagePainter.State` argument from `SubcomposeAsyncImage.content`. Use `painter.state` if needed.
    - Add `onLoading`/`onSuccess`/`onError` callbacks to both `AsyncImage` and `SubcomposeAsyncImage`.
- Deprecate `LocalImageLoader`. ([#1101](https://github.com/coil-kt/coil/pull/1101))
- Add support for `ImageRequest.tags`. ([#1066](https://github.com/coil-kt/coil/pull/1066))
- Move `isGif`, `isWebP`, `isAnimatedWebP`, `isHeif`, and `isAnimatedHeif` in `DecodeUtils` into coil-gif. Add `isSvg` to coil-svg. ([#1117](https://github.com/coil-kt/coil/pull/1117))
- Convert `FetchResult` and `DecodeResult` to be non-data classes. ([#1114](https://github.com/coil-kt/coil/pull/1114))
- Remove unused `DiskCache.Builder` context argument. ([#1099](https://github.com/coil-kt/coil/pull/1099))
- Fix scaling for bitmap resources with original size. ([#1072](https://github.com/coil-kt/coil/pull/1072))
- Fix failing to close `ImageDecoder` in `ImageDecoderDecoder`. ([#1109](https://github.com/coil-kt/coil/pull/1109))
- Fix incorrect scaling when converting a drawable to a bitmap. ([#1084](https://github.com/coil-kt/coil/pull/1084))
- Update Compose to 1.1.0-rc03.
- Update `accompanist-drawablepainter` to 0.22.1-rc.
- Update `androidx.appcompat:appcompat-resources` to 1.4.1.

## [2.0.0-alpha06] - December 24, 2021

- Add `ImageSource.Metadata` to support decoding from assets, resources, and content URIs without buffering or temporary files. ([#1060](https://github.com/coil-kt/coil/pull/1060))
- Delay executing the image request until `AsyncImage` has positive constraints. ([#1028](https://github.com/coil-kt/coil/pull/1028))
- Fix using `DefaultContent` for `AsyncImage` if `loading`, `success`, and `error` are all set. ([#1026](https://github.com/coil-kt/coil/pull/1026))
- Use androidx `LruCache` instead of the platform `LruCache`. ([#1047](https://github.com/coil-kt/coil/pull/1047))
- Update Kotlin to 1.6.10.
- Update Coroutines to 1.6.0.
- Update Compose to 1.1.0-rc01.
- Update `accompanist-drawablepainter` to 0.22.0-rc.
- Update `androidx.collection` to 1.2.0.

## [2.0.0-alpha05] - November 28, 2021

- **Important**: Refactor `Size` to support using the image's original size for either dimension.
    - `Size` is now composed of two `Dimension` values for its width and height. `Dimension` can either be a positive pixel value or `Dimension.Original`.
    - This change was made to better support unbounded width/height values (e.g. `wrap_content`, `Constraints.Infinity`) when one dimension is a fixed pixel value.
- Fix: Support inspection mode (preview) for `AsyncImage`.
- Fix: `SuccessResult.memoryCacheKey` should always be `null` if `imageLoader.memoryCache` is null.
- Convert `ImageLoader`, `SizeResolver`, and `ViewSizeResolver` constructor-like `invoke` functions to top level functions.
- Make `CrossfadeDrawable` start and end drawables public API.
- Mutate `ImageLoader` placeholder/error/fallback drawables.
- Add default arguments to `SuccessResult`'s constructor.
- Depend on `androidx.collection` instead of `androidx.collection-ktx`.
- Update OkHttp to 4.9.3.

## [2.0.0-alpha04] - November 22, 2021

- **New**: Add `AsyncImage` to `coil-compose`.
    - `AsyncImage` is a composable that executes an `ImageRequest` asynchronously and renders the result.
    - **`AsyncImage` is intended to replace `rememberImagePainter` for most use cases.**
    - Its API is not final and may change before the final 2.0 release.
    - It has a similar API to `Image` and supports the same arguments: `Alignment`, `ContentScale`, `alpha`, `ColorFilter`, and `FilterQuality`.
    - It supports overwriting what's drawn for each `AsyncImagePainter` state using the `content`, `loading`, `success`, and `error` arguments.
    - It fixes a number of design issues that `rememberImagePainter` has with resolving image size and scale.
    - Example usages:

```kotlin
// Only draw the image.
AsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = null, // Avoid `null` and set this to a localized string if possible.
)

// Draw the image with a circle crop, crossfade, and overwrite the `loading` state.
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data("https://example.com/image.jpg")
        .crossfade(true)
        .build(),
    contentDescription = null,
    modifier = Modifier
        .clip(CircleShape),
    loading = {
        CircularProgressIndicator()
    },
    contentScale = ContentScale.Crop
)

// Draw the image with a circle crop, crossfade, and overwrite all states.
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data("https://example.com/image.jpg")
        .crossfade(true)
        .build(),
    contentDescription = null,
    modifier = Modifier
        .clip(CircleShape),
    contentScale = ContentScale.Crop
) { state ->
    if (state is AsyncImagePainter.State.Loading) {
        CircularProgressIndicator()
    } else {
        AsyncImageContent() // Draws the image.
    }
}
```

- **Important**: Rename `ImagePainter` to `AsyncImagePainter` and `rememberImagePainter` to `rememberAsyncImagePainter`.
    - `ExecuteCallback` is no longer supported. To have the `AsyncImagePainter` skip waiting for `onDraw` to be called, set `ImageRequest.size(OriginalSize)` (or any size) instead.
    - Add an optional `FilterQuality` argument to `rememberAsyncImagePainter`.
- Use coroutines for cleanup operations in `DiskCache` and add `DiskCache.Builder.cleanupDispatcher`.
- Fix Compose preview for placeholder set using `ImageLoader.Builder.placeholder`.
- Mark `LocalImageLoader.current` with `@ReadOnlyComposable` to generate more efficient code.
- Update Compose to 1.1.0-beta03 and depend on `compose.foundation` instead of `compose.ui`.
- Update `androidx.appcompat-resources` to 1.4.0.

## [2.0.0-alpha03] - November 12, 2021

- Add ability to load music thumbnails on Android 29+. ([#967](https://github.com/coil-kt/coil/pull/967))
- Fix: Use `context.resources` to load resources for current package. ([#968](https://github.com/coil-kt/coil/pull/968))
- Fix: `clear` -> `dispose` replacement expression. ([#970](https://github.com/coil-kt/coil/pull/970))
- Update Compose to 1.0.5.
- Update `accompanist-drawablepainter` to 0.20.2.
- Update Okio to 3.0.0.
- Update `androidx.annotation` to 1.3.0.
- Update `androidx.core` to 1.7.0.
- Update `androidx.lifecycle` to 2.4.0.
    - Remove dependency on `lifecycle-common-java8` as it's been merged into `lifecycle-common`.

## [2.0.0-alpha02] - October 24, 2021

- Add a new `coil-bom` artifact which includes a [bill of materials](https://docs.gradle.org/7.2/userguide/platforms.html#sub:bom_import).
    - Importing `coil-bom` allows you to depend on other Coil artifacts without specifying a version.
- Fix failing to load an image when using `ExecuteCallback.Immediate`.
- Update Okio to 3.0.0-alpha.11.
    - This also resolves a compatibility issue with Okio 3.0.0-alpha.11.
- Update Kotlin to 1.5.31.
- Update Compose to 1.0.4.

## [2.0.0-alpha01] - October 11, 2021

Coil 2.0.0 is the next major iteration of the library and has new features, performance improvements, API improvements, and various bug fixes. This release may be binary/source incompatible with future alpha releases until the stable release of 2.0.0.

- **Important**: The minimum supported API is now 21.
- **Important**: Enable `-Xjvm-default=all`.
    - This generates Java 8 default methods instead of using Kotlin's default interface method support. Check out [this blog post](https://blog.jetbrains.com/kotlin/2020/07/kotlin-1-4-m3-generating-default-methods-in-interfaces/) for more information.
    - **You'll need to add `-Xjvm-default=all` or `-Xjvm-default=all-compatibility` to your build file as well.** See [here](https://coil-kt.github.io/coil/faq/#how-do-i-target-java-8) for how to do this.
- **Important**: Coil now has its own disk cache implementation and no longer relies on OkHttp for disk caching.
    - This change was made to:
        - Better support thread interruption while decoding images. This improves performance when image requests are started and stopped in quick succession.
        - Support exposing `ImageSource`s backed by `File`s. This avoids unnecessary copying when an Android API requires a `File` to decode (e.g. `MediaMetadataRetriever`).
        - Support reading from/writing to the disk cache files directly.
    - Use `ImageLoader.Builder.diskCache` and `DiskCache.Builder` to configure the disk cache.
    - You **should not** use OkHttp's `Cache` with Coil 2.0 as it can be corrupted if it's interrupted while writing to it.
    - `Cache-Control` and other cache headers are still supported - except `Vary` headers, as the cache only checks that the URLs match. Additionally, only responses with a response code in the range [200..300) are cached.
    - Support for cache headers can be enabled or disabled using `ImageLoader.Builder.respectCacheHeaders`.
    - Your existing disk cache will be cleared and rebuilt when upgrading to 2.0.
- **Important**: `ImageRequest`'s default `Scale` is now `Scale.FIT`
    - This was changed to make `ImageRequest.scale` consistent with other classes that have a default `Scale`.
    - Requests with an `ImageViewTarget` still have their scale autodetected.
- Significant changes to the image pipeline classes:
    - `Mapper`, `Fetcher`, and `Decoder` have been refactored to be more flexible.
    - `Fetcher.key` has been replaced with a new `Keyer` interface. `Keyer` creates the cache key from the input data.
    - Adds `ImageSource`, which allows `Decoder`s to decode `File`s directly.
- `BitmapPool` and `PoolableViewTarget` have been removed from the library. Bitmap pooling was removed because:
    - It's most effective on <= API 23 and has become less effective with newer Android releases.
    - Removing bitmap pooling allows Coil to use immutable bitmaps, which have performance benefits.
    - There's runtime overhead to manage the bitmap pool.
    - Bitmap pooling creates design restrictions on Coil's API as it requires tracking if a bitmap is eligible for pooling. Removing bitmap pooling allows Coil to expose the result `Drawable` in more places (e.g. `Listener`, `Disposable`). Additionally, this means Coil doesn't have to clear `ImageView`s, which has can cause [issues](https://github.com/coil-kt/coil/issues/650).
    - Bitmap pooling is [error-prone](https://github.com/coil-kt/coil/issues/546). Allocating a new bitmap is much safer than attempting to re-use a bitmap that could still be in use.
- `MemoryCache` has been refactored to be more flexible.
- Disable generating runtime not-null assertions.
    - If you use Java, passing null as a not-null annotated parameter to a function will no longer throw a `NullPointerException` immediately. If you use Kotlin, there is essentially no change.
    - This change allows the library's size to be smaller.
- `VideoFrameFileFetcher` and `VideoFrameUriFetcher` are removed from the library. Use `VideoFrameDecoder` instead, which supports all data sources.
- Adds support for `bitmapFactoryMaxParallelism`, which restricts the maximum number of in-progress `BitmapFactory` operations. This value is 4 by default, which improves UI performance.
- Adds support for `interceptorDispatcher`, `fetcherDispatcher`, `decoderDispatcher`, and `transformationDispatcher`.
- `Disposable` has been refactored and exposes the underlying `ImageRequest`'s job.
- Change `Transition.transition` to be a non-suspending function as it's no longer needed to suspend the transition until it completes.
- Add `GenericViewTarget`, which handles common `ViewTarget` logic.
- [`BlurTransformation`](https://github.com/coil-kt/coil/blob/845f39383f332428077c666e3567b954675ce248/coil-core/src/main/java/coil/transform/BlurTransformation.kt) and [`GrayscaleTransformation`](https://github.com/coil-kt/coil/blob/845f39383f332428077c666e3567b954675ce248/coil-core/src/main/java/coil/transform/GrayscaleTransformation.kt) are removed from the library.
    - If you use them, you can copy their code into your project.
- `ImageRequest.error` is now set on the `Target` if `ImageRequest.fallback` is null.
- `Transformation.key` is replaced with `Transformation.cacheKey`.
- `ImageRequest.Listener` returns `SuccessResult`/`ErrorResult` in `onSuccess` and `onError` respectively.
- Add `ByteBuffer`s to the default supported data types.
- Remove `toString` implementations from several classes.
- Update OkHttp to 4.9.2.
- Update Okio to 3.0.0-alpha.10.

## [1.4.0] - October 6, 2021

- **New**: Add `ImageResult` to `ImagePainter.State.Success` and `ImagePainter.State.Error`. ([#887](https://github.com/coil-kt/coil/pull/887))
    - This is a binary incompatible change to the signatures of `ImagePainter.State.Success` and `ImagePainter.State.Error`, however these APIs are marked as experimental.
- Only execute `CrossfadeTransition` if `View.isShown` is `true`. Previously it would only check `View.isVisible`. ([#898](https://github.com/coil-kt/coil/pull/898))
- Fix potential memory cache miss if scaling multiplier is slightly less than 1 due to a rounding issue. ([#899](https://github.com/coil-kt/coil/pull/899))
- Make non-inlined `ComponentRegistry` methods public. ([#925](https://github.com/coil-kt/coil/pull/925))
- Depend on `accompanist-drawablepainter` and remove Coil's custom `DrawablePainter` implementation. ([#845](https://github.com/coil-kt/coil/pull/845))
- Remove use of a Java 8 method to guard against desugaring issue. ([#924](https://github.com/coil-kt/coil/pull/924))
- Promote `ImagePainter.ExecuteCallback` to stable API. ([#927](https://github.com/coil-kt/coil/pull/927))
- Update compileSdk to 31.
- Update Kotlin to 1.5.30.
- Update Coroutines to 1.5.2.
- Update Compose to 1.0.3.

## [1.3.2] - August 4, 2021

- `coil-compose` now depends on `compose.ui` instead of `compose.foundation`.
    - `compose.ui` is a smaller dependency as it's a subset of `compose.foundation`.
- Update Jetpack Compose to 1.0.1.
- Update Kotlin to 1.5.21.
- Update Coroutines to 1.5.1.
- Update `androidx.exifinterface:exifinterface` to 1.3.3.

## [1.3.1] - July 28, 2021

- Update Jetpack Compose to `1.0.0`. Huge congrats to the Compose team on the [stable release](https://android-developers.googleblog.com/2021/07/jetpack-compose-announcement.html)!
- Update `androidx.appcompat:appcompat-resources` to 1.3.1.

## [1.3.0] - July 10, 2021

- **New**: Add support for [Jetpack Compose](https://developer.android.com/jetpack/compose). It's based on [Accompanist](https://github.com/google/accompanist/)'s Coil integration, but has a number of changes. Check out [the docs](https://coil-kt.github.io/coil/compose/) for more info.
- Add `allowConversionToBitmap` to enable/disable the automatic bitmap conversion for `Transformation`s. ([#775](https://github.com/coil-kt/coil/pull/775))
- Add `enforceMinimumFrameDelay` to `ImageDecoderDecoder` and `GifDecoder` to enable rewriting a GIF's frame delay if it's below a threshold. ([#783](https://github.com/coil-kt/coil/pull/783))
    - This is disabled by default, but will be enabled by default in a future release.
- Add support for enabling/disabling an `ImageLoader`'s internal network observer. ([#741](https://github.com/coil-kt/coil/pull/741))
- Fix the density of bitmaps decoded by `BitmapFactoryDecoder`. ([#776](https://github.com/coil-kt/coil/pull/776))
- Fix Licensee not finding Coil's licence url. ([#774](https://github.com/coil-kt/coil/pull/774))
- Update `androidx.core:core-ktx` to 1.6.0.

## [1.2.2] - June 4, 2021

- Fix race condition while converting a drawable with shared state to a bitmap. ([#771](https://github.com/coil-kt/coil/pull/771))
- Fix `ImageLoader.Builder.fallback` setting the `error` drawable instead of the `fallback` drawable.
- Fix incorrect data source returned by `ResourceUriFetcher`. ([#770](https://github.com/coil-kt/coil/pull/770))
- Fix log check for no available file descriptors on API 26 and 27.
- Fix incorrect version check for platform vector drawable support. ([#751](https://github.com/coil-kt/coil/pull/751))
- Update Kotlin (1.5.10).
- Update Coroutines (1.5.0).
- Update `androidx.appcompat:appcompat-resources` to 1.3.0.
- Update `androidx.core:core-ktx` to 1.5.0.

## [1.2.1] - April 27, 2021

- Fix `VideoFrameUriFetcher` attempting to handle http/https URIs. ([#734](https://github.com/coil-kt/coil/pull/734)

## [1.2.0] - April 12, 2021

- **Important**: Use an SVG's view bounds to calculate its aspect ratio in `SvgDecoder`. ([#688](https://github.com/coil-kt/coil/pull/688))
    - Previously, `SvgDecoder` used an SVG's `width`/`height` elements to determine its aspect ratio, however this doesn't correctly follow the SVG specification.
    - To revert to the old behaviour set `useViewBoundsAsIntrinsicSize = false` when constructing your `SvgDecoder`.
- **New**: Add `VideoFrameDecoder` to support decoding video frames from any source. ([#689](https://github.com/coil-kt/coil/pull/689))
- **New**: Support automatic SVG detection using the source's contents instead of just the MIME type. ([#654](https://github.com/coil-kt/coil/pull/654))
- **New**: Support sharing resources using `ImageLoader.newBuilder()`. ([#653](https://github.com/coil-kt/coil/pull/653))
    - Importantly, this enables sharing memory caches between `ImageLoader` instances.
- **New**: Add support for animated image transformations using `AnimatedTransformation`. ([#659](https://github.com/coil-kt/coil/pull/659))
- **New**: Add support for start/end callbacks for animated drawables. ([#676](https://github.com/coil-kt/coil/pull/676))

---

- Fix parsing EXIF data for HEIF/HEIC files. ([#664](https://github.com/coil-kt/coil/pull/664))
- Fix not using the `EmptyBitmapPool` implementation if bitmap pooling is disabled. ([#638](https://github.com/coil-kt/coil/pull/638))
    - Without this fix bitmap pooling was still disabled properly, however it used a more heavyweight `BitmapPool` implementation.
- Fix case where `MovieDrawable.getOpacity` would incorrectly return transparent. ([#682](https://github.com/coil-kt/coil/pull/682))
- Guard against the default temporary directory not existing. ([#683](https://github.com/coil-kt/coil/pull/683))

---

- Build using the JVM IR backend. ([#670](https://github.com/coil-kt/coil/pull/670))
- Update Kotlin (1.4.32).
- Update Coroutines (1.4.3).
- Update OkHttp (3.12.13).
- Update `androidx.lifecycle:lifecycle-common-java8` to 2.3.1.

## [1.1.1] - January 11, 2021

- Fix a case where `ViewSizeResolver.size` could throw an `IllegalStateException` due to resuming a coroutine more than once.
- Fix `HttpFetcher` blocking forever if called from the main thread.
    - Requests that are forced to execute on the main thread using `ImageRequest.dispatcher(Dispatchers.Main.immediate)` will fail with a `NetworkOnMainThreadException` unless `ImageRequest.networkCachePolicy` is set to `CachePolicy.DISABLED` or `CachePolicy.WRITE_ONLY`.
- Rotate video frames from `VideoFrameFetcher` if the video has rotation metadata.
- Update Kotlin (1.4.21).
- Update Coroutines (1.4.2).
- Update Okio (2.10.0).
- Update `androidx.exifinterface:exifinterface` (1.3.2).

## [1.1.0] - November 24, 2020

- **Important**: Change the `CENTER` and `MATRIX` `ImageView` scale types to resolve to `OriginalSize`. ([#587](https://github.com/coil-kt/coil/pull/587))
    - This change only affects the implicit size resolution algorithm when the request's size isn't specified explicitly.
    - This change was made to ensure that the visual result of an image request is consistent with `ImageView.setImageResource`/`ImageView.setImageURI`. To revert to the old behaviour set a `ViewSizeResolver` when constructing your request.
- **Important**: Return the display size from `ViewSizeResolver` if the view's layout param is `WRAP_CONTENT`. ([#562](https://github.com/coil-kt/coil/pull/562))
    - Previously, we would only return the display size if the view has been fully laid out. This change makes the typical behaviour more consistent and intuitive.
- Add the ability to control alpha pre-multiplication. ([#569](https://github.com/coil-kt/coil/pull/569))
- Support preferring exact intrinsic size in `CrossfadeDrawable`. ([#585](https://github.com/coil-kt/coil/pull/585))
- Check for the full GIF header including version. ([#564](https://github.com/coil-kt/coil/pull/564))
- Add an empty bitmap pool implementation. ([#561](https://github.com/coil-kt/coil/pull/561))
- Make `EventListener.Factory` a functional interface. ([#575](https://github.com/coil-kt/coil/pull/575))
- Stabilize `EventListener`. ([#574](https://github.com/coil-kt/coil/pull/574))
- Add `String` overload for `ImageRequest.Builder.placeholderMemoryCacheKey`.
- Add `@JvmOverloads` to the `ViewSizeResolver` constructor.
- Fix: Mutate start/end drawables in `CrossfadeDrawable`. ([#572](https://github.com/coil-kt/coil/pull/572))
- Fix: Fix GIF not playing on second load. ([#577](https://github.com/coil-kt/coil/pull/534))
- Update Kotlin (1.4.20) and migrate to the `kotlin-parcelize` plugin.
- Update Coroutines (1.4.1).

## [1.0.0] - October 22, 2020

Changes since `0.13.0`:
- Add `Context.imageLoader` extension function. ([#534](https://github.com/coil-kt/coil/pull/534))
- Add `ImageLoader.executeBlocking` extension function. ([#537](https://github.com/coil-kt/coil/pull/537))
- Don't shutdown previous singleton image loader if replaced. ([#533](https://github.com/coil-kt/coil/pull/533))

Changes since `1.0.0-rc3`:
- Fix: Guard against missing/invalid ActivityManager. ([#541](https://github.com/coil-kt/coil/pull/541))
- Fix: Allow OkHttp to cache unsuccessful responses. ([#551](https://github.com/coil-kt/coil/pull/551))
- Update Kotlin to 1.4.10.
- Update Okio to 2.9.0.
- Update `androidx.exifinterface:exifinterface` to 1.3.1.

## [1.0.0-rc3] - September 21, 2020

- Revert using the [`-Xjvm-default=all`](https://blog.jetbrains.com/kotlin/2020/07/kotlin-1-4-m3-generating-default-methods-in-interfaces/) compiler flag due to instability.
    - **This is a source compatible, but binary incompatible change from previous release candidate versions.**
- Add `Context.imageLoader` extension function. ([#534](https://github.com/coil-kt/coil/pull/534))
- Add `ImageLoader.executeBlocking` extension function. ([#537](https://github.com/coil-kt/coil/pull/537))
- Don't shutdown previous singleton image loader if replaced. ([#533](https://github.com/coil-kt/coil/pull/533))
- Update AndroidX dependencies:
    - `androidx.exifinterface:exifinterface` -> 1.3.0

## [1.0.0-rc2] - September 3, 2020

- **This release requires Kotlin 1.4.0 or above.**
- All the changes present in [0.13.0](#0130---september-3-2020).
- Depend on the base Kotlin `stdlib` instead of `stdlib-jdk8`.

## [0.13.0] - September 3, 2020

- **Important**: Launch the Interceptor chain on the main thread by default. ([#513](https://github.com/coil-kt/coil/pull/513))
    - This largely restores the behaviour from `0.11.0` and below where the memory cache would be checked synchronously on the main thread.
    - To revert to using the same behaviour as `0.12.0` where the memory cache is checked on `ImageRequest.dispatcher`, set `ImageLoader.Builder.launchInterceptorChainOnMainThread(false)`.
    - See [`launchInterceptorChainOnMainThread`](https://coil-kt.github.io/coil/api/coil-core/coil3/-image-loader/-builder/launch-interceptor-chain-on-main-thread/) for more information.

---

- Fix: Fix potential memory leak if request is started on a `ViewTarget` in a detached fragment. ([#518](https://github.com/coil-kt/coil/pull/518))
- Fix: Use `ImageRequest.context` to load resource URIs. ([#517](https://github.com/coil-kt/coil/pull/517))
- Fix: Fix race condition that could cause subsequent requests to not be saved to the disk cache. ([#510](https://github.com/coil-kt/coil/pull/510))
- Fix: Use `blockCountLong` and `blockSizeLong` on API 18.

---

- Make `ImageLoaderFactory` a fun interface.
- Add `ImageLoader.Builder.addLastModifiedToFileCacheKey` which allows you to enable/disable adding the last modified timestamp to the memory cache key for an image loaded from a `File`.

---

- Update Kotlin to 1.4.0.
- Update Coroutines to 1.3.9.
- Update Okio to 2.8.0.

## [1.0.0-rc1] - August 18, 2020

- **This release requires Kotlin 1.4.0 or above.**
- Update Kotlin to 1.4.0 and enable [`-Xjvm-default=all`](https://blog.jetbrains.com/kotlin/2020/07/kotlin-1-4-m3-generating-default-methods-in-interfaces/).
    - **[See here](https://coil-kt.github.io/coil/faq/#how-do-i-target-java-8) for how to enable `-Xjvm-default=all` in your build file.**
    - This generates Java 8 default methods for default Kotlin interface methods.
- Remove all existing deprecated methods in 0.12.0.
- Update Coroutines to 1.3.9.

## [0.12.0] - August 18, 2020

- **Breaking**: `LoadRequest` and `GetRequest` have been replaced with `ImageRequest`:
    - `ImageLoader.execute(LoadRequest)` -> `ImageLoader.enqueue(ImageRequest)`
    - `ImageLoader.execute(GetRequest)` -> `ImageLoader.execute(ImageRequest)`
    - `ImageRequest` implements `equals`/`hashCode`.
- **Breaking**: A number of classes were renamed and/or changed package:
    - `coil.request.RequestResult` -> `coil.request.ImageResult`
    - `coil.request.RequestDisposable` -> `coil.request.Disposable`
    - `coil.bitmappool.BitmapPool` -> `coil.bitmap.BitmapPool`
    - `coil.DefaultRequestOptions` -> `coil.request.DefaultRequestOptions`
- **Breaking**: [`SparseIntArraySet`](https://github.com/coil-kt/coil/blob/f52addd039f0195b66f93cb0f1cad59b0832f784/coil-core/src/main/java/coil/collection/SparseIntArraySet.kt) has been removed from the public API.
- **Breaking**: `TransitionTarget` no longer implements `ViewTarget`.
- **Breaking**: `ImageRequest.Listener.onSuccess`'s signature has changed to return an `ImageResult.Metadata` instead of just a `DataSource`.
- **Breaking**: Remove support for `LoadRequest.aliasKeys`. This API is better handled with direct read/write access to the memory cache.

---

- **Important**: Values in the memory cache are no longer resolved synchronously (if called from the main thread).
    - This change was also necessary to support executing `Interceptor`s on a background dispatcher.
    - This change also moves more work off the main thread, improving performance.
- **Important**: `Mappers` are now executed on a background dispatcher. As a side effect, automatic bitmap sampling is no longer **automatically** supported. To achieve the same effect, use the `MemoryCache.Key` of a previous request as the `placeholderMemoryCacheKey` of the subsequent request. [See here for an example](https://coil-kt.github.io/coil/recipes/#using-a-memory-cache-key-as-a-placeholder).
    - The `placeholderMemoryCacheKey` API offers more freedom as you can "link" two image requests with different data (e.g. different URLs for small/large images).
- **Important**: Coil's `ImageView` extension functions have been moved from the `coil.api` package to the `coil` package.
    - Use find + replace to refactor `import coil.api.load` -> `import coil.load`. Unfortunately, it's not possible to use Kotlin's `ReplaceWith` functionality to replace imports.
- **Important**: Use standard crossfade if drawables are not the same image.
- **Important**: Prefer immutable bitmaps on API 24+.
- **Important**: `MeasuredMapper` has been deprecated in favour of the new `Interceptor` interface. See [here](https://gist.github.com/colinrtwhite/90267704091467451e46b21b95154299) for an example of how to convert a `MeasuredMapper` into an `Interceptor`.
    - `Interceptor` is a much less restrictive API that allows for a wider range of custom logic.
- **Important**: `ImageRequest.data` is now not null. If you create an `ImageRequest` without setting its data it will return `NullRequestData` as its data.

---

- **New**: Add support for direct read/write access to an `ImageLoader`'s `MemoryCache`. See [the docs](https://coil-kt.github.io/coil/getting_started/#memory-cache) for more information.
- **New**: Add support for `Interceptor`s. See [the docs](https://coil-kt.github.io/coil/image_pipeline/#interceptors) for more information. Coil's `Interceptor` design is heavily inspired by [OkHttp](https://github.com/square/okhttp)'s!
- **New**: Add the ability to enable/disable bitmap pooling using `ImageLoader.Builder.bitmapPoolingEnabled`.
    - Bitmap pooling is most effective on API 23 and below, but may still be benificial on API 24 and up (by eagerly calling `Bitmap.recycle`).
- **New**: Support thread interruption while decoding.

---

- Fix parsing multiple segments in content-type header.
- Rework bitmap reference counting to be more robust.
- Fix WebP decoding on API < 19 devices.
- Expose FetchResult and DecodeResult in the EventListener API.

---

- Compile with SDK 30.
- Update Coroutines to 1.3.8.
- Update OkHttp to 3.12.12.
- Update Okio to 2.7.0.
- Update AndroidX dependencies:
    - `androidx.appcompat:appcompat-resources` -> 1.2.0
    - `androidx.core:core-ktx` -> 1.3.1

## [0.11.0] - May 14, 2020

- **Breaking**: **This version removes all existing deprecated functions.**
    - This enables removing Coil's `ContentProvider` so it doesn't run any code at app startup.
- **Breaking**: Convert `SparseIntArraySet.size` to a val. ([#380](https://github.com/coil-kt/coil/pull/380))
- **Breaking**: Move `Parameters.count()` to an extension function. ([#403](https://github.com/coil-kt/coil/pull/403))
- **Breaking**: Make `BitmapPool.maxSize` an Int. ([#404](https://github.com/coil-kt/coil/pull/404))

---

- **Important**: Make `ImageLoader.shutdown()` optional (similar to `OkHttpClient`). ([#385](https://github.com/coil-kt/coil/pull/385))

---

- Fix: Fix AGP 4.1 compatibility. ([#386](https://github.com/coil-kt/coil/pull/386))
- Fix: Fix measuring GONE views. ([#397](https://github.com/coil-kt/coil/pull/397))

---

- Reduce the default memory cache size to 20%. ([#390](https://github.com/coil-kt/coil/pull/390))
    - To restore the existing behaviour set `ImageLoaderBuilder.availableMemoryPercentage(0.25)` when creating your `ImageLoader`.
- Update Coroutines to 1.3.6.
- Update OkHttp to 3.12.11.

## [0.10.1] - April 26, 2020

- Fix OOM when decoding large PNGs on API 23 and below. ([#372](https://github.com/coil-kt/coil/pull/372)).
    - This disables decoding EXIF orientation for PNG files. PNG EXIF orientation is very rarely used and reading PNG EXIF data (even if it's empty) requires buffering the entire file into memory, which is bad for performance.
- Minor Java compatibility improvements to `SparseIntArraySet`.

---

- Update Okio to 2.6.0.

## [0.10.0] - April 20, 2020

### Highlights

- **This version deprecates most of the DSL API in favour of using the builders directly.** Here's what the change looks like:

    ```kotlin
    // 0.9.5 (old)
    val imageLoader = ImageLoader(context) {
        bitmapPoolPercentage(0.5)
        crossfade(true)
    }

    val disposable = imageLoader.load(context, "https://example.com/image.jpg") {
        target(imageView)
    }

    val drawable = imageLoader.get("https://example.com/image.jpg") {
        size(512, 512)
    }

    // 0.10.0 (new)
    val imageLoader = ImageLoader.Builder(context)
        .bitmapPoolPercentage(0.5)
        .crossfade(true)
        .build()

    val request = LoadRequest.Builder(context)
        .data("https://example.com/image.jpg")
        .target(imageView)
        .build()
    val disposable = imageLoader.execute(request)

    val request = GetRequest.Builder(context)
        .data("https://example.com/image.jpg")
        .size(512, 512)
        .build()
    val drawable = imageLoader.execute(request).drawable
    ```

    - If you're using the `io.coil-kt:coil` artifact, you can call `Coil.execute(request)` to execute the request with the singleton `ImageLoader`.

- **`ImageLoader`s now have a weak reference memory cache** that tracks weak references to images once they're evicted from the strong reference memory cache.
    - This means an image will always be returned from an `ImageLoader`'s memory cache if there's still a strong reference to it.
    - Generally, this should make the memory cache much more predictable and increase its hit rate.
    - This behaviour can be enabled/disabled with `ImageLoaderBuilder.trackWeakReferences`.

- Add a new artifact, **`io.coil-kt:coil-video`**, to decode specific frames from a video file. [Read more here](https://coil-kt.github.io/coil/videos/).

- Add a new [EventListener](https://github.com/coil-kt/coil/blob/main/coil-core/src/main/java/coil/EventListener.kt) API for tracking metrics.

- Add [ImageLoaderFactory](https://github.com/coil-kt/coil/blob/main/coil/src/main/java/coil/ImageLoaderFactory.kt) which can be implemented by your `Application` to simplify singleton initialization.

---

### Full Release Notes

- **Important**: Deprecate DSL syntax in favour of builder syntax. ([#267](https://github.com/coil-kt/coil/pull/267))
- **Important**: Deprecate `Coil` and `ImageLoader` extension functions. ([#322](https://github.com/coil-kt/coil/pull/322))
- **Breaking**: Return sealed `RequestResult` type from `ImageLoader.execute(GetRequest)`. ([#349](https://github.com/coil-kt/coil/pull/349))
- **Breaking**: Rename `ExperimentalCoil` to `ExperimentalCoilApi`. Migrate from `@Experimental` to `@RequiresOptIn`. ([#306](https://github.com/coil-kt/coil/pull/306))
- **Breaking**: Replace `CoilLogger` with `Logger` interface. ([#316](https://github.com/coil-kt/coil/pull/316))
- **Breaking**: Rename destWidth/destHeight to dstWidth/dstHeight. ([#275](https://github.com/coil-kt/coil/pull/275))
- **Breaking**: Re-arrange `MovieDrawable`'s constructor params. ([#272](https://github.com/coil-kt/coil/pull/272))
- **Breaking**: `Request.Listener`'s methods now receive the full `Request` object instead of just its data.
- **Breaking**: `GetRequestBuilder` now requires a `Context` in its constructor.
- **Breaking**: Several properties on `Request` are now nullable.
- **Behaviour change**: Include parameter values in the cache key by default. ([#319](https://github.com/coil-kt/coil/pull/319))
- **Behaviour change**: Slightly adjust `Request.Listener.onStart()` timing to be called immediately after `Target.onStart()`. ([#348](https://github.com/coil-kt/coil/pull/348))

---

- **New**: Add `WeakMemoryCache` implementation. ([#295](https://github.com/coil-kt/coil/pull/295))
- **New**: Add `coil-video` to support decoding video frames. ([#122](https://github.com/coil-kt/coil/pull/122))
- **New**: Introduce [`EventListener`](https://github.com/coil-kt/coil/blob/main/coil-core/src/main/java/coil/EventListener.kt). ([#314](https://github.com/coil-kt/coil/pull/314))
- **New**: Introduce [`ImageLoaderFactory`](https://github.com/coil-kt/coil/blob/main/coil/src/main/java/coil/ImageLoaderFactory.kt). ([#311](https://github.com/coil-kt/coil/pull/311))
- **New**: Support animated HEIF image sequences on Android 11. ([#297](https://github.com/coil-kt/coil/pull/297))
- **New**: Improve Java compatibility. ([#262](https://github.com/coil-kt/coil/pull/262))
- **New**: Support setting a default `CachePolicy`. ([#307](https://github.com/coil-kt/coil/pull/307))
- **New**: Support setting a default `Bitmap.Config`. ([#342](https://github.com/coil-kt/coil/pull/342))
- **New**: Add `ImageLoader.invalidate(key)` to clear a single memory cache item ([#55](https://github.com/coil-kt/coil/pull/55))
- **New**: Add debug logs to explain why a cached image is not reused. ([#346](https://github.com/coil-kt/coil/pull/346))
- **New**: Support `error` and `fallback` drawables for get requests.

---

- Fix: Fix memory cache miss when Transformation reduces input bitmap's size. ([#357](https://github.com/coil-kt/coil/pull/357))
- Fix: Ensure radius is below RenderScript max in BlurTransformation. ([#291](https://github.com/coil-kt/coil/pull/291))
- Fix: Fix decoding high colour depth images. ([#358](https://github.com/coil-kt/coil/pull/358))
- Fix: Disable `ImageDecoderDecoder` crash work-around on Android 11 and above. ([#298](https://github.com/coil-kt/coil/pull/298))
- Fix: Fix failing to read EXIF data on pre-API 23. ([#331](https://github.com/coil-kt/coil/pull/331))
- Fix: Fix incompatibility with Android R SDK. ([#337](https://github.com/coil-kt/coil/pull/337))
- Fix: Only enable inexact size if `ImageView` has a matching `SizeResolver`. ([#344](https://github.com/coil-kt/coil/pull/344))
- Fix: Allow cached images to be at most one pixel off requested size. ([#360](https://github.com/coil-kt/coil/pull/360))
- Fix: Skip crossfade transition if view is not visible. ([#361](https://github.com/coil-kt/coil/pull/361))

---

- Deprecate `CoilContentProvider`. ([#293](https://github.com/coil-kt/coil/pull/293))
- Annotate several `ImageLoader` methods with `@MainThread`.
- Avoid creating a `LifecycleCoroutineDispatcher` if the lifecycle is currently started. ([#356](https://github.com/coil-kt/coil/pull/356))
- Use full package name for `OriginalSize.toString()`.
- Preallocate when decoding software bitmap. ([#354](https://github.com/coil-kt/coil/pull/354))

---

- Update Kotlin to 1.3.72.
- Update Coroutines to 1.3.5.
- Update OkHttp to 3.12.10.
- Update Okio to 2.5.0.
- Update AndroidX dependencies:
    - `androidx.exifinterface:exifinterface` -> 1.2.0

## [0.9.5] - February 6, 2020

- Fix: Ensure a view is attached before checking if it is hardware accelerated. This fixes a case where requesting a hardware bitmap could miss the memory cache.

---

- Update AndroidX dependencies:
    - `androidx.core:core-ktx` -> 1.2.0

## [0.9.4] - February 3, 2020

- Fix: Respect aspect ratio when downsampling in ImageDecoderDecoder. Thanks @zhanghai.

---

- Previously bitmaps would be returned from the memory cache as long as their config was greater than or equal to the config specified in the request. For example, if you requested an `ARGB_8888` bitmap, it would be possible to have a `RGBA_F16` bitmap returned to you from the memory cache. Now, the cached config and the requested config must be equal.
- Make `scale` and `durationMillis` public in `CrossfadeDrawable` and `CrossfadeTransition`.

## [0.9.3] - February 1, 2020

- Fix: Translate child drawable inside `ScaleDrawable` to ensure it is centered.
- Fix: Fix case where GIFs and SVGs would not fill bounds completely.

---

- Defer calling `HttpUrl.get()` to background thread.
- Improve BitmapFactory null bitmap error message.
- Add 3 devices to hardware bitmap blacklist. ([#264](https://github.com/coil-kt/coil/pull/264))

---

- Update AndroidX dependencies:
    - `androidx.lifecycle:lifecycle-common-java8` -> 2.2.0

## [0.9.2] - January 19, 2020

- Fix: Fix decoding GIFs on pre-API 19. Thanks @mario.
- Fix: Fix rasterized vector drawables not being marked as sampled.
- Fix: Throw exception if Movie dimensions are <= 0.
- Fix: Fix CrossfadeTransition not being resumed for a memory cache event.
- Fix: Prevent returning hardware bitmaps to all target methods if disallowed.
- Fix: Fix MovieDrawable not positioning itself in the center of its bounds.

---

- Remove automatic scaling from CrossfadeDrawable.
- Make `BitmapPool.trimMemory` public.
- Wrap AnimatedImageDrawable in a ScaleDrawable to ensure it fills its bounds.
- Add @JvmOverloads to RequestBuilder.setParameter.
- Set an SVG's view box to its size if the view box is not set.
- Pass state and level changes to CrossfadeDrawable children.

---

- Update OkHttp to 3.12.8.

## [0.9.1] - December 30, 2019

- Fix: Fix crash when calling `LoadRequestBuilder.crossfade(false)`.

## [0.9.0] - December 30, 2019

- **Breaking**: `Transformation.transform` now includes a `Size` parameter. This is to support transformations that change the size of the output `Bitmap` based on the size of the `Target`. Requests with transformations are now also exempt from [image sampling](https://coil-kt.github.io/coil/getting_started/#image-sampling).
- **Breaking**: `Transformation`s are now applied to any type of `Drawable`. Before, `Transformation`s would be skipped if the input `Drawable` was not a `BitmapDrawable`. Now, `Drawable`s are rendered to a `Bitmap` before applying the `Transformation`s.
- **Breaking**: Passing `null` data to `ImageLoader.load` is now treated as an error and calls `Target.onError` and `Request.Listener.onError` with a `NullRequestDataException`. This change was made to support setting a `fallback` drawable if data is `null`. Previously the request was silently ignored.
- **Breaking**: `RequestDisposable.isDisposed` is now a `val`.

---

- **New**: Support for custom transitions. [See here for more info](https://coil-kt.github.io/coil/transitions/). Transitions are marked as experimental as the API is incubating.
- **New**: Add `RequestDisposable.await` to support suspending while a `LoadRequest` is in progress.
- **New**: Support setting a `fallback` drawable when request data is null.
- **New**: Add `Precision`. This makes the size of the output `Drawable` exact while enabling scaling optimizations for targets that support scaling (e.g. `ImageViewTarget`). See [its documentation](https://github.com/coil-kt/coil/blob/main/coil-core/src/main/java/coil/size/Precision.kt) for more information.
- **New**: Add `RequestBuilder.aliasKeys` to support matching multiple cache keys.

---

- Fix: Make RequestDisposable thread safe.
- Fix: `RoundedCornersTransformation` now crops to the size of the target then rounds the corners.
- Fix: `CircleCropTransformation` now crops from the center.
- Fix: Add several devices to the [hardware bitmap blacklist](https://github.com/coil-kt/coil/blob/main/coil-core/src/main/java/coil/memory/HardwareBitmapService.kt).
- Fix: Preserve aspect ratio when converting a Drawable to a Bitmap.
- Fix: Fix possible memory cache miss with `Scale.FIT`.
- Fix: Ensure Parameters iteration order is deterministic.
- Fix: Defensive copy when creating Parameters and ComponentRegistry.
- Fix: Ensure RealBitmapPool's maxSize >= 0.
- Fix: Show the start drawable if CrossfadeDrawable is not animating or done.
- Fix: Adjust CrossfadeDrawable to account for children with undefined intrinsic size.
- Fix: Fix `MovieDrawable` not scaling properly.

---

- Update Kotlin to 1.3.61.
- Update Kotlin Coroutines to 1.3.3.
- Update Okio to 2.4.3.
- Update AndroidX dependencies:
    - `androidx.exifinterface:exifinterface` -> 1.1.0

## [0.8.0] - October 22, 2019

- **Breaking**: `SvgDrawable` has been removed. Instead, SVGs are now prerendered to `BitmapDrawable`s by `SvgDecoder`. This makes SVGs **significantly less expensive to render on the main thread**. Also `SvgDecoder` now requires a `Context` in its constructor.
- **Breaking**: `SparseIntArraySet` extension functions have moved to the `coil.extension` package.

---

- **New**: Support setting per-request network headers. [See here for more info](https://github.com/coil-kt/coil/pull/120).
- **New**: Add new `Parameters` API to support passing custom data through the image pipeline.
- **New**: Support individual corner radii in RoundedCornersTransformation. Thanks @khatv911.
- **New**: Add `ImageView.clear()` to support proactively freeing resources.
- **New**: Support loading resources from other packages.
- **New**: Add `subtractPadding` attribute to ViewSizeResolver to enable/disable subtracting a view's padding when measuring.
- **New**: Improve HttpUrlFetcher MIME type detection.
- **New**: Add Animatable2Compat support to MovieDrawable and CrossfadeDrawable.
- **New**: Add `RequestBuilder<*>.repeatCount` to set the repeat count for a GIF.
- **New**: Add BitmapPool creation to the public API.
- **New**: Annotate Request.Listener methods with `@MainThread`.

---

- Fix: Make CoilContentProvider visible for testing.
- Fix: Include night mode in the resource cache key.
- Fix: Work around ImageDecoder native crash by temporarily writing the source to disk.
- Fix: Correctly handle contact display photo uris.
- Fix: Pass tint to CrossfadeDrawable's children.
- Fix: Fix several instances of not closing sources.
- Fix: Add a blacklist of devices with broken/incomplete hardware bitmap implementations.

---

- Compile against SDK 29.
- Update Kotlin Coroutines to 1.3.2.
- Update OkHttp to 3.12.6.
- Update Okio to 2.4.1.
- Change `appcompat-resources` from `compileOnly` to `implementation` for `coil-base`.

## [0.7.0] - September 8, 2019
- **Breaking**: `ImageLoaderBuilder.okHttpClient(OkHttpClient.Builder.() -> Unit)` is now `ImageLoaderBuilder.okHttpClient(() -> OkHttpClient)`. The initializer is also now called lazily on a background thread. **If you set a custom `OkHttpClient` you must set `OkHttpClient.cache` to enable disk caching.** If you don't set a custom `OkHttpClient`, Coil will create the default `OkHttpClient` which has disk caching enabled. The default Coil cache can be created using `CoilUtils.createDefaultCache(context)`. e.g.:

```kotlin
val imageLoader = ImageLoader(context) {
    okHttpClient {
        OkHttpClient.Builder()
            .cache(CoilUtils.createDefaultCache(context))
            .build()
    }
}
```

- **Breaking**: `Fetcher.key` no longer has a default implementation.
- **Breaking**: Previously, only the first applicable `Mapper` would be called. Now, all applicable `Mapper`s will be called. No API changes.
- **Breaking**: Minor named parameter renaming: `url` -> `uri`, `factory` -> `initializer`.

---

- **New**: `coil-svg` artifact, which has an `SvgDecoder` that supports automatically decoding SVGs. Powered by [AndroidSVG](https://github.com/BigBadaboom/androidsvg). Thanks @rharter.
- **New**: `load(String)` and `get(String)` now accept any of the supported Uri schemes. e.g. You can now do `imageView.load("file:///path/to/file.jpg")`.
- **New**: Refactor ImageLoader to use `Call.Factory` instead of `OkHttpClient`. This allows lazy initialization of the networking resources using `ImageLoaderBuilder.okHttpClient { OkHttpClient() }`. Thanks @ZacSweers.
- **New**: `RequestBuilder.decoder` to explicitly set the decoder for a request.
- **New**: `ImageLoaderBuilder.allowHardware` to enable/disable hardware bitmaps by default for an ImageLoader.
- **New**: Support software rendering in ImageDecoderDecoder.

---

- Fix: Multiple bugs with loading vector drawables.
- Fix: Support WRAP_CONTENT View dimensions.
- Fix: Support parsing EXIF data longer than 8192 bytes.
- Fix: Don't stretch drawables with different aspect ratios when crossfading.
- Fix: Guard against network observer failing to register due to exception.
- Fix: Fix divide by zero error in MovieDrawable. Thanks @R12rus.
- Fix: Support nested Android asset files. Thanks @JaCzekanski.
- Fix: Guard against running out of file descriptors on Android O and O_MR1.
- Fix: Don't crash when disabling memory cache. Thanks @hansenji.
- Fix: Ensure Target.cancel is always called from the main thread.

---

- Update Kotlin to 1.3.50.
- Update Kotlin Coroutines to 1.3.0.
- Update OkHttp to 3.12.4.
- Update Okio to 2.4.0.
- Update AndroidX dependencies to the latest stable versions:
    - `androidx.appcompat:appcompat` -> 1.1.0
    - `androidx.core:core-ktx` -> 1.1.0
    - `androidx.lifecycle:lifecycle-common-java8` -> 2.1.0
- Replace `appcompat` with `appcompat-resources` as an optional `compileOnly` dependency. `appcompat-resources` is a much smaller artifact.

## [0.6.1] - August 16, 2019
- New: Add `transformations(List<Transformation>)` to RequestBuilder.
- Fix: Add the last modified date to the cache key for file uris.
- Fix: Ensure View dimensions are evaluated to at least 1px.
- Fix: Clear MovieDrawable's canvas between frames.
- Fix: Open assets correctly.

## [0.6.0] - August 12, 2019
- Initial release.
