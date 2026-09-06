# Gifs

Coil supports animated GIFs and WebP images on Android, JVM desktop, and supported native
targets. Animated HEIF images are also supported on Android 30 and above.

To add GIF support, import the extension library:

```kotlin
implementation("io.coil-kt.coil3:coil-gif:3.6.2")
```

And that's it! The `ImageLoader` will automatically detect any animated images using their file headers and decode them correctly.

## Platform Support

| Decoder | Platforms | Formats                   |
|---------|-----------|---------------------------|
| `AnimatedImageDecoder` | Android 28+ | GIF, WebP, HEIF (API 30+) |
| `GifDecoder` | Android (all versions) | GIF only                  |
| `AnimatedSkiaImageDecoder` | JVM desktop, iOS, macOS, Linux native | GIF, WebP                 |

## Usage

If you want to manually include the animated image decoder factory, use:

```kotlin
val imageLoader = ImageLoader.Builder(context)
    .components {
        add(AnimatedImageDecoderFactory())
    }
    .build()
```

## Transformations

To transform the pixel data of each frame of an animated image, see [AnimatedTransformation](/coil/api/coil-gif/coil3.gif/-animated-transformation).

## Notes

- `GifDecoder` supports all Android API levels but is slower than `AnimatedImageDecoder`.
- `AnimatedImageDecoder` is powered by Android's [ImageDecoder](https://developer.android.com/reference/android/graphics/ImageDecoder) API (API 28+) and supports animated WebP and HEIF (API 30+).
- `AnimatedSkiaImageDecoder` uses Skia for decoding. By default, it buffers at most two decoded frame snapshots, in addition to the encoded image data and reusable decode/output bitmaps. Pass `bufferedFramesCount` to configure the frame buffer. Single-frame GIFs are decoded to a `BitmapImage` and release the encoded data immediately.
- `AnimatedSkiaImageDecoder` depends on the Compose runtime and will not animate properly in other environments.
- JavaScript and WebAssembly targets are not supported.
