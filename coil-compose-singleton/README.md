# Jetpack Compose

To add support for [Jetpack Compose](https://developer.android.com/jetpack/compose), import the extension library:

```kotlin
implementation("io.coil-kt:coil-compose:1.3.0")
```

Then use the `rememberImagePainter` function to create an `ImagePainter` that can be drawn by the `Image` composable:

```kotlin
// Basic
Image(
    painter = rememberImagePainter("https://www.example.com/image.jpg"),
    contentDescription = null,
    modifier = Modifier.size(128.dp)
)

// Advanced
Image(
    painter = rememberImagePainter(
        data = "https://www.example.com/image.jpg",
        builder = {
            transformation(CircleCropTransformation())
            allowHardware(false)
        }
    ),
    contentDescription = null,
    modifier = Modifier.size(128.dp)
)
```

`ImagePainter` manages the asynchronous image request and handles drawing the placeholder/success/error drawables.

## Transitions

You can enable the built in crossfade transition using `ImageRequest.Builder.crossfade`:

```kotlin
Image(
    painter = rememberImagePainter(
        data = "https://www.example.com/image.jpg",
        builder = {
            crossfade(true)
        }
    ),
    contentDescription = null,
    modifier = Modifier.size(128.dp)
)
```

Custom [`Transition`](transitions.md)s do not work with `rememberImagePainter` as they require a `View` reference. `CrossfadeTransition` works due to special internal support.

That said, it's possible to create custom transitions in Compose by observing the `ImagePainter`'s state:

```kotlin
val painter = rememberImagePainter("https://www.example.com/image.jpg")

val state = painter.state
if (state is ImagePainter.State.Success && state.metadata.dataSource != DataSource.MEMORY_CACHE }) {
    // Perform the transition animation.
}

Image(
    painter = painter,
    contentDescription = null,
    modifier = Modifier.size(128.dp)
)
```

In the above example, the the composable will recompose when the `ImagePainter`'s state changes. If the image request is successful and not served from the memory cache, it'll execute the animation inside the if statement.

## LocalImageLoader

The integration also adds a pseudo-[`CompositionLocal`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/CompositionLocal) for getting the local `ImageLoader`.

In most cases the local `ImageLoader` will be the singleton `ImageLoader`, however it's possible to overwrite the local `ImageLoader` using a `CompositionLocalProvider` if necessary.

```kotlin
// Get
val imageLoader = LocalImageLoader.current

// Set
CompositionLocalProvider(LocalImageLoader provides ImageLoader(context)) {
    // Describe the rest of the UI.
}
```

!!! Note
    There's also the `coil-compose-base` artifact which is a subset of `coil-compose`. It does not include `LocalImageLoader` and the singleton `ImageLoader`.

## Migrating from Accompanist

Coil's Jetpack Compose integration is based on [Accompanist](https://github.com/google/accompanist)'s Coil integration, but has the following changes:

- `rememberCoilPainter` is renamed to `rememberImagePainter` and its arguments changed:
    - `shouldRefetchOnSizeChange` is replaced with `onExecute`, which has more control over if image requests are executed or skipped.
    - `requestBuilder` is renamed to `builder`.
    - `fadeIn` and `fadeInDurationMs` are removed. Migrate to `ImageRequest.Builder.crossfade` (see [Transitions](#Transitions)).
    - `previewPlaceholder` is removed. `ImageRequest.placeholder` is now automatically used if inspection mode is enabled.
- `LoadPainter` is renamed to `ImagePainter`.
    - `ImagePainter` no longer falls back to executing an image request with the root view's size if `onDraw` is not called. This is most likely to be noticeable if you use `ImagePainter` in a `LazyColumn` and the `Image`'s size isn't constrained.
- `Loader` and `rememberLoadPainter` are removed.
- `LocalImageLoader.current` is not-null and returns the singleton `ImageLoader` by default.
- `DrawablePainter` and `rememberDrawablePainter` are now private.

Here's an example call site migration:

```kotlin
// accompanist-coil
Image(
    painter = rememberCoilPainter(
        request = "https://www.example.com/image.jpg",
        requestBuilder = {
            allowHardware(false)
        },
        shouldRefetchOnSizeChange = ShouldRefetchOnSizeChange { _, _ -> true },
        fadeIn = true,
        previewPlaceholder = R.drawable.placeholder
    ),
    contentDescription = null,
    modifier = Modifier.size(128.dp)
)

// coil-compose
Image(
    painter = rememberImagePainter(
        data = "https://www.example.com/image.jpg",
        onExecute = ExecuteCallback { _, _ -> true },
        builder = {
            allowHardware(false)
            crossfade(true)
            placeholder(R.drawable.placeholder)
        }
    ),
    contentDescription = null,
    modifier = Modifier.size(128.dp)
)
```
