import coil3.addAllMultiplatformTargets
import coil3.compileSdk
import coil3.minSdk
import coil3.multiplatformAndroidLibrary

plugins {
    id("com.android.kotlin.multiplatform.library")
    id("kotlin-multiplatform")
    id("org.jetbrains.kotlinx.atomicfu")
    id("dev.drewhamilton.poko")
}

addAllMultiplatformTargets(libs.versions.skiko)
multiplatformAndroidLibrary()

kotlin {
    androidLibrary {
        namespace = "coil3.test.utils"
        compileSdk = project.compileSdk
        minSdk = project.minSdk

        androidResources {
            enable = true
        }

        withHostTest {
            isIncludeAndroidResources = true
        }

        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        lint {
            warningsAsErrors = true
            disable += listOf(
                "ComposableNaming",
                "UnknownIssueId",
                "UnsafeOptInUsageWarning",
                "UnusedResources",
                "UseSdkSuppress",
                "VectorPath",
                "VectorRaster",
            )
        }

        packaging {
            resources.pickFirsts += listOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/*kotlin_module",
            )
        }
    }

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
