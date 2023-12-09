import coil3.addAllMultiplatformTargets
import coil3.androidLibrary
import coil3.androidUnitTest
import coil3.nonAndroidMain

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("kotlinx-atomicfu")
    id("dev.drewhamilton.poko")
}

addAllMultiplatformTargets()
androidLibrary(name = "coil3.test")

kotlin {
    nonAndroidMain()

    sourceSets {
        commonMain {
            dependencies {
                api(projects.coilCore)
            }
        }
        androidMain {
            dependencies {
                api(libs.androidx.core)
            }
        }
        commonTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.common)
            }
        }
        androidUnitTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.jvm)
            }
        }
    }
}
