import coil3.addAllMultiplatformTargets
import coil3.multiplatformAndroidLibrary

plugins {
    id("com.android.kotlin.multiplatform.library")
    id("kotlin-multiplatform")
    id("org.jetbrains.kotlinx.atomicfu")
    id("dev.drewhamilton.poko")
}

addAllMultiplatformTargets(libs.versions.skiko)
multiplatformAndroidLibrary(name = "coil3.test.utils") {
    androidResources {
        enable = true
    }
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(projects.coilCore)
                api(projects.coilNetworkCore)
                api(libs.bundles.test.common)
                api(libs.coroutines.test)
                api(libs.kotlinx.datetime)
            }
        }
        androidMain {
            dependencies {
                api(libs.androidx.activity)
                api(libs.androidx.appcompat.resources)
                api(libs.androidx.core)
                api(libs.androidx.test.core)
                api(libs.androidx.test.junit)
                compileOnly(libs.robolectric)
            }
        }
        named("jvmCommonMain") {
            dependencies {
                api(libs.kotlin.test.junit)
                api(libs.junit)
            }
        }
    }
}
