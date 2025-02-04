![Coil](logo.svg)

[Android](https://www.android.com/) および [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) 用の画像読み込みライブラリ。Coil の特徴:

- **高速**: Coil は、メモリとディスクのキャッシュ、画像のダウンサンプリング、リクエストの自動一時停止/キャンセルなど、さまざまな最適化を実行します。
- **軽量**: Coil は Kotlin、Coroutines、Okio のみに依存し、Google の R8 コード シュリンカーとシームレスに連携します。
- **使いやすい**: Coil の API は、シンプルさと最小限の定型文を実現するために Kotlin の言語機能を活用しています。
- **最新**: Coil は Kotlin ファーストであり、Compose、Coroutines、Okio、OkHttp、Ktor などの最新のライブラリと相互運用できます。

Coil は、**Co**routine **I**mage **L**oader の頭字語です。

## クイックスタート

Compose ライブラリと [ネットワーク ライブラリ](https://coil-kt.github.io/coil/network/) をインポートします:

```kotlin
implementation("io.coil-kt.coil3:coil-compose:3.1.0")
implementation("io.coil-kt.coil3:coil-network-okhttp:3.1.0")
```

画像を読み込むには、`AsyncImage` を使用しますcomposable:

```kotlin
AsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = null,
)
```

Coil の [完全なドキュメントはこちら](https://coil-kt.github.io/coil/getting_started/) をご覧ください。

## ライセンス

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
