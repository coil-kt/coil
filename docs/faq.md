# FAQ

Have a question that isn't part of the FAQ? Check [StackOverflow](https://stackoverflow.com/questions/tagged/coil) with the tag #coil or search [Github discussions](https://github.com/coil-kt/coil/discussions).

## Can Coil be used with Java projects or mixed Kotlin/Java projects?

Yes! [Read here](java_compatibility.md).

## How do I preload an image?

Launch an image request with no target:

```kotlin
val request = ImageRequest.Builder(context)
    .data("https://example.com/image.jpg")
    .build()
imageLoader.enqueue(request)
```

That will preload the image and save it to the disk and memory caches.

If you only want to preload to the disk cache you can skip decoding and saving to the memory cache like so:

```kotlin
val request = ImageRequest.Builder(context)
    .data("https://example.com/image.jpg")
    // Disables writing to the memory cache.
    .memoryCachePolicy(CachePolicy.DISABLED)
    // Skips the decode step so we don't waste time/memory decoding the image into memory.
    .decoderFactory(BlackholeDecoder.Factory())
    .build()
imageLoader.enqueue(request)
```

## How do I enable logging?

Set `logger(DebugLogger())` when [constructing your `ImageLoader`](getting_started.md#configuring-the-singleton-imageloader).

!!! Note
    `DebugLogger` should only be used in debug builds.

## How do I target Java 8 or Java 11?

Coil requires [Java 8 bytecode](https://developer.android.com/studio/write/java8-support). This is enabled by default on the Android Gradle Plugin `4.2.0` and later and the Kotlin Gradle Plugin `1.5.0` and later. If you're using older versions of those plugins add the following to your Gradle build script:

```kotlin
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
```

As of Coil `3.2.0`, Java 11 bytecode is required for `coil-compose` and `coil-compose-core`:

```kotlin
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}
```

## How do I get development snapshots?

Add the snapshots repository to your list of repositories:

Gradle (`.gradle`):

```groovy
allprojects {
    repositories {
        maven { url 'https://central.sonatype.com/repository/maven-snapshots/' }
    }
}
```

Gradle Kotlin DSL (`.gradle.kts`):

```kotlin
allprojects {
    repositories {
        maven("https://central.sonatype.com/repository/maven-snapshots/")
    }
}
```

Then depend on the same artifacts with [the latest snapshot version](https://github.com/coil-kt/coil/blob/main/gradle.properties#L34).

!!! Note
    Snapshots are deployed for each new commit on `main` that passes CI. They can potentially contain breaking changes or may be unstable. Use at your own risk.

## How to I use Proguard with Coil?

To use Proguard with Coil, add the following rules to your config:

```
-keep class coil3.util.DecoderServiceLoaderTarget { *; }
-keep class coil3.util.FetcherServiceLoaderTarget { *; }
-keep class coil3.util.ServiceLoaderComponentRegistry { *; }
-keep class * implements coil3.util.DecoderServiceLoaderTarget { *; }
-keep class * implements coil3.util.FetcherServiceLoaderTarget { *; }
```

You may also need to add custom rules for Ktor, OkHttp, and Coroutines.

!!! Note
    **You do not need to add any custom rules for Coil if you use R8**, which is the default code shrinker on Android. The rules are added automatically.
