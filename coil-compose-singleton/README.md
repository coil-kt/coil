# Jetpack Compose

To add support for [Jetpack Compose](https://developer.android.com/jetpack/compose), import the extension library:

```kotlin
implementation("io.coil-kt:coil-compose:1.3.0")
```

To load an image in Jetpack Compose, use the `rememberImagePainter` function:

```kotlin
// Basic usage
Image(
    painter = rememberImagePainter("https://www.example.com/image.jpg"),
    contentDescription = null,
    modifier = Modifier.size(128.dp)
)

// Configure the image request
Image(
    painter = rememberImagePainter(
        data = "https://www.example.com/image.jpg"
        builder = {
            fadeIn(true)
            allowHardware(false)
        }
    ),
    contentDescription = null,
    modifier = Modifier.size(128.dp)
)
```

The integration also adds a pseudo-[`CompositionLocal`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/CompositionLocal) for getting/setting the `ImageLoader` for a composable.

Most apps will only use it to get the singleton `ImageLoader`, but this enables setting local `ImageLoader` instances (for a specific screen, for example) if necessary.

```kotlin
// Get
val imageLoader = LocalImageLoader.current

// Set
CompositionLocalProvider(LocalImageLoader provides ImageLoader(context)) {
    // Describe the rest of the UI.
}
```

!!! Note
    There's also a `coil-compose-base` artifact which is a subset of `coil-compose`. It does not include `LocalImageLoader` and does not depend on the singleton `ImageLoader`.
