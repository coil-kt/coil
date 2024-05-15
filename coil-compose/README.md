# Jetpack Compose

To add support for [Jetpack Compose](https://developer.android.com/jetpack/compose), import the extension library:

```kotlin
implementation("io.coil-kt:coil-compose:2.6.0")
```

Then use the `AsyncImage` composable to load and display an image:

```kotlin
AsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = null,
)
```

`model` can either be the `ImageRequest.data` value - or the `ImageRequest` itself. `contentDescription` sets the text used by accessibility services to describe what this image represents.

## AsyncImage

`AsyncImage` is a composable that executes an image request asynchronously and renders the result. It supports the same arguments as the standard `Image` composable and additionally, it supports setting `placeholder`/`error`/`fallback` painters and `onLoading`/`onSuccess`/`onError` callbacks. Here's an example that loads an image with a circle crop, crossfade, and sets a placeholder:

```kotlin
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data("https://example.com/image.jpg")
        .crossfade(true)
        .build(),
    placeholder = painterResource(R.drawable.placeholder),
    contentDescription = stringResource(R.string.description),
    contentScale = ContentScale.Crop,
    modifier = Modifier.clip(CircleShape)
)
```

## SubcomposeAsyncImage

`SubcomposeAsyncImage` is a variant of `AsyncImage` that uses subcomposition to provide a slot API for `AsyncImagePainter`'s states instead of using `Painter`s. Here's an example:

```kotlin
SubcomposeAsyncImage(
    model = "https://example.com/image.jpg",
    loading = {
        CircularProgressIndicator()
    },
    contentDescription = stringResource(R.string.description)
)
```

Additionally, you can have more complex logic using its `content` argument and `SubcomposeAsyncImageContent`, which renders the current state:

```kotlin
SubcomposeAsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = stringResource(R.string.description)
) {
    val state = painter.state
    if (state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Error) {
        CircularProgressIndicator()
    } else {
        SubcomposeAsyncImageContent()
    }
}
```

Subcomposition is less performant than regular composition so this composable may not be suitable for parts of your UI where high performance is critical (e.g. lists).

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

## Observing AsyncImagePainter.state

An image request needs a size to determine the output image's dimensions. By default, both `AsyncImage` and `AsyncImagePainter` resolve the request's size [**after** composition occurs](https://developer.android.com/jetpack/compose/layouts/basics), but before the first frame is drawn. It's resolved this way to maximize performance. This means that `AsyncImagePainter.state` will be `Loading` for the first composition - even if the image is present in the memory cache and it will be drawn in the first frame.

If you need `AsyncImagePainter.state` to be up-to-date during the first composition, use `SubcomposeAsyncImage` **or** set a custom size for the image request using `ImageRequest.Builder.size`. For example, `AsyncImagePainter.state` will always be up-to-date during the first composition in this example:

```kotlin
val painter = rememberAsyncImagePainter(
    model = ImageRequest.Builder(LocalContext.current)
        .data("https://example.com/image.jpg")
        .size(Size.ORIGINAL) // Set the target size to load the image at.
        .build()
)

if (painter.state is AsyncImagePainter.State.Success) {
    // This will be executed during the first composition if the image is in the memory cache.
}

Image(
    painter = painter,
    contentDescription = stringResource(R.string.description)
)
```

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
val painter = rememberAsyncImagePainter("https://example.com/image.jpg")

val state = painter.state
if (state is AsyncImagePainter.State.Success && state.result.dataSource != DataSource.MEMORY_CACHE) {
    // Perform the transition animation.
}

Image(
    painter = painter,
    contentDescription = stringResource(R.string.description)
)
```
