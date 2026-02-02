import coil3.addAllMultiplatformTargets
import coil3.applyJvm11OnlyToJvmTarget
import coil3.compileSdk
import coil3.minSdk
import coil3.multiplatformAndroidLibrary

plugins {
    id("com.android.kotlin.multiplatform.library")
    id("kotlin-multiplatform")
    id("org.jetbrains.kotlinx.atomicfu")
    id("dev.drewhamilton.poko")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("androidx.baselineprofile.consumer")
}

addAllMultiplatformTargets(libs.versions.skiko, enableNativeLinux = false)
multiplatformAndroidLibrary()

// https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-test.html
dependencies {
    "androidMainRuntimeOnly"(libs.androidx.compose.ui.test.manifest)
}

kotlin {
    androidLibrary {
        namespace = "coil3.compose.core"
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
                api(compose.foundation)
            }
        }
        commonTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.kotlin.test)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.google.drawablepainter)
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
                implementation(compose.desktop.uiTestJUnit4)
            }
        }
    }
}

baselineProfile {
    mergeIntoMain = true
    saveInSrc = true
    baselineProfileOutputDir = "."
    filter {
        include("coil3.compose.**")
    }
    variants {
        create("androidMain") {
            from(project(":internal:benchmark"))
        }
    }
}

// Compose 1.8.0 requires JVM 11 only for the JVM target.
applyJvm11OnlyToJvmTarget()
