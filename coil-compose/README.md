# Compose

To add support for [Compose UI](https://www.jetbrains.com/compose-multiplatform/), import the extension library:

```kotlin
implementation("io.coil-kt.coil3:coil-compose:3.0.0-rc01")
```

Then use the `AsyncImage` composable to load and display an image:

```kotlin
AsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = null,
)
```

`model` can either be the `ImageRequest.data` value - or the `ImageRequest` itself. `contentDescription` sets the text used by accessibility services to describe what this image represents.

!!! Note
    If you use Compose on JVM/desktop you should import `org.jetbrains.kotlinx:kotlinx-coroutines-swing:<coroutines-version>`. Coil relies on `Dispatchers.Main.immediate` to resolve images from the memory cache synchronously and `kotlinx-coroutines-swing` provides support for that on JVM (non-Android) platforms.

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
    modifier = Modifier.clip(CircleShape),
)
```

**When to use this function:**

Prefer using `AsyncImage` in most cases. It correctly determines the size your image should be loaded at based on the constraints of the composable and the provided `ContentScale`.

## rememberAsyncImagePainter

Internally, `AsyncImage` and `SubcomposeAsyncImage` use `rememberAsyncImagePainter` to load the `model`. If you need a `Painter` and not a composable, you can load the image using `rememberAsyncImagePainter`:

```kotlin
val painter = rememberAsyncImagePainter("https://example.com/image.jpg")
```

`rememberAsyncImagePainter` is more flexible than `AsyncImage` and `SubcomposeAsyncImage`, but has a couple drawbacks (see below).

**When to use this function:**

Useful if you need a `Painter` instead of a composable - or if you need to observe the `AsyncImagePainter.state` and draw a different composable based on it - or if you need to manually restart the image request using `AsyncImagePainter.restart`.

The main drawback of this function is it does not detect the size your image is loaded at on screen and always loads the image with its original dimensions. You can pass a custom `SizeResolver` or use `ConstraintsSizeResolver` (which is what `AsyncImage` uses internally) to resolve this. Example:

```kotlin
val sizeResolver = rememberConstraintsSizeResolver()
val painter = rememberAsyncImagePainter(
    model = ImageRequest.Builder(LocalPlatformContext.current)
        .data("https://www.example.com/image.jpg")
        .size(sizeResolver)
        .build(),
)

Image(
    painter = painter,
    contentDescription = null,
    modifier = Modifier.then(sizeResolver),
)
```

Another drawback is `AsyncImagePainter.state` will always be `AsyncImagePainter.State.Empty` for the first composition when using `rememberAsyncImagePainter` - even if the image is present in the memory cache and it will be drawn in the first frame.

## SubcomposeAsyncImage

`SubcomposeAsyncImage` is a variant of `AsyncImage` that uses subcomposition to provide a slot API for `AsyncImagePainter`'s states instead of using `Painter`s. Here's an example:

```kotlin
SubcomposeAsyncImage(
    model = "https://example.com/image.jpg",
    loading = {
        CircularProgressIndicator()
    },
    contentDescription = stringResource(R.string.description),
)
```

Additionally, you can have more complex logic using its `content` argument and `SubcomposeAsyncImageContent`, which renders the current state:

```kotlin
SubcomposeAsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = stringResource(R.string.description)
) {
    val state = painter.state.collectAsState().value
    if (state is AsyncImagePainter.State.Success) {
        SubcomposeAsyncImageContent()
    } else {
        CircularProgressIndicator()
    }
}
```

!!! Note
    Subcomposition is slower than regular composition so this composable may not be suitable for performance-critical parts of your UI (e.g. `LazyList`).

**When to use this function:**

Generally prefer using `rememberAsyncImagePainter` instead of this function if you need to observe `AsyncImagePainter.state` as it does not use subcomposition.

Specifically, this function is only useful if you need to observe `AsyncImagePainter.state` and you can't have it be `Loading` for the first composition and first frame like with `rememberAsyncImagePainter`. `SubcomposeAsyncImage` uses subcomposition to get the image's constraints so it's `AsyncImagePainter.state` is up to date immediately.

## Observing AsyncImagePainter.state

Example:

```kotlin
val painter = rememberAsyncImagePainter("https://www.example.com/image.jpg")

when (painter.state) {
    is AsyncImagePainter.State.Empty,
    is AsyncImagePainter.State.Loading -> {
        CircularProgressIndicator()
    }
    is AsyncImagePainter.State.Success -> {
        Image(
            painter = painter,
            contentDescription = stringResource(R.string.description)
        )
    }
    is AsyncImagePainter.State.Error -> {
        // Show some error UI.
    }
}
```

## Transitions

You can enable the built in crossfade transition using `ImageRequest.Builder.crossfade`:

```kotlin
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data("https://example.com/image.jpg")
        .crossfade(true)
        .build(),
    contentDescription = null,
)
```

Custom [`Transition`](transitions.md)s do not work with `AsyncImage`, `SubcomposeAsyncImage`, or `rememberAsyncImagePainter` as they require a `View` reference. `CrossfadeTransition` works due to special internal support.

That said, it's possible to create custom transitions in Compose by observing `AsyncImagePainter.state`:

```kotlin
val painter = rememberAsyncImagePainter("https://example.com/image.jpg")

val state = painter.state.collectAsState().value
if (state is AsyncImagePainter.State.Success && state.result.dataSource != DataSource.MEMORY_CACHE) {
    // Perform the transition animation.
}

Image(
    painter = painter,
    contentDescription = stringResource(R.string.description),
)
```

## Previews

The Android Studio preview behaviour for `AsyncImage`/`rememberAsyncImagePainter`/`SubcomposeAsyncImage` is controlled by the `LocalAsyncImagePreviewHandler`. By default, it will attempt to perform the request as normal inside the preview environment. Network access is disabled in the preview environment so network URLs will always fail.

You can override the preview behaviour like so:

```kotlin
val previewHandler = AsyncImagePreviewHandler {
    FakeImage(color = 0xFFFF0000) // Available in `io.coil-kt.coil3:coil-test`.
}

CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler) {
    AsyncImage(
        model = "https://www.example.com/image.jpg",
        contentDescription = null,
    )
}
```

This is also useful for [AndroidX's Compose Preview Screenshot Testing library](https://developer.android.com/studio/preview/compose-screenshot-testing), which executes in the same preview environment.

## Compose Multiplatform Resources

Coil supports loading [Compose Multiplatform Resources](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-multiplatform-resources.html) by using `Res.getUri` as the `model` parameter. Example:

```kotlin
AsyncImage(
    model = Res.getUri("drawable/sample.jpg"),
    contentDescription = null,
)
```
