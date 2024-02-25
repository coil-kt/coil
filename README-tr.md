![Coil](logo.svg)

Android için Kotlin Coroutines tarafından desteklenen bir görüntü yükleme kütüphanesi. Coil:

- **Hızlıdır**: Coil bellek ve disk önbellekleme, bellekteki görüntüyü alt-örnekleme, biteşlemlerin tekrar kullanımı, isteklerin otomatik olarak durdurulması/iptali ve daha fazlasını içeren pek çok sayıda optimizasyon gerçekleştirir.
- **Hafiftir**: Coil, APK'nıza Picasso ile benzer ve Glide ve Fresco'dan önemli ölçüde daha az sayıda, 2000 civarında metod ekler.(Halihazırda OkHttp ve Coroutines kullanan uygulamalar için)
- **Kullanımı kolaydır**: Coil'in API'si basitlik ve mininum basmakalıp için Kotlin'in dil özelliklerini sonuna kadar kullanır.
- **Moderndir**: Coil Kotlin-önceliklidir ve Coroutines, OkHttp, Okio ve AndroidX Lifecycles gibi modern kütüphaneleri kullanır.


Coil şunların baş harflerinden oluşur: **Co**routine **I**mage **L**oader.

## Yükleme

Coil `mavenCentral()`'da mevcuttur.

```kotlin
implementation("io.coil-kt:coil:2.6.0")
```

## Hızlı Başlangıç

Görüntüyü `ImageView`'e yüklemek için `load` uzantı fonksiyonunu kullanın:

```kotlin
// URL
imageView.load("https://example.com/image.jpg")

// Resource
imageView.load(R.drawable.image)

// File
imageView.load(File("/path/to/image.jpg"))

// Ve daha fazlası...
```

İstekler tercihe bağlı bir takip eden lambda ile yapılandırılabilir:

```kotlin
imageView.load("https://example.com/image.jpg") {
    crossfade(true)
    placeholder(R.drawable.image)
    transformations(CircleCropTransformation())
}
```

#### Görüntü Yükleyiciler

`imageView.load` bir `ImageRequest`'i kuyruğa eklemek için yegane `ImageLoader` kullanır. Yegane `ImageLoader` uzantı fonksiyonu kullanılarak erişilebilir:

```kotlin
val imageLoader = context.imageLoader
```

İsteğe bağlı olarak, kendi `ImageLoader` kopya(ları)nızı oluşturabilir ve bağımlılık enjeksiyonu ile enjekte edebilirsiniz:

```kotlin
val imageLoader = ImageLoader(context)
```

Eğer yegane `ImageLoader` istemiyorsanız, `io.coil-kt:coil-base`'e bağlı kalabilirsiniz.

#### İstekler

Bir görüntüyü özel bir hedefe yüklemek için, bir `ImageRequest`'i `enqueue` edin:

```kotlin
val request = ImageRequest.Builder(context)
    .data("https://example.com/image.jpg")
    .target { drawable ->
        // Sonucu işleyin.
    }
    .build()
val disposable = imageLoader.enqueue(request)
```

Bir görüntüyü mecburi bir şekilde yüklemek için, bir `ImageRequest`'i `execute` edin:

```kotlin
val request = ImageRequest.Builder(context)
    .data("https://example.com/image.jpg")
    .build()
val drawable = imageLoader.execute(request).drawable
```

Coil'in [dokümantasyonunun tamamına buradan](https://coil-kt.github.io/coil/getting_started/) ulaşabilirsiniz.

## R8 / Proguard

Coil R8 ile tamamen uyumludur ve ek kurallar eklemeyi gerektirmez.

Eğer Proguard kullanıyorsanız, [Coroutines](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/resources/META-INF/proguard/coroutines.pro) ve [OkHttp](https://github.com/square/okhttp/blob/master/okhttp/src/jvmMain/resources/META-INF/proguard/okhttp3.pro) için kurallar eklemeniz gerekebilir.

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
