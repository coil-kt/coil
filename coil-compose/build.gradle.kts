import coil3.addAllMultiplatformTargets
import coil3.androidLibrary
import coil3.applyJvm11OnlyToJvmTarget

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("org.jetbrains.kotlinx.atomicfu")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

addAllMultiplatformTargets(libs.versions.skiko)
androidLibrary(name = "coil3.compose.singleton")

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.coil)
                api(projects.coilComposeCore)
            }
        }
    }
}

// Compose 1.8.0 requires JVM 11 only for the JVM target.
applyJvm11OnlyToJvmTarget()
