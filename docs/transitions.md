# Transitions

Transitions allow you to animate setting the result of an image request on a `Target`.

Both `ImageLoader` and `ImageRequest` builders accept a `Transition.Factory`. Transitions allow you to control how the success/error drawable is set on the `Target`. This allows you to animate the target's view or wrap the input drawable.

By default, Coil comes packaged with 2 transitions:

- [`CrossfadeTransition.Factory`](/coil/api/coil-core/coil3.transition/-crossfade-transition/) which crossfades from the current drawable to the success/error drawable.
- [`Transition.Factory.NONE`](/coil/api/coil-core/coil3.transition/-transition/-factory/-companion/-n-o-n-e) which sets the drawable on the `Target` immediately without animating.

Take a look at the [`CrossfadeTransition` source code](https://github.com/coil-kt/coil/blob/main/coil-core/src/main/java/coil/transition/CrossfadeTransition.kt) for an example of how to write a custom `Transition`.

See the [API documentation](/coil/api/coil-core/coil3.transition/-transition/) for more information.
