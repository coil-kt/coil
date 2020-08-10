![Coil](logo.svg)

Coil是一个Android图片加载库，通过Kotlin协程的方式加载图片。
An image loading library for Android backed by Kotlin Coroutines. Coil is:

- **更快**: Coil在性能上有很多优化包括内存缓存和磁盘缓存，把缩略图存保存在内存中，循环利于bitmap，自动暂停和取消图片网络请求等。
- **更轻量级**: Coil 只有2000个方法（前提是你的APP里面集成了OkHttp和Coroutines），Coil和Picasso的方法数差不多相比Glide和Fresco要轻量级很多。
- **更容易使用**: Coil's API 充分利用了Kotlin语言的新特性简化和减少了很多重复的代码。
- **更流行**: Coil首选Kotlin语言开发并且使用包含Coroutines, OkHttp, Okio和AndroidX Lifecycles在内的最流行的开源库。

Coil的首字母由来：取**Co**routine，**I**mage和**L**oader得来Coil。

用❤️服务于[Instacart](https://www.instacart.com).

## 下载

Coil允许使用`mavenCentral()`.

```kotlin
implementation("io.coil-kt:coil:0.11.0")
```

## 快速使用

可以使用`ImageView`的扩展函数`load` 加载一张图片：
```kotlin
// URL
imageView.load("https://www.example.com/image.jpg")

// Resource
imageView.load(R.drawable.image)

// File
imageView.load(File("/path/to/image.jpg"))

// And more...
```


可以使用lambda语法轻松配置请求选项：

```kotlin
imageView.load("https://www.example.com/image.jpg") {
    crossfade(true)
    placeholder(R.drawable.image)
    transformations(CircleCropTransformation())
}
```

#### 图片加载
`imageView.load`使用单例`ImageLoader`来执行`LoadRequest`。您也可以通过以下方式获得单例。
```kotlin
val imageLoader = Coil.imageLoader(context)
```

您也可以通过依赖注入的方式创建您自己的`ImageLoader`：
```kotlin
val imageLoader = ImageLoader(context)
```

如果您不需要单例`ImageLoader`，只需要添加依赖：`io.coil-kt:coil-base`。


#### 请求

通过`LoadRequest`加载自定义标签:

```kotlin
val request = LoadRequest.Builder(context)
    .data("https://www.example.com/image.jpg")
    .target { drawable ->
        // Handle the result.
    }
    .build()
imageLoader.execute(request)
```

也可以通过`GetRequest`获得一个drawable对象：

```kotlin
val request = GetRequest.Builder(context)
    .data("https://www.example.com/image.jpg")
    .build()
val drawable = imageLoader.execute(request).drawable
```

也可以查看Coli文档获得更多信息： [full documentation here](https://coil-kt.github.io/coil/getting_started/).

## 环境要求

- AndroidX
- Min SDK 14+
- [Java 8+](https://coil-kt.github.io/coil/getting_started/#java-8)

## R8 / Proguard

Coil兼容R8混淆您无需在添加其他的规则 

如果您需要混淆代码，你可能需要添加对应的混淆规则：[Coroutines](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/resources/META-INF/proguard/coroutines.pro), [OkHttp](https://github.com/square/okhttp/blob/master/okhttp/src/main/resources/META-INF/proguard/okhttp3.pro) and [Okio](https://github.com/square/okio/blob/master/okio/src/jvmMain/resources/META-INF/proguard/okio.pro)。

## License

    Copyright 2020 Coil Contributors

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
