﻿![Coil](logo.svg)

An image loading library for Android and [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/). Coil is:

- **Fast**: Coil performs a number of optimizations including memory and disk caching, downsampling the image, automatically pausing/cancelling requests, and more.
- **Lightweight**: Coil only depends on Kotlin, Coroutines, and Okio and works seamlessly with code shrinkers like [Google's R8](https://developer.android.com/build/shrink-code).
- **Easy to use**: Coil's API leverages Kotlin's language features for simplicity and minimal boilerplate.
- **Modern**: Coil is Kotlin-first and interoperates with modern libraries including Coroutines, Okio, Ktor, and OkHttp.

Coil is an acronym for: **Co**routine **I**mage **L**oader.

Translations: [日本語](README-ja.md), [한국어](README-ko.md), [Русский](README-ru.md), [Svenska](README-sv.md), [Türkçe](README-tr.md), [中文](README-zh.md)

## Download

Coil is available on `mavenCentral()`.

```kotlin
implementation("io.coil-kt:coil:2.7.0")
```

## Quick Start

#### Compose

Import the [Compose](https://developer.android.com/jetpack/compose) extension library:

```kotlin
implementation("io.coil-kt:coil-compose:2.7.0")
```

To load an image, use the `AsyncImage` composable:

```kotlin
AsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = null,
)
```

#### ImageViews

To load an image into an `ImageView`, use the `load` extension function:

```kotlin
imageView.load("https://example.com/image.jpg")
```

Requests can be configured with an optional trailing lambda:

```kotlin
imageView.load("https://example.com/image.jpg") {
    crossfade(true)
    placeholder(R.drawable.image)
}
```

Check out Coil's [full documentation here](https://coil-kt.github.io/coil/getting_started/).

## License

    Copyright 2024 Coil Contributors

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
