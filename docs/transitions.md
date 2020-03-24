# Transitions

Transitions allow you to animate setting the result of an image request on a `Target`.

Both `ImageLoader` and `LoadRequest` builders accept a `Transition`. Transitions allow you to intercept setting the `Drawable` on the `Target`. This allows you to animate the target's view or wrap the input drawable.

By default, Coil comes packaged with 1 transition: [crossfade](../api/coil-base/coil.transition/-crossfade-transition/). Take a look at the [`CrossfadeTransition`](https://github.com/coil-kt/coil/blob/master/coil-base/src/main/java/coil/transition/CrossfadeTransition.kt) source code for an example of how to write a custom `Transition`.

See the [API doc](../api/coil-base/coil.transition/-transition/) for more information.
