# Jetpack Compose

To add support for [Jetpack Compose](https://developer.android.com/jetpack/compose), import the extension library:

```kotlin
implementation("io.coil-kt:coil-compose:2.0.0-rc01")
```

Then use the `AsyncImage` composable to load and display an image:

```kotlin
// Basic
AsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = null
)
```

`model` can either be the `ImageRequest.data` value - or the `ImageRequest` itself. `contentDescription` sets the text used by accessibility services to describe what this image represents.

`AsyncImage` supports the same arguments as the standard `Image` composable. Additionally, it supports setting `placeholder`/`error`/`fallback` painters and `onLoading`/`onSuccess`/`onError` callbacks. Here's an example that loads image with a circle crop, crossfade, and sets a placeholder:

```kotlin
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data("https://example.com/image.jpg")
        .crossfade(true)
        .build(),
    placeholder = painterResource(R.drawable.placeholder),
    contentScale = ContentScale.Crop,
    contentDescription = stringResource(R.string.image_description),
    modifier = Modifier.clip(CircleShape)
)
```

## SubcomposeAsyncImage

`SubcomposeAsyncImage` is a variant of `AsyncImage` that uses subcomposition to provide a slot API for `AsyncImagePainter`'s states. Here's an example:

```kotlin
SubcomposeAsyncImage(
    model = "https://example.com/image.jpg",
    loading = {
        CircularProgressIndicator()
    },
    contentDescription = stringResource(R.string.image_description),
)
```

Additionally, you can have more complex logic using its `content` argument and `SubcomposeAsyncImageContent`, which renders the current state:

```kotlin
SubcomposeAsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = stringResource(R.string.image_description),
) {
    val state = painter.state
    if (state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Error) {
        CircularProgressIndicator()
    } else {
        SubcomposeAsyncImageContent()
    }
}
```

Subcomposition is more expensive computationally than regular composition so this composable may not be suitable for parts of your UI where high performance is critical. 

!!! Note
    If you set a custom size for the `ImageRequest` using `ImageRequest.Builder.size` (e.g. `size(Size.ORIGINAL)`), `SubcomposeAsyncImage` will not use subcomposition since it doesn't need to resolve the composable's constraints.

## AsyncImagePainter

Internally, `AsyncImage` and `SubcomposeAsyncImage` use `AsyncImagePainter` to load the `model`. If you need a `Painter` and can't use `AsyncImage`, you can load the image using `rememberAsyncImagePainter`:

```kotlin
val painter = rememberAsyncImagePainter("https://example.com/image.jpg")
```

`rememberAsyncImagePainter` is a lower-level API that may not behave as expected in all cases. Read the method's documentation for more information.

!!! Note
    If you set a custom `ContentScale` on the `Image` that's rendering the `AsyncImagePainter`, you should also set it in `rememberAsyncImagePainter`. It's necessary to determine the correct dimensions to load the image at.

## Transitions

You can enable the built in crossfade transition using `ImageRequest.Builder.crossfade`:

```kotlin
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data("https://example.com/image.jpg")
        .crossfade(true)
        .build(),
    contentDescription = null
)
```

Custom [`Transition`](transitions.md)s do not work with `AsyncImage`, `SubcomposeAsyncImage`, or `rememberAsyncImagePainter` as they require a `View` reference. `CrossfadeTransition` works due to special internal support.

That said, it's possible to create custom transitions in Compose by observing the `AsyncImagePainter`'s state:

```kotlin
SubcomposeAsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = null
) {
    val state = painter.state
    if (state is AsyncImagePainter.State.Success && state.dataSource != DataSource.MEMORY_CACHE) {
        // Perform the transition animation.
    }

    // Render the content.
    SubcomposeAsyncImageContent()
}
```
