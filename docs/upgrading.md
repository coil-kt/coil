# Upgrading from Coil 1.x to 2.x

This is a short guide to highlight the main changes when upgrading from Coil 1.x to 2.x and how to handle them. This upgrade guide doesn't cover every binary or source incompatible change, but it does cover the most important changes.

## Minimum API 21

Coil 2.x requires minimum API 21. This is also the minimum API required for Jetpack Compose and OkHttp 4.x.

## Jetpack Compose

Coil 2.x significantly reworks the Jetpack Compose integration to add features, improve stability, and improve performance.

In Coil 1.x you would use `rememberImagePainter` to load an image:

```kotlin
val painter = rememberImagePainter("https://www.example.com/image.jpg") {
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
        .data("https://www.example.com/image.jpg")
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

In Coil 2.x `Cache-Control` and other cache headers are still supported - except `Vary` headers, as the cache only checks that the URLs match. Additionally, only responses with a response code in the range [200..300) are cached.

When upgrading from Coil 1.x to 2.x, any existing disk cache will be cleared as the internal format has changed.

## ImageRequest defaults

Coil 2.x changes the default scale from `Scale.FILL` to `Scale.FIT`. This was done to ensure be consistent with `ImageView`'s default `ScaleType` and `Image`'s default `ContentScale`. Scale is still autodetected if you set an `ImageView` as your `ImageRequest.target`.

Coil 2.x also changes the default size to `Size.ORIGINAL`. In Coil 1.x, the default `SizeResolver` uses the current display's size. If you want to restore the behaviour from 1.x, copy [`DisplaySizeResolver`](https://github.com/coil-kt/coil/blob/1.x/coil-base/src/main/java/coil/size/DisplaySizeResolver.kt) into your project and manually set it on your `ImageRequest`. This change was made to ensure transformations that modify the aspect ratio of the image (e.g. `RoundedCornersTransformation`) work as expected when a custom size isn't set.

## Size refactor

`Size`'s `width` and `height` is now composed of two `Dimension`s instead of `Int` pixel values. `Dimension` is either a pixel value or `Dimension.Original`, which represents the source value for that dimension (similar to how `Size.ORIGINAL` represents the source values for both width and height). You can use the `pxOrElse` extension to get the pixel value (if present), else use a fallback:

```kotlin
val width = size.width.pxOrElse { -1 }
if (width > 0) {
    // Use the pixel value.
}
```

This change was made to improve support for cases where a target has one unbounded dimension (e.g. if one dimension is `Constraints.Infinity` in Compose).

## Image pipeline refactor

Coil 2.x refactors the image pipeline classes to be more flexible. Here's a high-level list of the changes:

- Introduce a new class, `Keyer`, that computes the memory cache key for a request. It replaces `Fetcher.key`.
- `Mapper`, `Keyer`, `Fetcher`, and `Decoder` can return `null` to delegate to the next element in the list of components.
- Add `Options` to `Mapper.map`'s signature.
- Introduce `Fetcher.Factory` and `Decoder.Factory`. Use the factories to determine if a specific `Fetcher`/`Decoder` is applicable.

## Remove bitmap pooling

Coil 2.x removes bitmap pooling and its associated classes (`BitmapPool`, `PoolableViewTarget`). See [here](https://github.com/coil-kt/coil/discussions/1186#discussioncomment-2305528) for why it was removed.
