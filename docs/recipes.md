# Recipes

This page provides guidance on how to handle some common use cases with Coil. You might have to modify this code to fit your exact requirements, but it should hopefully give you a push in the right direction!

See a common use case that isn't covered? Feel free to submit a PR with a new section.

## Palette

[Palette](https://developer.android.com/training/material/palette-colors?hl=en) allows you to exact prominent colors from an image. To create a `Palette`, you'll need access to an image's `Bitmap`. This can be done in a number of ways:

#### Enqueue

You can get access to an image's bitmap by setting a `Target` and enqueuing `ImageRequest`:

```kotlin
val request = ImageRequest.Builder(context)
    .data("https://www.example.com/image.jpg")
    .allowHardware(false) // Disable hardware bitmaps.
    .target { drawable ->
        // Generate the Palette on a background thread.
        val task = Palette.Builder(drawable.toBitmap()).generate { palette ->
            // Consume the palette.
        }
    }
    .build()
val disposable = imageLoader.enqueue(request)
```

#### Execute

You can also execute an `ImageRequest`, which returns the drawable imperatively:

```kotlin
val request = ImageRequest.Builder(context)
    .data("https://www.example.com/image.jpg")
    .allowHardware(false) // Disable hardware bitmaps.
    .build()
val drawable = (imageLoader.execute(request) as SuccessResult).drawable

val palette = coroutineScope {
    launch(Dispatchers.IO) {
        Palette.Builder(drawable.toBitmap()).generate()
    }
}
```

#### Transition

There may be cases where you want to load an image into a `PoolableViewTarget` (e.g. `ImageViewTarget`) while extracting the image's colors in parallel. For these cases, you can use a custom [`Transition`](transitions.md) to get access to the underlying bitmap:

```kotlin
class PaletteTransition(
    private val delegate: Transition?,
    private val onGenerated: (Palette) -> Unit
) : Transition {

    override suspend fun transition(target: TransitionTarget, result: RequestResult) {
        // Execute the delegate transition.
        val delegateJob = delegate?.let { delegate ->
            coroutineScope {
                launch(Dispatchers.Main.immediate) {
                    delegate.transition(target, result)
                }
            }
        }

        // Compute the palette on a background thread.
        if (result is SuccessResult) {
            val bitmap = result.drawable.toBitmap()
            val palette = withContext(Dispatchers.IO) {
                Palette.Builder(bitmap).generate()
            }
            onGenerated(palette)
        }

        delegateJob?.join()
    }
}

// ImageRequest
val request = ImageRequest.Builder(context)
    .data("https://www.example.com/image.jpg")
    .allowHardware(false) // Disable hardware bitmaps.
    .transition(PaletteTransition(CrossfadeTransition()) { palette ->
        // Consume the palette.
    })
    .target(imageView)
    .build()
imageLoader.enqueue(request)

// ImageView.load
imageView.load("https://www.example.com/image.jpg") {
    allowHardware(false)
    transition(PaletteTransition(CrossfadeTransition()) { palette ->
        // Consume the palette.
    })
}
```

!!! Note
    You should not pass the drawable outside the scope of `Transition.transition`. This can cause the drawable's underlying bitmap to be pooled while it is still in use, which can result in rendering issues and crashes.

## Using a custom OkHttpClient

Coil uses [`OkHttp`](https://github.com/square/okhttp/) for all its networking operations. You can specify a custom `OkHttpClient` when creating your `ImageLoader`:

```kotlin
val imageLoader = ImageLoader.Builder(context)
    // Create the OkHttpClient inside a lambda so it will be initialized lazily on a background thread.
    .okHttpClient {
        OkHttpClient.Builder()
            // You need to set the cache for disk caching to work.
            .cache(CoilUtils.createDefaultCache(context))
            .build()
    }
    .build()
```

!!! Note
    If you already have a built `OkHttpClient`, use [`newBuilder()`](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-http-url/new-builder/) to build a new client that shares resources with the original. Also, it's recommended to use a separate `Cache` instance for your Coil `OkHttpClient`. Image files can quickly evict more important cached network responses if they share the same cache.

#### Headers

Headers can be added to your image requests in one of two ways. You can set headers for a single request:

```kotlin
val request = ImageRequest.Builder(context)
    .data("https://www.example.com/image.jpg")
    .setHeader("Cache-Control", "max-age=31536000,public")
    .target(imageView)
    .build()
imageLoader.execute(request)
```

Or you can create an OkHttp [`Interceptor`](https://square.github.io/okhttp/interceptors/) that sets headers for every request executed by your `ImageLoader`:

```kotlin
class ResponseHeaderInterceptor(
    private val name: String,
    private val value: String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        return response.newBuilder().header(name, value).build()
    }
}

val imageLoader = ImageLoader.Builder(context)
    .okHttpClient {
        OkHttpClient.Builder()
            .cache(CoilUtils.createDefaultCache(context))
            // This header will be added to every image request.
            .addNetworkInterceptor(ResponseHeaderInterceptor("Cache-Control", "max-age=31536000,public"))
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
listImageView.load("https://www.example.com/image.jpg")

// Second request
detailImageView.load("https://www.example.com/image.jpg") {
    placeholderMemoryCacheKey(listImageView.metadata.memoryCacheKey)
}
```

!!! Note
    Previous versions of Coil would attempt to set up this effect **automatically**. This required executing parts of the image pipeline synchronously on the main thread and it was ultimately removed in version `0.12.0`.

## Shared Element Transitions

[Shared element transitions](https://developer.android.com/training/transitions/start-activity) allow you to animate between `Activities` and `Fragments`. Here are some recommendations on how to get them to work with Coil:

- **Shared element transitions are incompatible with hardware bitmaps.** You should set `allowHardware(false)` to disable hardware bitmaps for both the `ImageView` you are animating from and the view you are animating to. If you don't, the transition will throw an `java.lang.IllegalArgumentException: Software rendering doesn't support hardware bitmaps` exception.

- Use the [`MemoryCache.Key`](getting_started.md#memory-cache) of the start image as the [`placeholderMemoryCacheKey`](../api/coil-base/coil.request/-image-request/-builder/placeholder-memory-cache-key) for the end image. This ensures that the start image is used as the placeholder for the end image, which results in a smooth transition with no white flashes if the image is in the memory cache.

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
val target = RemoteViewsTarget(...)
val request = ImageRequest.Builder(context)
    .data("https://www.example.com/image.jpg")
    .target(target)
    .build()
imageLoader.enqueue(request)
```
