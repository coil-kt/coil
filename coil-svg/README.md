# coil-svg

Adds SVG decoding support to Coil.

```kotlin
implementation("io.coil-kt:coil-svg:latest.version.here")
```
Note that this imports [AndroidSVG](https://bigbadaboom.github.io/androidsvg/) as well.

## Quick Start

Add the decoders to your component registry when constructing your ImageLoader:

```kotlin
// within a global place, like your Application.onCreate()
val imageLoader = ImageLoader(context) {
    componentRegistry {
        add(SvgDecoder())
    }
}
Coil.setDefaultImageLoader(imageLoader)
```

And that's it! The ImageLoader will automatically detect any SVGs using their file headers and decode them correctly.
