import coil3.addAllMultiplatformTargets
import coil3.compileSdk
import coil3.multiplatformAndroidLibrary
import coil3.minSdk

plugins {
    id("com.android.kotlin.multiplatform.library")
    id("kotlin-multiplatform")
    id("org.jetbrains.kotlinx.atomicfu")
}

addAllMultiplatformTargets(libs.versions.skiko)
multiplatformAndroidLibrary()

kotlin {
    androidLibrary {
        namespace = "coil3.network.cachecontrol"
        compileSdk = project.compileSdk
        minSdk = project.minSdk

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
                api(libs.kotlinx.datetime)
            }
        }
        commonTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.common)
            }
        }
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.bundles.test.jvm)
            }
        }
    }
}
