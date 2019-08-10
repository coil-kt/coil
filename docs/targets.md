# Targets

Targets handle the result of an image request. They often act as "view adapters" by taking the placeholder/error/success Drawables and applying them to a `View`.

Here's the easiest way to create a custom target:

```kotlin
Coil.load(context, "https://www.example.com/image.jpg") {
    target { drawable ->
        // Handle the successful result.
    }
}
```

There are 3 types of targets:

* [Target](../api/coil-base/coil.target/-target/): The base target class. Prefer this if the image request isn't tied to a `View`.
* [ViewTarget](../api/coil-base/coil.target/-view-target/): A target with an associated `View`. Prefer this if the request sets the placeholder/error/success Drawables on a `View`. Using `ViewTarget` also binds the request to the `View`'s lifecycle.
* [PoolableViewTarget](../api/coil-base/coil.target/-poolable-view-target/): A `ViewTarget` that supports [bitmap pooling](../getting_started/#bitmap-pooling). This has performance benefits, however it comes with several strict behavior requirements. Read the docs for more information.
