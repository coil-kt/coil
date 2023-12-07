import coil3.addAllMultiplatformTargets
import coil3.androidUnitTest
import coil3.nonAndroidMain
import coil3.androidLibrary

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("kotlinx-atomicfu")
}

addAllMultiplatformTargets()
androidLibrary(name = "coil3.network")

kotlin {
    nonAndroidMain()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.coilCore)
                api(libs.ktor.core)
            }
        }
        commonTest {
            dependencies {
                implementation(projects.coilTestInternal)
                implementation(libs.bundles.test.common)
                implementation(libs.ktor.mock)
            }
        }
        androidUnitTest {
            dependencies {
                implementation(projects.coilTestInternal)
                implementation(libs.bundles.test.jvm)
            }
        }
    }
}
