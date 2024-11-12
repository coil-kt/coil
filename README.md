![Coil](logo.svg)

An image loading library for [Android](https://www.android.com/) and [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/). Coil is:

- **Fast**: Coil performs a number of optimizations including memory and disk caching, downsampling the image, automatically pausing/cancelling requests, and more.
- **Lightweight**: Coil only depends on Kotlin, Coroutines, and Okio and works seamlessly with Google's R8 code shrinker.
- **Easy to use**: Coil's API leverages Kotlin's language features for simplicity and minimal boilerplate.
- **Modern**: Coil is Kotlin-first and interoperates with modern libraries including Compose, Coroutines, Okio, OkHttp, and Ktor.

Coil is an acronym for: **Co**routine **I**mage **L**oader.

Translations: [日本語](README-ja.md), [한국어](README-ko.md), [Русский](README-ru.md), [Svenska](README-sv.md), [Türkçe](README-tr.md), [中文](README-zh.md)

## Quick Start

Import the Compose library and a [networking library](https://coil-kt.github.io/coil/network/):

```kotlin
implementation("io.coil-kt.coil3:coil-compose:3.0.2")
implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.2")
```
> **⚠️ Important:** If you use Compose Multiplatform, you'll need to use Ktor instead of OkHttp. See [here](https://coil-kt.github.io/coil/network/#ktor-network-engines) for how to do that.

To load an image, use the `AsyncImage` composable:

```kotlin
AsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = null,
)
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
