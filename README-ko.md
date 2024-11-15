![Coil](logo.svg)

[Android](https://www.android.com/) 및 [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)을 위한 이미지 로딩 라이브러리입니다. Coil의 특징은 다음과 같습니다.

- **빠름**: Coil은 메모리 및 디스크 캐싱, 이미지 다운샘플링, 요청 자동 일시 중지/취소 등 여러 가지 최적화를 수행합니다.
- **가벼움**: Coil은 Kotlin, Coroutines, Okio에만 의존하며 Google의 R8 코드 축소기와 원활하게 작동합니다.
- **사용하기 쉬움**: Coil의 API는 Kotlin의 언어 기능을 활용하여 단순성과 최소한의 보일러플레이트를 제공합니다.
- **현대적**: Coil은 Kotlin을 우선으로 하며 Compose, Coroutines, Okio, OkHttp, Ktor를 포함한 최신 라이브러리와 상호 운용됩니다.

코일은 **Co**routine **I**mage **L**oader**의 약자입니다.

## 빠른 시작

Compose 라이브러리와 [네트워킹 라이브러리](https://coil-kt.github.io/coil/network/ 가져오기:

```kotlin
구현("io.coil-kt.coil3:coil-compose:3.0.3")
구현("io.coil-kt.coil3:coil-network-okhttp:3.0.3")
```

이미지를 로드하려면 `AsyncImage`를 사용하세요. 구성 가능:

```kotlin
AsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = null,
)
```

Coil의 [전체 문서는 여기에서](https://coil-kt.github.io/coil/getting_started/)에서 확인하세요.

## 라이선스

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
