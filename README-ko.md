![Coil](logo.svg)

Coil은 Android 백앤드를 위해 Kotlin Coroutines으로 만들어진 이미지 로딩 라이브러리입니다. Coil 은:

- **빠르다**: Coil은 메모리와 디스크 캐싱, 메모리의 이미지 다운 샘플링, Bitmap 재사용, 자동 요청사항 일시정지/취소 등의 수많은 최적화를 수행합니다.
- **가볍다**: Coil은 최댁 1500개의 method들을 APK에 추가합니다 OkHttp와 Coroutines을 이미 사용 중인 앱), 이는 Picasso 비슷한 수준이며 Glide와 Fresco. 보다는 적습니다.
- **사용하기 쉽다**: Coil의 API는 leverages Kotlin's language features for simplicity and minimal boilerplate.
- **현대적이다**: Coil is Kotlin-first and uses modern libraries including Coroutines, OkHttp, Okio, and AndroidX Lifecycles.

Coil은: **Co**routine **I**mage **L**oader의 약자입니다.

Made with ❤️ at [Instacart](https://www.instacart.com).

## Download

Coil is available on `mavenCentral()`.

```kotlin
implementation("io.coil-kt:coil:0.6.1")
```

## Quick Start

To load an image into an `ImageView`, use the `load` extension function:

```kotlin
// URL
imageView.load("https://www.example.com/image.jpg")

// Resource
imageView.load(R.drawable.image)

// File
imageView.load(File("/path/to/image.jpg"))

// And more...
```

Requests can be configured with an optional trailing lambda:

```kotlin
imageView.load("https://www.example.com/image.jpg") {
    crossfade(true)
    placeholder(R.drawable.image)
    transformations(CircleCropTransformation())
}
```

To get an image imperatively, use the `get` [suspend](https://kotlinlang.org/docs/reference/coroutines/basics.html) function:

```kotlin
val drawable = Coil.get("https://www.example.com/image.jpg")
```

Check out Coil's [full documentation here](https://coil-kt.github.io/coil/).

## Requirements

- AndroidX
- Min SDK 14+
- Compile SDK: 28+
- Java 8+

## R8 / Proguard

Coil is fully compatible with R8 out of the box and doesn't require adding any extra rules.

If you use Proguard, you may need to add rules for [Coroutines](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/resources/META-INF/proguard/coroutines.pro), [OkHttp](https://github.com/square/okhttp/blob/master/okhttp/src/main/resources/META-INF/proguard/okhttp3.pro) and [Okio](https://github.com/square/okio/blob/master/okio/src/jvmMain/resources/META-INF/proguard/okio.pro).

## License

    Copyright 2019 Coil Contributors

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
