# Gifs

Coil supports animated GIFs and WebP images on Android, JVM desktop, and supported native
targets. Animated HEIF images are also supported on Android 28 and above.

To add GIF support, import the extension library:

```kotlin
implementation("io.coil-kt.coil3:coil-gif:3.6.1")
```

And that's it! The `ImageLoader` will automatically detect any animated images using their file headers and decode them correctly.

## Platform Support

| Decoder | Platforms | Formats |
|---------|-----------|---------|
| `AnimatedImageDecoder` | Android 28+ | GIF, WebP, HEIF |
| `GifDecoder` | Android (all versions) | GIF only |
| `AnimatedSkiaImageDecoder` | JVM desktop, iOS, macOS, Linux native | GIF, WebP |

## Usage

The recommended approach is to use `AnimatedImageDecoderFactory()`, which automatically selects the appropriate decoder for each platform:

```kotlin
val imageLoader = ImageLoader.Builder(context)
    .components {
        add(AnimatedImageDecoderFactory())
    }
    .build()
```

Alternatively, you can manually add a specific decoder:

```kotlin
// Android
val imageLoader = ImageLoader.Builder(context)
    .components {
        if (SDK_INT >= 28) {
            add(AnimatedImageDecoder.Factory())
        } else {
            add(GifDecoder.Factory())
        }
    }
    .build()

// Non-Android (JVM desktop and supported native targets)
val imageLoader = ImageLoader.Builder(context)
    .components {
        add(AnimatedSkiaImageDecoder.Factory())
    }
    .build()
```

## Transformations

To transform the pixel data of each frame of an animated image, see [AnimatedTransformation](/coil/api/coil-gif/coil3.gif/-animated-transformation).

## Notes

- `GifDecoder` supports all Android API levels but is slower than `AnimatedImageDecoder`.
- `AnimatedImageDecoder` is powered by Android's [ImageDecoder](https://developer.android.com/reference/android/graphics/ImageDecoder) API (API 28+) and supports animated WebP and HEIF.
- `AnimatedSkiaImageDecoder` uses Skia for decoding. Its factory keeps at most two decoded frames
  in memory by default. Pass `bufferedFramesCount` to tune that bound for a specific workload.
- JavaScript and WebAssembly are not supported because Skia's animated-image decoder does not
  currently provide usable performance on those targets.
