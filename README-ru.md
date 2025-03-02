![Coil](logo.svg)

Библиотека загрузки изображений для [Android](https://www.android.com/) и [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/). Coil:

- **Быстрая**: Coil выполняет ряд оптимизаций, включая кэширование памяти и диска, понижение разрешения изображения, автоматическую приостановку/отмену запросов и многое другое.
- **Легкая**: Coil зависит только от Kotlin, Coroutines и Okio и без проблем работает с укоротителем кода R8 от Google.
- **Простая в использовании**: API Coil использует возможности языка Kotlin для простоты и минимального шаблона.
- **Современная**: Coil ориентирована на Kotlin и взаимодействует с современными библиотеками, включая Compose, Coroutines, Okio, OkHttp и Ktor.

Coil — это аббревиатура от: **Co**routine **I**mage **L**loader.

## Быстрый старт

Импортируйте библиотеку Compose и [сетевую библиотеку](https://coil-kt.github.io/coil/network/):

```kotlin
implementation("io.coil-kt.coil3:coil-compose:3.1.0")
implementation("io.coil-kt.coil3:coil-network-okhttp:3.1.0")
```

Чтобы загрузите изображение, используйте `AsyncImage` composable:

```kotlin
AsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = null,
)
```

Ознакомьтесь с [полной документацией Coil] здесь (https://coil-kt.github.io/coil/getting_started/).

## Лицензия

    Copyright 2025 Coil Contributors

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
