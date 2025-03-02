![Coil](logo.svg)

适用于 [Android](https://www.android.com/) 和 [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) 的图像加载库。Coil 具有以下特点：

- **快速**：Coil 执行多项优化，包括内存和磁盘缓存、图像降采样、自动暂停/取消请求等。
- **轻量**：Coil 仅依赖于 Kotlin、Coroutines 和 Okio，可与 Google 的 R8 代码压缩器无缝协作。
- **易于使用**：Coil 的 API 利用 Kotlin 的语言功能实现简单性并减少样板代码。
- **现代**：Coil 是 Kotlin 优先的，可与包括 Compose、Coroutines、Okio、OkHttp 和 Ktor 在内的现代库互操作。

Coil 是 Co**routine **I**mage **L**oader 的缩写。

## 快速入门

导入 Compose 库和 [网络库](https://coil-kt.github.io/coil/network/)：

```kotlin
implementation("io.coil-kt.coil3:coil-compose:3.1.0")
implementation("io.coil-kt.coil3:coil-network-okhttp:3.1.0")
```

要加载图像，请使用 `AsyncImage`可组合：

```kotlin
AsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = null,
)
```

查看 Coil 的[完整文档](https://coil-kt.github.io/coil/getting_started/)。

## License

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
