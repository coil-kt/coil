# FAQ

Have a question that isn't part of the FAQ? Check [StackOverflow](https://stackoverflow.com/questions/tagged/coil) with the tag #coil or search our [Github issues](https://github.com/coil-kt/coil/issues).

## Can Coil be used with Java projects or mixed Kotlin/Java projects?

Yes! [Read here](java_compatibility.md).

## How do I preload an image?

[Read here](getting_started.md#preloading).

## How do I set up disk caching?

[Read here](image_loaders.md#caching).

## How do I enable logging?

Set `logger(DebugLogger())` when constructing your `ImageLoader`.

!!! Note
    `DebugLogger` should only be used in debug builds.

## How do I get development snapshots?

Add the snapshots repository to your list of repositories:

Gradle (`.gradle`):

```groovy
allprojects {
    repositories {
        maven { url 'https://oss.sonatype.org/content/repositories/snapshots/' }
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

Then depend on the same artifacts with [the latest snapshot version](https://github.com/coil-kt/coil/blob/master/gradle.properties#L19).

!!! Note
    Snapshots are deployed for each new commit on `master` that passes CI. They can potentially contain breaking changes or may be unstable. Use at your own risk.
