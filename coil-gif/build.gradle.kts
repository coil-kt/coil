import coil3.addAllMultiplatformTargets
import coil3.androidLibrary
import coil3.applyJvm11OnlyToJvmTarget
import coil3.skikoAwtRuntimeDependency

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("org.jetbrains.kotlinx.atomicfu")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

addAllMultiplatformTargets(libs.versions.skiko)
androidLibrary(name = "coil3.gif")

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.coilCore)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.androidx.core)
                implementation(libs.androidx.vectordrawable.animated)
                // Required by compose compiler plugin even though not used on Android
                implementation(compose.runtime)
            }
        }
        named("nonAndroidMain") {
            dependencies {
                implementation(libs.skiko)
                // Comment #7: Use compose.runtime only, not coil-compose-core
                implementation(compose.runtime)
            }
        }
        commonTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.common)
            }
        }
        jvmTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(skikoAwtRuntimeDependency(libs.versions.skiko.get()))
            }
        }
        androidUnitTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.jvm)
            }
        }
        androidInstrumentedTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.android)
            }
        }
    }
}

// Compose 1.8.0 requires JVM 11 only for the JVM target.
applyJvm11OnlyToJvmTarget()
