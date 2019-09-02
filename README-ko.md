![Coil](logo.svg)

Coil은 Android 백앤드를 위해 Kotlin Coroutines으로 만들어진 이미지 로딩 라이브러리입니다. Coil 은:

- **빠르다**: Coil은 메모리와 디스크 캐싱, 메모리의 이미지 다운 샘플링, Bitmap 재사용, 자동 요청사항 일시정지/취소 등의 수많은 최적화를 수행합니다.
- **가볍다**: Coil은 최댁 1500개의 method들을 APK에 추가합니다 OkHttp와 Coroutines을 이미 사용 중인 앱), 이는 Picasso 비슷한 수준이며 Glide와 Fresco. 보다는 적습니다.
- **사용하기 쉽다**: Coil의 API는 심플함과 최소한의 boilerplate를 위해 Kotlin의 언어 기능을 활용합니다.
- **현대적이다**: Coil은 Kotlin 우선이며 Coroutines, OkHttp, Okio, AndroidX Lifecycles등의 최신 라이브러리를 사용합니다.

Coil은: **Co**routine **I**mage **L**oader의 약자입니다.

❤️[Instacart](https://www.instacart.com)에서 ❤️으로 만들었습니다.

## 다운로드

Coil은`mavenCentral()`로 이용 가능합니다.

```kotlin
implementation("io.coil-kt:coil:0.6.1")
```

## 빠른 시작

`ImageView`에 이미지를 불러오기 위해, `load` Extension functions을 사용합니다.

```kotlin
// URL
imageView.load("https://www.example.com/image.jpg")

// Resource
imageView.load(R.drawable.image)

// File
imageView.load(File("/path/to/image.jpg"))

// And more...
```

Requests는 추가적인 trailing lambda식 으로 구성될 수 있습니다:

```kotlin
imageView.load("https://www.example.com/image.jpg") {
    crossfade(true)
    placeholder(R.drawable.image)
    transformations(CircleCropTransformation())
}
```

이미지를 급하게 가져오기 위해서, `get` [suspend](https://kotlinlang.org/docs/reference/coroutines/basics.html) function을 사용합니다:

```kotlin
val drawable = Coil.get("https://www.example.com/image.jpg")
```

Coil's [전체 문서](https://coil-kt.github.io/coil/)를 여기서 확인하세요.

## 요구사항

- AndroidX
- Min SDK 14+
- Compile SDK: 28+
- Java 8+

## R8 / Proguard

Coil은 별도의 설정 없이 R8과 완벽하게 호환 가능하며 추가 규칙이 필요하지 않습니다.

Proguard를 사용할 경우, [Coroutines](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/resources/META-INF/proguard/coroutines.pro), [OkHttp](https://github.com/square/okhttp/blob/master/okhttp/src/main/resources/META-INF/proguard/okhttp3.pro), [Okio](https://github.com/square/okio/blob/master/okio/src/jvmMain/resources/META-INF/proguard/okio.pro)에 규칙을 추가할 필요가 있을 수 있습니다.

## 라이선스

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
