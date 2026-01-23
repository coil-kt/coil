import coil3.addAllMultiplatformTargets
import coil3.multiplatformAndroidLibrary
import coil3.skikoAwtRuntimeDependency

plugins {
    id("com.android.kotlin.multiplatform.library")
    id("kotlin-multiplatform")
    id("org.jetbrains.kotlinx.atomicfu")
    id("org.jetbrains.compose")
}

addAllMultiplatformTargets(
    skikoVersion = libs.versions.skiko,
    enableJs = false,
    enableWasm = false,
)
multiplatformAndroidLibrary(name = "coil3.gif")

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
            }
        }
        named("nonAndroidMain") {
            dependencies {
                implementation(libs.skiko)
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
                implementation(skikoAwtRuntimeDependency())
            }
        }
        getByName("androidHostTest") {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.jvm)
            }
        }
        getByName("androidDeviceTest") {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.android)
            }
        }
    }
}
