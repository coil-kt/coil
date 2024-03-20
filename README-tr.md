![Coil](logo.svg)

Kotlin Coroutines tarafından desteklenen Android için bir görüntü yükleme kütüphanesi. Coil şunlardır:

- **Hızlı**: Coil, bellek ve disk önbellekleme, bellekteki görüntünün örnekleme yapılması, otomatik olarak isteklerin durdurulması/iptal edilmesi ve daha fazlası dahil olmak üzere bir dizi optimizasyon gerçekleştirir.
- **Hafif**: Coil, APK'nıza ~2000 yöntem ekler (zaten OkHttp ve Coroutines kullanan uygulamalar için), bu da Picasso ile karşılaştırılabilir ve Glide ve Fresco'dan önemli ölçüde daha azdır.
- **Kullanımı Kolay**: Coil'in API'si, basitlik ve minimum kod tekrarı için Kotlin'in dil özelliklerinden yararlanır.
- **Modern**: Coil, öncelikle Kotlin'e dayanır ve Coroutines, OkHttp, Okio ve AndroidX Lifecycle gibi modern kütüphaneleri kullanır.

Coil, **Co**routine **I**mage **L**oader'ın kısaltmasıdır.

Çeviriler: [日本語](README-ja.md), [한국어](README-ko.md), [Русский](README-ru.md), [Svenska](README-sv.md), [Türkçe](README-tr.md), [中文](README-zh.md)

## İndirme

Coil, `mavenCentral()` üzerinde mevcuttur.

```kotlin
implementation("io.coil-kt:coil:2.6.0")
```

## Hızlı Başlangıç

#### ImageViews

Bir görüntüyü bir `ImageView`'a yüklemek için `load` genişletme fonksiyonunu kullanın:

```kotlin
// URL
imageView.load("https://example.com/image.jpg")

// Dosya
imageView.load(File("/path/to/image.jpg"))

// Ve daha fazlası...
```

İstekler, isteğe bağlı bir kapanan lambda ile yapılandırılabilir:

```kotlin
imageView.load("https://example.com/image.jpg") {
    crossfade(true)
    placeholder(R.drawable.image)
    transformations(CircleCropTransformation())
}
```

#### Jetpack Compose

[Jetpack Compose](https://developer.android.com/jetpack/compose) uzantı kütüphanesini içe aktarın:

```kotlin
implementation("io.coil-kt:coil-compose:2.6.0")
```

Bir görüntü yüklemek için, `AsyncImage` bileşenini kullanın:

```kotlin
AsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = null,
)
```

#### Görüntü Yükleyiciler

`imageView.load` ve `AsyncImage` hem görüntü isteklerini yürütmek için singleton `ImageLoader`'ı kullanır. Singleton `ImageLoader`'a bir `Context` genişletme fonksiyonu kullanarak erişilebilir:

```kotlin
val imageLoader = context.imageLoader
```

`ImageLoader`'lar paylaşılabilir olarak tasarlanmıştır ve uygulamanız boyunca tek bir örnek oluşturup paylaştığınızda en verimlidir. Bununla birlikte, kendi `ImageLoader` örneğinizi de oluşturabilirsiniz:

```kotlin
val imageLoader = ImageLoader(context)
```

Eğer singleton `ImageLoader` istemiyorsanız, `io.coil-kt:coil` yerine `io.coil-kt:coil-base` bağımlılığını kullanın.

#### İstekler

Bir görüntüyü özel bir hedefe yüklemek için bir `ImageRequest`'i `enqueue` edin:

```kotlin
val request = ImageRequest.Builder(context)
    .data("https://example.com/image.jpg")
    .target { drawable ->
        // Sonucu işleyin.
    }
    .build()
val disposable = imageLoader.enqueue(request)
```

Bir görüntüyü emperatif olarak yüklemek için bir `ImageRequest`'i `execute` edin:

```kotlin
val request = ImageRequest.Builder(context)
    .data("https://example.com/image.jpg")
    .build()
val drawable = imageLoader.execute(request).drawable
```

Coil'in [tam belgelerini buradan](https://coil-kt.github.io/coil/getting_started/) inceleyin.

## R8 / Proguard

Coil, R8 ile uyumlu olarak kutudan çıkar ve ekstra kurallar eklemeyi gerektirmez.

Eğer Proguard kullanıyorsanız, [Coroutines](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/resources/META-INF/proguard/coroutines.pro) ve [OkHttp](https://github.com/square/okhttp/blob/master/okhttp/src/main/resources/META-INF/proguard/okhttp3.pro) için kurallar eklemeniz gerekebilir.

## Lisans

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
