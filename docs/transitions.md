# Transitions

Transformations allow you to animate the result of an image request.

Both `ImageLoader` and `Request` builders accept a `Transition.Factory`, which enables you to return a new transition (or null for no transition) for each `Transition.Event`.

By default, Coil comes packaged with 1 transformation: [crossfade](../api/coil-base/coil.transition/-crossfade-transition/).

See the [API doc](../api/coil-base/coil.transition/-transition/) for more information.
