![Spole](logo.svg)

Ett bildbibliotek för [Android](https://www.android.com/) och [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/). Spolen är:

- **Snabb**: Coil utför ett antal optimeringar inklusive minne och diskcache, nedsampling av bilden, automatisk paus/avbryt förfrågningar och mer.
- **Lättvikt**: Spolen beror bara på Kotlin, Coroutines och Okio och fungerar sömlöst med Googles R8-kodkrympare.
- **Lätt att använda**: Coils API utnyttjar Kotlins språkfunktioner för enkelhet och minimal konstruktion.
- **Modern**: Coil är Kotlin-först och samverkar med moderna bibliotek inklusive Compose, Coroutines, Okio, OkHttp och Ktor.

Spole är en akronym för: **Co**rutin **I**bild **L**loader.

## Snabbstart

Importera Compose-biblioteket och ett [nätverksbibliotek](https://coil-kt.github.io/coil/network/):

```kotlin
implementering("io.coil-kt.coil3:coil-compose:3.0.1")
implementering("io.coil-kt.coil3:coil-network-okhttp:3.0.1")
```

För att ladda en bild, använd "AsyncImage" komponerbar:

```kotlin
AsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = null,
)
```

Kolla in Coils [fullständiga dokumentation här](https://coil-kt.github.io/coil/getting_started/).

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
