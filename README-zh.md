![Coil](logo.svg)

Coil 是一个 Android 图片加载库，通过 Kotlin 协程的方式加载图片。特点如下：

- **更快**: Coil 在性能上有很多优化，包括内存缓存和磁盘缓存，把缩略图存保存在内存中，循环利用 bitmap，自动暂停和取消图片网络请求等。
- **更轻量级**: Coil 只有2000个方法（前提是你的 APP 里面集成了 OkHttp 和 Coroutines），Coil 和 Picasso 的方法数差不多，相比 Glide 和 Fresco 要轻量很多。
- **更容易使用**: Coil 的 API 充分利用了 Kotlin 语言的新特性，简化和减少了很多样板代码。
- **更流行**: Coil 首选 Kotlin 语言开发并且使用包含 Coroutines, OkHttp, Okio 和 AndroidX Lifecycles 在内最流行的开源库。

Coil 名字的由来：取 **Co**routine **I**mage **L**oader 首字母得来。

在 [Instacart](https://www.instacart.com) 用❤️打造。

## 下载

Coil 可以在 `mavenCentral()` 下载

```kotlin
implementation("io.coil-kt:coil:1.3.0")
```

## 快速上手

可以使用 `ImageView` 的扩展函数 `load` 加载一张图片：
```kotlin
// URL
imageView.load("https://www.example.com/image.jpg")

// Resource
imageView.load(R.drawable.image)

// File
imageView.load(File("/path/to/image.jpg"))

// And more...
```

可以使用 lambda 语法轻松配置请求选项：

```kotlin
imageView.load("https://www.example.com/image.jpg") {
    crossfade(true)
    placeholder(R.drawable.image)
    transformations(CircleCropTransformation())
}
```

也可以查看 Coil 的[完整文档](https://coil-kt.github.io/coil/getting_started/)获得更多信息。

## 环境要求

- AndroidX
- Min SDK 21+
- [Java 8+](https://coil-kt.github.io/coil/getting_started/#java-8)

## R8 / Proguard

Coil 兼容 R8 混淆，您无需再添加其他的规则

如果您需要混淆代码，可能需要添加对应的混淆规则：[Coroutines](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/resources/META-INF/proguard/coroutines.pro), [OkHttp](https://github.com/square/okhttp/blob/master/okhttp/src/main/resources/META-INF/proguard/okhttp3.pro), [Okio](https://github.com/square/okio/blob/master/okio/src/jvmMain/resources/META-INF/proguard/okio.pro)。

## License

    Copyright 2021 Coil Contributors

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
