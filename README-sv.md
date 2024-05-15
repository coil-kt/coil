![Coil](logo.svg)

Ett bildladdningsbibliotek för Android med stöd för Kotlin Coroutines. Coil är:

- **Snabbt**: Coil utför ett antal optimeringar inklusive minne och diskcache, nedsampling av bilden i minnet, automatisk paus/avbryt förfrågningar och mer.
- **Effektivt och optimerat**: Coil lägger till ~2000 metoder till din APK (för appar som redan använder OkHttp och Coroutines), vilket är jämförbart med Picasso och betydligt mindre än Glide och Fresco.
- **Enkelt att använda**: Coils API utnyttjar Kotlins språkfunktioner för enkelhet och minimal boilerplate kod.
- **Modernt**: Coil är skapat för Kotlin i första hand och använder moderna bibliotek inklusive Coroutines, OkHttp, Okio och AndroidX Lifecycles.

Coil är en förkortning för: **Co**routine **I**mage **L**oader.

Översättningar: [한국어](README-ko.md), [中文](README-zh.md), [Türkçe](README-tr.md), [日本語](README-ja.md), [Svenska](README-sv.md)

## Hämta

Coil finns att ladda ned från `mavenCentral()`.

```kotlin
implementation("io.coil-kt:coil:2.6.0")
```

## Snabbstartsguide

#### ImageViews

För att ladda in en bild i en `ImageView`, använd förlängningsfunktionen `load`:

```kotlin
// URL
imageView.load("https://example.com/image.jpg")

// Fil
imageView.load(File("/path/to/image.jpg"))

// Och mer...
```

Förfrågningar kan konfigureras med en valfri släpande lambda:

```kotlin
imageView.load("https://example.com/image.jpg") {
    crossfade(true)
    placeholder(R.drawable.image)
    transformations(CircleCropTransformation())
}
```

#### Jetpack Compose

Importera [Jetpack Compose](https://developer.android.com/jetpack/compose)-förlängningsbiblioteket:

```kotlin
implementation("io.coil-kt:coil-compose:2.6.0")
```

För att ladda in en bild, använd en `AsyncImage` composable:

```kotlin
AsyncImage(
    model = "https://example.com/image.jpg",
    contentDescription = null,
)
```

#### Bildladdare

Både `imageView.load` och `AsyncImage` använder singletonobjektet `ImageLoader` för att genomföra bildförfrågningar. Singletonobjektet `ImageLoader` kan kommas åt genom att använda en förlängningsfunktion för `Context`:

```kotlin
val imageLoader = context.imageLoader
```

`ImageLoader`s är designade för att vara delbara och är mest effektiva när du skapar en enda instans och delar den i hela appen. Med det sagt, kan du även skapa din(a) egna instans(er) av `ImageLoader`:

```kotlin
val imageLoader = ImageLoader(context)
```

Om du inte vill använda singletonobjektet `ImageLoader`, använd artefakten `io.coil-kt:coil-base` istället för `io.coil-kt:coil`.

#### Förfrågningar

För att ladda en bild till ett anpassat mål, använd metoden `enqueue` på en instans av klassen `ImageRequest`:

```kotlin
val request = ImageRequest.Builder(context)
    .data("https://example.com/image.jpg")
    .target { drawable ->
        // Handle the result.
    }
    .build()
val disposable = imageLoader.enqueue(request)
```

För att ladda en bild imperativt, använd metoden `execute` på en instans av klassen `ImageRequest`:

```kotlin
val request = ImageRequest.Builder(context)
    .data("https://example.com/image.jpg")
    .build()
val drawable = imageLoader.execute(request).drawable
```

Kolla in Coils [fullständiga dokumentation här](https://coil-kt.github.io/coil/getting_started/).

## R8 / Proguard

Coil är fullt kompatibel med R8 och kräver inga särskilda extra regler.

Om du använder Proguard kan du behöva lägga till regler för [Coroutines](https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/resources/META-INF/proguard/coroutines.pro) och [OkHttp](https://github.com/square/okhttp/blob/master/okhttp/src/main/resources/META-INF/proguard/okhttp3.pro).

## Licens

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
