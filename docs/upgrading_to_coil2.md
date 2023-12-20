# Upgrading to Coil 2.x

This is a short guide to highlight the main changes when upgrading from Coil 1.x to 2.x and how to handle them. This upgrade guide doesn't cover every binary or source incompatible change, but it does cover the most important changes.

## Minimum API 21

Coil 2.x requires minimum API 21. This is also the minimum API required for Jetpack Compose and OkHttp 4.x.

## ImageRequest default scale

Coil 2.x changes `ImageRequest`'s default scale from `Scale.FILL` to `Scale.FIT`. This was done to be consistent with `ImageView`'s default `ScaleType` and `Image`'s default `ContentScale`. Scale is still autodetected if you set an `ImageView` as your `ImageRequest.target`.

## Size refactor

`Size`'s `width` and `height` are now two `Dimension`s instead of `Int` pixel values. `Dimension` is either a pixel value or `Dimension.Undefined`, which represents an undefined/unbounded constraint. For example, if the size is `Size(400, Dimension.Undefined)` that means the image should be scaled to have 400 pixels for its width irrespective of its height. You can use the `pxOrElse` extension to get the pixel value (if present), else use a fallback:

```kotlin
val width = size.width.pxOrElse { -1 }
if (width > 0) {
    // Use the pixel value.
}
```

This change was made to improve support for cases where a target has one unbounded dimension (e.g. if one dimension is `ViewGroup.LayoutParams.WRAP_CONTENT` for a `View` or `Constraints.Infinity` in Compose).

## Jetpack Compose

Coil 2.x significantly reworks the Jetpack Compose integration to add features, improve stability, and improve performance.

In Coil 1.x you would use `rememberImagePainter` to load an image:

```kotlin
val painter = rememberImagePainter("https://example.com/image.jpg") {
    crossfade(true)
}

Image(
    painter = painter,
    contentDescription = null,
    contentScale = ContentScale.Crop
)
```

In Coil 2.x `rememberImagePainter` has been changed to `rememberAsyncImagePainter` with the following changes:

- The trailing lambda argument to configure the `ImageRequest` has been removed.
- In Coil 2.x, `rememberAsyncImagePainter` defaults to using `ContentScale.Fit` to be consistent with `Image` whereas in Coil 1.x it would default to `ContentScale.Crop`. As such, if you set a custom `ContentScale` on `Image`, you now also need to pass it to `rememberAsyncImagePainter`.

```kotlin
val painter = rememberAsyncImagePainter(
    model = ImageRequest.Builder(LocalContext.current)
        .data("https://example.com/image.jpg")
        .crossfade(true)
        .build(),
    contentScale = ContentScale.Crop
)

Image(
    painter = painter,
    contentDescription = null,
    contentScale = ContentScale.Crop
)
```

Additionally, Coil now has `AsyncImage` and `SubcomposeAsyncImage` composable functions, which add new features and work-around some design limitations of `rememberAsyncImagePainter`. Check out the full Jetpack Compose docs [here](compose.md).

## Disk Cache

Coil 2.x has its own public disk cache class that can be accessed using `imageLoader.diskCache`. Coil 1.x relied on OkHttp's disk cache, however it's no longer needed.

To configure the disk cache in 1.x you would use `CoilUtils.createDefaultCache`:

```kotlin
ImageLoader.Builder(context)
    .okHttpClient {
        OkHttpClient.Builder().cache(CoilUtils.createDefaultCache(context)).build()
    }
    .build()
```

In Coil 2.x you should not set a `Cache` object on your `OkHttpClient` when used with an `ImageLoader`. Instead configure the disk cache object like so:

```kotlin
ImageLoader.Builder(context)
    .diskCache {
        DiskCache.Builder()
            .directory(context.cacheDir.resolve("image_cache"))
            .build()
    }
    .build()
```

This change was made to add functionality and improve performance:

- Support thread interruption while decoding images.
  - Thread interruption allows fast cancellation of decode operations. This is particularly important for quickly scrolling through a list.
  - By using a custom disk cache Coil is able to ensure a network source is fully read to disk before decoding. This is necessary as writing the data to disk cannot be interrupted - only the decode step can be interrupted. OkHttp's `Cache` shouldn't be used with Coil 2.0 as it's not possible to guarantee that all data is written to disk before decoding.
- Avoid buffering/creating temporary files for decode APIs that don't support `InputStream`s or require direct access to a `File` (e.g. `ImageDecoder`, `MediaMetadataRetriever`).
- Add a public read/write `DiskCache` API.

In Coil 2.x `Cache-Control` and other cache headers are still supported - except `Vary` headers, as the cache only checks that the URLs match. Additionally, only responses with a response code in the range [200..300) are cached.

When upgrading from Coil 1.x to 2.x, any existing disk cache will be cleared as the internal format has changed.

## Image pipeline refactor

Coil 2.x refactors the image pipeline classes to be more flexible. Here's a high-level list of the changes:

- Introduce a new class, `Keyer`, that computes the memory cache key for a request. It replaces `Fetcher.key`.
- `Mapper`, `Keyer`, `Fetcher`, and `Decoder` can return `null` to delegate to the next element in the list of components.
- Add `Options` to `Mapper.map`'s signature.
- Introduce `Fetcher.Factory` and `Decoder.Factory`. Use the factories to determine if a specific `Fetcher`/`Decoder` is applicable. Return `null` if that `Fetcher`/`Decoder` is not applicable.

## Remove bitmap pooling

Coil 2.x removes bitmap pooling and its associated classes (`BitmapPool`, `PoolableViewTarget`). See [here](https://github.com/coil-kt/coil/discussions/1186#discussioncomment-2305528) for why it was removed.
