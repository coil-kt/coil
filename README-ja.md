![Coil](logo.svg)

Coil は Kotlin Coroutines で作られた Android 用の画像読み込みライブラリです。 Coil は:

- **高速**: Coil は、メモリとディスクのキャッシング、メモリ内の画像のダウンサンプリング、リクエストの一時停止/キャンセルの自動化など、多くの最適化を実行します。
- **軽量**: Coil は ~2000 のメソッドを APK に追加します (すでに OkHttp と Coroutines を使用しているアプリの場合)。これは Picasso に匹敵し、Glide や Fresco よりも大幅に少ない数です。
- **使いやすい**: Coil の API は、ボイラープレートの最小化とシンプルさのために Kotlin の言語機能を活用しています。
- **現代的**: Coil は Kotlin ファーストで、Coroutines、OkHttp、Okio、AndroidX Lifecycles などの最新のライブラリを使用します。

Coil は **Co**routine **I**mage **L**oader の頭字語です。

## ダウンロード

Coil は `mavenCentral()` で利用できます。

```kotlin
implementation("io.coil-kt:coil:2.6.0")
```

## クイックスタート

#### ImageViews

画像を `ImageView` に読み込むには、 `load` 拡張関数を使用します。

```kotlin
// URL
imageView.load("https://example.com/image.jpg")

// File
imageView.load(File("/path/to/image.jpg"))

// And more...
```

Requests は、 trailing lambda 式を使用して追加の設定を行うことができます:

```kotlin
imageView.load("https://example.com/image.jpg") {
    crossfade(true)
    placeholder(R.drawable.image)
    transformations(CircleCropTransformation())
}
```

#### Jetpack Compose

[Jetpack Compose](https://developer.android.com/jetpack/compose) 拡張ライブラリをインポートします:

```kotlin
implementation("io.coil-kt:coil-compose:2.6.0")
```

画像を読み込むには、`AsyncImage` composable を使用します:

```kotlin
AsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = null,
)
```

#### Image Loaders

`imageView.load` と `AsyncImage` はシングルトンの `ImageLoader` を使用して画像リクエストを実行します。 シングルトンの `ImageLoader` には `Context` 拡張関数を使用してアクセスできます:

```kotlin
val imageLoader = context.imageLoader
```

`ImageLoader` は共有できるように設計されており、単一のインスタンスを作成してアプリ全体で共有すると最も効率的です。 また、独自の `ImageLoader` インスタンスを作成することもできます:

```kotlin
val imageLoader = ImageLoader(context)
```

シングルトンの `ImageLoader` が必要ない場合は、 `io.coil-kt:coil` の代わりに `io.coil-kt:coil-base` を使用してください。

#### Requests

画像をカスタムターゲットにロードするには、 `ImageRequest` を `enqueue` してください:

```kotlin
val request = ImageRequest.Builder(context)
    .data("https://example.com/image.jpg")
    .target { drawable ->
        // Handle the result.
    }
    .build()
val disposable = imageLoader.enqueue(request)
```

画像を命令的にロードするには、 `ImageRequest` を `execute` してください:

```kotlin
val request = ImageRequest.Builder(context)
    .data("https://example.com/image.jpg")
    .build()
val drawable = imageLoader.execute(request).drawable
```

[こちらで Coil の完全なドキュメント](https://coil-kt.github.io/coil/getting_started/) を確認してください。

## R8 / Proguard

Coil は R8 と完全に互換性があり、追加のルールを追加する必要はありません。

Proguardを使用している場合は、[Coroutines](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/resources/META-INF/proguard/coroutines.pro)、[OkHttp](https://github.com/square/okhttp/blob/master/okhttp/src/jvmMain/resources/META-INF/proguard/okhttp3.pro)にルールを追加する必要があるかもしれません。

## ライセンス

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
