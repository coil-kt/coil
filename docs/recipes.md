# Recipes

This page provides guidance on how to handle some common use cases with Coil. You might have to modify this code to fit your exact requirements, but it should hopefully give you a push in the right direction!

See a common use case that isn't covered? Feel free to submit a PR with a new section.

## Palette

[Palette](https://developer.android.com/training/material/palette-colors?hl=en) allows you to extract prominent colors from an image. To create a `Palette`, you'll need access to an image's `Bitmap`. This can be done in a number of ways:

You can get access to an image's bitmap by setting a `ImageRequest.Listener` and enqueuing an `ImageRequest`:

```kotlin
imageView.load("https://example.com/image.jpg") {
    // Disable hardware bitmaps as Palette needs to read the image's pixels.
    allowHardware(false)
    listener(
        onSuccess = { _, result ->
            // Create the palette on a background thread.
            Palette.Builder(result.drawable.toBitmap()).generate { palette ->
                // Consume the palette.
            }
        }
    )
}
```

## Using a custom OkHttpClient

Coil uses [OkHttp](https://github.com/square/okhttp/) for all its networking operations. You can specify a custom `OkHttpClient` when creating your `ImageLoader`:

```kotlin
val imageLoader = ImageLoader.Builder(context)
    // Create the OkHttpClient inside a lambda so it will be initialized lazily on a background thread.
    .okHttpClient {
        OkHttpClient.Builder()
            .addInterceptor(CustomInterceptor())
            .build()
    }
    .build()
```

!!! Note
    If you already have a built `OkHttpClient`, use [`newBuilder()`](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/#customize-your-client-with-newbuilder) to build a new client that shares resources with the original.

#### Headers

Headers can be added to your image requests in one of two ways. You can set headers for a single request:

```kotlin
val request = ImageRequest.Builder(context)
    .data("https://example.com/image.jpg")
    .setHeader("Cache-Control", "no-cache")
    .target(imageView)
    .build()
imageLoader.execute(request)
```

Or you can create an OkHttp [`Interceptor`](https://square.github.io/okhttp/interceptors/) that sets headers for every request executed by your `ImageLoader`:

```kotlin
class RequestHeaderInterceptor(
    private val name: String,
    private val value: String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header(name, value)
            .build()
        return chain.proceed(request)
    }
}

val imageLoader = ImageLoader.Builder(context)
    .okHttpClient {
        OkHttpClient.Builder()
            // This header will be added to every image request.
            .addNetworkInterceptor(RequestHeaderInterceptor("Cache-Control", "no-cache"))
            .build()
    }
    .build()
```

## Using a Memory Cache Key as a Placeholder

Using a previous request's [`MemoryCache.Key`](getting_started.md#memory-cache) as a placeholder for a subsequent request can be useful if the two images are the same, though loaded at different sizes. For instance, if the first request loads the image at 100x100 and the second request loads the image at 500x500, we can use the first image as a synchronous placeholder for the second request.

Here's what this effect looks like in the sample app:

<p style="text-align: center;">
    <video width="360" height="640" autoplay loop muted playsinline>
        <source src="../images/crossfade.mp4" type="video/mp4">
    </video>
</p>

*Images in the list have intentionally been loaded with very low detail and the crossfade is slowed down to highlight the visual effect.*

To achieve this effect, use the `MemoryCache.Key` of the first request as the `ImageRequest.placeholderMemoryCacheKey` of the second request. Here's an example:

```kotlin
// First request
listImageView.load("https://example.com/image.jpg")

// Second request (once the first request finishes)
detailImageView.load("https://example.com/image.jpg") {
    placeholderMemoryCacheKey(listImageView.result.memoryCacheKey)
}
```

!!! Note
    Previous versions of Coil would attempt to set up this effect **automatically**. This required executing parts of the image pipeline synchronously on the main thread and it was ultimately removed in version `0.12.0`.

## Shared Element Transitions

[Shared element transitions](https://developer.android.com/training/transitions/start-activity) allow you to animate between `Activities` and `Fragments`. Here are some recommendations on how to get them to work with Coil:

- **Shared element transitions are incompatible with hardware bitmaps.** You should set `allowHardware(false)` to disable hardware bitmaps for both the `ImageView` you are animating from and the view you are animating to. If you don't, the transition will throw an `java.lang.IllegalArgumentException: Software rendering doesn't support hardware bitmaps` exception.

- Use the [`MemoryCache.Key`](getting_started.md#memory-cache) of the start image as the [`placeholderMemoryCacheKey`](/coil/api/coil-core/coil3.request/-image-request/-builder/placeholder-memory-cache-key) for the end image. This ensures that the start image is used as the placeholder for the end image, which results in a smooth transition with no white flashes if the image is in the memory cache.

- Use [`ChangeImageTransform`](https://developer.android.com/reference/android/transition/ChangeImageTransform) and [`ChangeBounds`](https://developer.android.com/reference/android/transition/ChangeBounds) together for optimal results.

## Remote Views

Coil does not provide a `Target` for [`RemoteViews`](https://developer.android.com/reference/android/widget/RemoteViews) out of the box, however you can create one like so:

```kotlin
class RemoteViewsTarget(
    private val context: Context,
    private val componentName: ComponentName,
    private val remoteViews: RemoteViews,
    @IdRes private val imageViewResId: Int
) : Target {

    override fun onStart(placeholder: Drawable?) = setDrawable(placeholder)

    override fun onError(error: Drawable?) = setDrawable(error)

    override fun onSuccess(result: Drawable) = setDrawable(result)

    private fun setDrawable(drawable: Drawable?) {
        remoteViews.setImageViewBitmap(imageViewResId, drawable?.toBitmap())
        AppWidgetManager.getInstance(context).updateAppWidget(componentName, remoteViews)
    }
}
```

Then `enqueue`/`execute` the request like normal:

```kotlin
val request = ImageRequest.Builder(context)
    .data("https://example.com/image.jpg")
    .target(RemoteViewsTarget(context, componentName, remoteViews, imageViewResId))
    .build()
imageLoader.enqueue(request)
```

## Transforming Painters

Both `AsyncImage` and `AsyncImagePainter` have `placeholder`/`error`/`fallback` arguments that accept `Painter`s. Painters are less flexible than using composables, but are faster as Coil doesn't need to use subcomposition. That said, it may be necessary to inset, stretch, tint, or transform your painter to get the desired UI. To accomplish this, [copy this Gist into your project](https://gist.github.com/colinrtwhite/c2966e0b8584b4cdf0a5b05786b20ae1) and wrap the painter like so:

```kotlin
AsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = null,
    placeholder = forwardingPainter(
        painter = painterResource(R.drawable.placeholder),
        colorFilter = ColorFilter(Color.Red),
        alpha = 0.5f
    )
)
```

The `onDraw` can be overwritten using a trailing lambda:

```kotlin
AsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = null,
    placeholder = forwardingPainter(painterResource(R.drawable.placeholder)) { info ->
        inset(50f, 50f) {
            with(info.painter) {
                draw(size, info.alpha, info.colorFilter)
            }
        }
    }
)
```
