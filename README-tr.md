![Coil](logo.svg)

[Android](https://www.android.com/) ve [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) için bir resim yükleme kütüphanesi. Coil:

- **Hızlı**: Coil, bellek ve disk önbelleğe alma, resmin alt örneklemesini yapma, istekleri otomatik olarak duraklatma/iptal etme ve daha fazlası dahil olmak üzere bir dizi optimizasyon gerçekleştirir.
- **Hafif**: Coil yalnızca Kotlin, Coroutines ve Okio'ya bağlıdır ve Google'ın R8 kod küçültücüsüyle sorunsuz bir şekilde çalışır.
- **Kullanımı kolay**: Coil'in API'si, basitlik ve minimum kalıp için Kotlin'in dil özelliklerini kullanır.
- **Modern**: Coil, Kotlin önceliklidir ve Compose, Coroutines, Okio, OkHttp ve Ktor gibi modern kütüphanelerle birlikte çalışır.

Coil, **Co**routine **I**mage **L**oader'ın kısaltmasıdır.

## Hızlı Başlangıç

Compose kütüphanesini ve bir [ağ kütüphanesini](https://coil-kt.github.io/coil/network/) içe aktarın:

```kotlin
implementation("io.coil-kt.coil3:coil-compose:3.2.0")
implementation("io.coil-kt.coil3:coil-network-okhttp:3.2.0")
```

Bir görüntüyü yüklemek için, `AsyncImage` bileşenini kullanın:

```kotlin
AsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = null,
)
```

Coil'in [tam belgelerine buradan](https://coil-kt.github.io/coil/getting_started/) göz atın.

## Lisans

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
