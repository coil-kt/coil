import coil3.addAllMultiplatformTargets
import coil3.androidLibrary
import com.android.build.gradle.tasks.MergeSourceSetFolders

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
}

val skikoNativeX64: Configuration by configurations.creating
val skikoNativeArm64: Configuration by configurations.creating

dependencies {
    skikoNativeX64("org.jetbrains.skiko:skiko-android-runtime-x64:${libs.versions.skiko.get()}")
    skikoNativeArm64("org.jetbrains.skiko:skiko-android-runtime-arm64:${libs.versions.skiko.get()}")
}

val jniDir = "${projectDir.absolutePath}/src/androidMain/jniLibs"

val unzipTaskX64 = tasks.register<Copy>("unzipNativeX64") {
    destinationDir = file("$jniDir/x86_64")
    from(skikoNativeX64.map(::zipTree))
    include("**/*.so")
    includeEmptyDirs = false
}

val unzipTaskArm64 = tasks.register<Copy>("unzipNativeArm64") {
    destinationDir = file("$jniDir/arm64-v8a")
    from(skikoNativeArm64.map(::zipTree))
    include("**/*.so")
    includeEmptyDirs = false
}

tasks.withType<MergeSourceSetFolders>().configureEach {
    dependsOn(unzipTaskX64)
    dependsOn(unzipTaskArm64)
}

addAllMultiplatformTargets(libs.versions.skiko)
androidLibrary(name = "coil3.svg.skia") {
    defaultConfig {
        ndk {
            abiFilters += listOf("x86_64", "arm64-v8a")
        }
    }
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.coilCore)
                api(libs.skiko)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.androidx.core)
            }
        }
    }
}
