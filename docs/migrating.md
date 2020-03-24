# Migrating from Glide/Picasso

Here are a few examples of how to migrate Glide/Picasso calls into Coil calls:

### Basic Usage

```kotlin
// Glide
Glide.with(context)
    .load(url)
    .into(imageView)

// Picasso
Picasso.get()
    .load(url)
    .into(imageView)

// Coil
imageView.load(url)
```

### Custom Requests

```kotlin
imageView.scaleType = ImageView.ScaleType.FIT_CENTER

// Glide
Glide.with(context)
    .load(url)
    .placeholder(placeholder)
    .fitCenter()
    .into(imageView)

// Picasso
Picasso.get()
    .load(url)
    .placeholder(placeholder)
    .fit()
    .into(imageView)

// Coil (autodetects the scale type)
imageView.load(url) {
    placeholder(placeholder)
}
```

### Non-View Targets

```kotlin
// Glide (has optional callbacks for start and error)
Glide.with(context)
    .load(url)
    .into(object : CustomTarget<Drawable>() {
        override fun onResourceReady(resource: Drawable, transition: Transition<Drawable>) {
            // Handle the successful result.
        }

        override fun onLoadCleared(placeholder: Drawable) {
            // Remove the drawable provided in onResourceReady from any Views and ensure no references to it remain.
        }
    })

// Picasso
Picasso.get()
    .load(url)
    .into(object : BitmapTarget {
        override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom) {
            // Handle the successful result.
        }

        override fun onBitmapFailed(e: Exception, errorDrawable: Drawable?) {
            // Handle the error drawable.
        }

        override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
            // Handle the placeholder drawable.
        }
    })

// Coil
Coil.load(context)
    .data(url)
    .target(
        onStart = { drawable ->
            // Handle the placeholder drawable.
        },
        onSuccess = {
            // Handle the successful result.
        },
        onError = {
            // Handle the error drawable.
        }
    )
    .launch()
```

### Background Thread

```kotlin
// Glide (blocks the current thread; must not be called from the main thread)
val drawable = Glide.with(context)
    .load(url)
    .submit(width, height)
    .get()

// Picasso (blocks the current thread; must not be called from the main thread)
val drawable = Picasso.get()
    .load(url)
    .resize(width, height)
    .get()

// Coil (suspends the current context; thread safe)
val drawable = Coil.get(context)
    .data(url)
    .size(width, height)
    .launch()
```
