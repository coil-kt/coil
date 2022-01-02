# Jetpack Compose

To add support for [Jetpack Compose](https://developer.android.com/jetpack/compose), import the extension library:

```kotlin
implementation("io.coil-kt:coil-compose:2.0.0-rc01")
```

Then use the `AsyncImage` composable to load and display an image:

```kotlin
// Basic
AsyncImage("https://example.com/image.jpg")
```

`model` can either be the `ImageRequest.data` value - or the `ImageRequest` itself.

`AsyncImage` supports the same arguments as the standard `Image` composable. Additionally, it supports setting `placeholder`/`error`/`fallback` painters and `onLoading`/`onSuccess`/`onError` callbacks. Here's an example that loads image with a circle crop, crossfade, and sets a placeholder:

```kotlin
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data("https://example.com/image.jpg")
        .crossfade(true)
        .build(),
    contentScale = ContentScale.Crop,
    placeholder = painterResource(R.drawable.placeholder),
    modifier = Modifier
        .clip(CircleShape)
)
```

## AsyncImagePainter

Internally, `AsyncImage` uses `AsyncImagePainter` to load the `model`. If you need a `Painter` and can't use `AsyncImage`, you can load the image using `rememberAsyncImagePainter`:

```kotlin
val painter = rememberAsyncImagePainter("https://example.com/image.jpg")
```

That said, you should prefer using `AsyncImage` as `AsyncImagePainter` is unable to determine the target size if its parent constraints are unbounded (due to how `Painter`s are designed in Jetpack Compose) and it will appear to load forever. Additionally, `AsyncImagePainter` can't determine the correct scale and always uses the value from `ImageRequest.scale`, which defaults to `Scale.FIT`. `AsyncImage` does not have these issues.

## Transitions

You can enable the built in crossfade transition using `ImageRequest.Builder.crossfade`:

```kotlin
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data("https://example.com/image.jpg")
        .crossfade(true)
        .build()
)
```

Custom [`Transition`](transitions.md)s do not work with `AsyncImage` or `rememberAsyncImagePainter` as they require a `View` reference. `CrossfadeTransition` works due to special internal support.

That said, it's possible to create custom transitions in Compose by observing the `AsyncImagePainter`'s state:

```kotlin
AsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = null
) { state ->
    if (state is AsyncImagePainter.State.Success && state.dataSource != DataSource.MEMORY_CACHE }) {
        // Perform the transition animation.
    } else {
        // Render the content as normal.
        AsyncImageContent()
    }
}
```

!!! Note
    Using the `loading`/`success`/`error`/`content` slot APIs is expensive as it uses subcomposition. Avoid using them in cases where high UI performance is necessary!
