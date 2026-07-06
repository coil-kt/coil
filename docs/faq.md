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

## Why do I get a Skiko version warning with Compose Multiplatform?

Compose Multiplatform will print a warning like this if Coil's Skiko dependency is older than Compose Multiplatform's:

```text
w: Skiko dependencies' versions are incompatible.
```

This warning is generally safe to ignore, as Skiko versions typically maintain binary compatibility. Coil releases track Compose Multiplatform **stable** releases and their Skiko versions, so if you encounter this warning, first update Coil to the latest version.

**NOTE**: As a rule, Coil doesn't release new versions only to match the Skiko versions used by **alpha** and **beta** versions of Compose Multiplatform unless those Skiko versions are incompatible with the version depended on by the latest Coil release.

If the warning is still present, you can ignore it by setting the following Gradle property:

```properties
org.jetbrains.compose.library.compatibility.check.disable=true
```

However, that Gradle property disables all Compose Multiplatform library compatibility checks for all libraries - not just Coil. To disable the warning only for Coil and its Skiko dependency, add this code snippet to your root `build.gradle.kts` file:

```kotlin
dependencies {
    components {
        all {
            if (id.group == "io.coil-kt.coil3") {
                allVariants {
                    withDependencies {
                        val hadSkikoDependency = removeAll {
                            it.group == "org.jetbrains.skiko" && it.name == "skiko"
                        }
                        if (hadSkikoDependency) {
                            // Replace `0.150.0` with the Skiko version used by your Compose Multiplatform version.
                            add("org.jetbrains.skiko:skiko:0.150.0")
                        }
                    }
                }
            }
        }
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

To use Proguard with Coil, [add these Proguard rules to your config](https://github.com/coil-kt/coil/blob/main/coil-core/src/jvmMain/resources/META-INF/proguard/proguard-rules.pro).

You may also need to add custom rules for Ktor, OkHttp, and Coroutines.

!!! Note
    **You do not need to add any custom rules for Coil if you use R8**, which is the default code shrinker on Android. The rules are added automatically.
