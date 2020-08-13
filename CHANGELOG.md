# Changelog

## [0.12.0] - August XX, 2020

- **Breaking**: `LoadRequest` and `GetRequest` have been replaced with `ImageRequest`:
  - `ImageLoader.execute(LoadRequest)` -> `ImageLoader.enqueue(ImageRequest)`
  - `ImageLoader.execute(GetRequest)` -> `ImageLoader.execute(ImageRequest)`
  - `ImageRequest` implements `equals`/`hashCode`.
- **Breaking**: A number of classes have been renamed:
  - `RequestResult` -> `ImageResult`
  - `RequestDisposable` -> `Disposable`
- **Breaking**: [`SparseIntArraySet`](https://github.com/coil-kt/coil/blob/f52addd039f0195b66f93cb0f1cad59b0832f784/coil-base/src/main/java/coil/collection/SparseIntArraySet.kt) has been removed from the public API.
- **Breaking**: `TransitionTarget` no longer implements `ViewTarget`.
- **Breaking**: `ImageRequest.Listener.onSuccess`'s signature has changed to return an `ImageResult.Metadata` instead of just a `DataSource`.
- **Breaking**: Remove support for `LoadRequest.aliasKeys`. This API is limiting and can be replaced with direct read/write access to the memory cache.

---

- **New**: Add support for direct read/write access to an `ImageLoader`'s `MemoryCache`. See [the docs](https://coil-kt.github.io/coil/image_pipeline/#interceptors) for more information.
- **New**: Add support for `Interceptor`s. See [the docs](https://coil-kt.github.io/coil/image_pipeline/#interceptors) for more information.
- **New**: Add the ability to enable/disable bitmap pooling using `ImageLoader.Builder.bitmapPoolingEnabled`.
- **New**: Support thread interruption while decoding.

---

- **Important**: `Mappers` are now executed on a background dispatcher. As a side effect, automatic bitmap sampling is no longer **automatically** supported. To achieve the same affect, use the `MemoryCache.Key` of a previous request as the `placeholderMemoryCacheKey` of the subsequent request. Here's an example:
  ```kotlin
  listImageView.load("https://www.example.com/image.jpg")

  // Later when you navigate to your app's detail view.
  detailImageView.load("https://www.example.com/image.jpg") {
      placeholderMemoryCacheKey(listImageView.metadata.memoryCacheKey)
  }
  ```
- **Important**: Coil's `ImageView` extension functions have been moved from the `coil.api` package to the `coil` package.
  - Use find + replace to refactor `import coil.api.load` -> `import coil.load`. Unfortunately, it's not possible to use Kotlin's `ReplaceWith` functionality to replace imports.
- **Important**: Use standard crossfade if drawables are not the same image.
- **Important**: Prefer immutable bitmaps on API 24+.

---

- Fix parsing multiple segments in content-type header.
- Rework BitmapReferenceCounter to be more robust.
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

    val disposable = imageLoader.load(context, "https://www.example.com/image.jpg") {
        target(imageView)
    }

    val drawable = imageLoader.get("https://www.example.com/image.jpg") {
        size(512, 512)
    }

    // 0.10.0 (new)
    val imageLoader = ImageLoader.Builder(context)
        .bitmapPoolPercentage(0.5)
        .crossfade(true)
        .build()

    val request = LoadRequest.Builder(context)
        .data("https://www.example.com/image.jpg")
        .target(imageView)
        .build()
    val disposable = imageLoader.execute(request)

    val request = GetRequest.Builder(context)
        .data("https://www.example.com/image.jpg")
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

- Add a new [EventListener](https://github.com/coil-kt/coil/blob/master/coil-base/src/main/java/coil/EventListener.kt) API for tracking metrics.

- Add [ImageLoaderFactory](https://github.com/coil-kt/coil/blob/master/coil-singleton/src/main/java/coil/ImageLoaderFactory.kt) which can be implemented by your `Application` to simplify singleton initialization.

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
- **New**: Introduce [`EventListener`](https://github.com/coil-kt/coil/blob/master/coil-base/src/main/java/coil/EventListener.kt). ([#314](https://github.com/coil-kt/coil/pull/314))
- **New**: Introduce [`ImageLoaderFactory`](https://github.com/coil-kt/coil/blob/master/coil-singleton/src/main/java/coil/ImageLoaderFactory.kt). ([#311](https://github.com/coil-kt/coil/pull/311))
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
- **New**: Add `Precision`. This makes the size of the output `Drawable` exact while enabling scaling optimizations for targets that support scaling (e.g. `ImageViewTarget`). See [its documentation](https://github.com/coil-kt/coil/blob/master/coil-base/src/main/java/coil/size/Precision.kt) for more information.
- **New**: Add `RequestBuilder.aliasKeys` to support matching multiple cache keys.

---

- Fix: Make RequestDisposable thread safe.
- Fix: `RoundedCornersTransformation` now crops to the size of the target then rounds the corners.
- Fix: `CircleCropTransformation` now crops from the center.
- Fix: Add several devices to the [hardware bitmap blacklist](https://github.com/coil-kt/coil/blob/master/coil-base/src/main/java/coil/memory/HardwareBitmapService.kt).
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
