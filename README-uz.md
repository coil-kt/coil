![Coil](logo.svg)

[Android](https://www.android.com/) va [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) uchun rasm yuklash kutubxonasi. Coil quyidagi xususiyatlarga ega:

- **Tez**: Coil bir qancha optimallashtirishlarni amalga oshiradi: xotira va disk keshini ishlatadi, rasmlarni kichraytiradi (downsampling), so‘rovlarni avtomatik to‘xtatadi yoki bekor qiladi va boshqalar.
- **Yengil**: Coil faqat Kotlin, Coroutine'lar va Okio'ga bog‘liq bo‘lib, Google'ning R8 kod qisqartiruvchisi bilan to‘liq mos ishlaydi.
- **Foydalanish oson**: Coil API'lari Kotlin tilining imkoniyatlaridan foydalanib, minimal kod bilan ishlashni ta'minlaydi.
- **Zamonaviy**: Coil Kotlin-birinchi yondashuvga asoslangan va Compose, Coroutines, Okio, OkHttp, va Ktor kabi zamonaviy kutubxonalar bilan mos ishlaydi.

Coil so‘zi qisqartma bo‘lib, quyidagini anglatadi: **Co**routine **I**mage **L**oader.

## Foylanishni boshlash

Coil Compose kutubxonasi va [tarmoq kutubxonasini](https://coil-kt.github.io/coil/network/) yuklang:

```kotlin
implementation("io.coil-kt.coil3:coil-compose:3.3.0")
implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")
```

Rasm yuklash uchun `AsyncImage` composable'dan foydalaning:

```kotlin
AsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = null,
)
```
Coil haqidagi [to‘liq dokumentatsiya]((https://coil-kt.github.io/coil/getting_started/))ni ko'rib chiqing.

## Litsenziya

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
