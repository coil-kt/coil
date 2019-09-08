# Changelog

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

- **New**: `coil-svg` artifact, which has an `SvgDecoder` that supports automatically decoding SVGs. Powered by [AndroidSVG](https://github.com/BigBadaboom/androidsvg).
- **New**: `load(String)` and `get(String)` now accept any of the supported Uri schemes. e.g. You can now do `imageView.load("file:///path/to/file.jpg")`.
- **New**: Refactor ImageLoader to use `Call.Factory` instead of `OkHttpClient`. This allows lazy initialization of the networking resources using `ImageLoaderBuilder.okHttpClient { OkHttpClient() }`.
- **New**: `RequestBuilder.decoder` to explicitly set the decoder for a request.
- **New**: `ImageLoaderBuilder.allowHardware` to enable/disable hardware bitmaps by default for an ImageLoader.
- **New**: Support software rendering in ImageDecoderDecoder.

---

- Fix: Multiple bugs with loading vector drawables.
- Fix: Support WRAP_CONTENT View dimensions.
- Fix: Support parsing EXIF data longer than 8192 bytes.
- Fix: Don't stretch drawables with different aspect ratios when crossfading.
- Fix: Guard against network observer failing to register due to exception.
- Fix: Fix divide by zero error in MovieDrawable.
- Fix: Support nested Android asset files.
- Fix: Guard against running out of file descriptors on Android O and O_MR1.
- Fix: Don't crash when disabling memory cache.

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
