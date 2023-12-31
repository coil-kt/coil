# FAQ

Have a question that isn't part of the FAQ? Check [StackOverflow](https://stackoverflow.com/questions/tagged/coil) with the tag #coil or search [Github discussions](https://github.com/coil-kt/coil/discussions).

## Can Coil be used with Java projects or mixed Kotlin/Java projects?

Yes! [Read here](java_compatibility.md).

## How do I preload an image?

[Read here](getting_started.md#preloading).

## How do I enable logging?

Set `logger(DebugLogger())` when [constructing your `ImageLoader`](getting_started.md#image-loaders).

!!! Note
    `DebugLogger` should only be used in debug builds.

## How do I target Java 8?

Coil requires [Java 8 bytecode](https://developer.android.com/studio/write/java8-support). This is enabled by default on the Android Gradle Plugin `4.2.0` and later and the Kotlin Gradle Plugin `1.5.0` and later. If you're using older versions of those plugins add the following to your Gradle build script:

Gradle (`.gradle`):

```groovy
android {
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
```

Gradle Kotlin DSL (`.gradle.kts`):

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

## How do I get development snapshots?

Add the snapshots repository to your list of repositories:

Gradle (`.gradle`):

```groovy
allprojects {
    repositories {
        maven { url 'https://oss.sonatype.org/content/repositories/snapshots' }
    }
}
```

Gradle Kotlin DSL (`.gradle.kts`):

```kotlin
allprojects {
    repositories {
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }
}
```

Then depend on the same artifacts with [the latest snapshot version](https://github.com/coil-kt/coil/blob/main/gradle.properties#L22).

!!! Note
    Snapshots are deployed for each new commit on `main` that passes CI. They can potentially contain breaking changes or may be unstable. Use at your own risk.
