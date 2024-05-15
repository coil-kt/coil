![Coil](logo.svg)

Coil은 Kotlin Coroutines으로 만들어진 Android 백앤드 이미지 로딩 라이브러리입니다. Coil 은:

-   **빠르다**: Coil은 메모리와 디스크의 캐싱, 메모리의 이미지 다운 샘플링, Bitmap 재사용, 일시정지/취소의 자동화 등등 수 많은 최적화 작업을 수행합니다.
-   **가볍다**: Coil은 최대 2000개의 method들을 APK에 추가합니다(이미 OkHttp와 Coroutines을 사용중인 앱에 한하여), 이는 Picasso 비슷한 수준이며 Glide와 Fresco보다는 적습니다.
-   **사용하기 쉽다**: Coil API는 심플함과 최소한의 boilerplate를 위하여 Kotlin의 기능을 활용합니다.
-   **현대적이다**: Coil은 Kotlin 우선이며 Coroutines, OkHttp, Okio, AndroidX Lifecycles등의 최신 라이브러리를 사용합니다.

Coil은: **Co**routine **I**mage **L**oader의 약자입니다.

## 다운로드

Coil은 `mavenCentral()`로 이용 가능합니다.

```kotlin
implementation("io.coil-kt:coil:2.6.0")
```

## 빠른 시작

#### ImageViews

`ImageView`로 이미지를 불러오기 위해, `load` 확장 함수를 사용합니다.

```kotlin
// URL
imageView.load("https://example.com/image.jpg")

// File
imageView.load(File("/path/to/image.jpg"))

// And more...
```

Requests는 trailing lambda 식을 이용하여 추가 설정을 할 수 있습니다:

```kotlin
imageView.load("https://example.com/image.jpg") {
    crossfade(true)
    placeholder(R.drawable.image)
    transformations(CircleCropTransformation())
}
```

#### Jetpack Compose

[Jetpack Compose](https://developer.android.com/jetpack/compose) 확장 라이브러리 추가:

```kotlin
implementation("io.coil-kt:coil-compose:2.6.0")
```

이미지를 불러오려면, `AsyncImage` composable를 사용하세요:

```kotlin
AsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = null,
)
```

#### Image Loaders

`imageView.load` 와 `AsyncImage`는 이미지를 불러오기 위해 싱글톤 `ImageLoader`를 사용합니다. 싱글톤 `ImageLoader`는 `Context`의 확장함수를 통해 접근할 수 있습니다:

```kotlin
val imageLoader = context.imageLoader
```

`ImageLoader`는 공유가 가능하게 설계 되었으며, 싱글 객체를 만들어서 앱에 전반적으로 사용했을 때 가장 효율적입니다. 즉, 직접 `ImageLoader` 인스턴스를 생성해도 됩니다:

```kotlin
val imageLoader = ImageLoader(context)
```

싱글톤 `ImageLoader`를 사용하고 싶지 않을때에는, `io.coil-kt:coil`를 참조하는 대신, `io.coil-kt:coil-base`를 참조하세요.

#### Requests

커스텀 타겟에 이미지를 로드하려면, `ImageRequest`를 `enqueue` 하세요:

```kotlin
val request = ImageRequest.Builder(context)
    .data("https://example.com/image.jpg")
    .target { drawable ->
        // Handle the result.
    }
    .build()
val disposable = imageLoader.enqueue(request)
```

Imperative하게 이미지 로드를 하려면, `ImageRequest`를 `execute` 하세요:

```kotlin
val request = ImageRequest.Builder(context)
    .data("https://example.com/image.jpg")
    .build()
val drawable = imageLoader.execute(request).drawable
```

[여기서 Coil의 전체 문서](https://coil-kt.github.io/coil/)를 확인하세요.

## R8 / Proguard

Coil은 별도의 설정 없이 R8과 완벽하게 호환 가능하며 추가 규칙이 필요하지 않습니다.

Proguard를 사용할 경우, [Coroutines](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/resources/META-INF/proguard/coroutines.pro)와 [OkHttp](https://github.com/square/okhttp/blob/master/okhttp/src/jvmMain/resources/META-INF/proguard/okhttp3.pro)의 규칙을 추가할 필요가 있을 수 있습니다.

## 라이선스

    Copyright 2023 Coil Contributors

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
