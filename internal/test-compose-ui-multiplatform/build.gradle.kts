import coil3.addAllMultiplatformTargets
import coil3.applyJvm11OnlyToJvmTarget
import coil3.compileSdk
import coil3.minSdk
import coil3.multiplatformAndroidLibrary
import coil3.skikoAwtRuntimeDependency
import org.jetbrains.compose.ExperimentalComposeLibrary

plugins {
    id("com.android.kotlin.multiplatform.library")
    id("kotlin-multiplatform")
    id("org.jetbrains.kotlinx.atomicfu")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

addAllMultiplatformTargets(libs.versions.skiko, enableNativeLinux = false)
multiplatformAndroidLibrary()

kotlin {
    androidLibrary {
        namespace = "coil3.test.composeuimultiplatform"
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
                implementation(projects.coil)
                implementation(projects.coilComposeCore)
                implementation(compose.components.resources)
            }
        }
        commonTest {
            dependencies {
                implementation(projects.internal.testUtils)
                implementation(libs.bundles.test.common)
                @OptIn(ExperimentalComposeLibrary::class)
                implementation(compose.uiTest)
            }
        }
        jvmTest {
            dependencies {
                implementation(skikoAwtRuntimeDependency(libs.versions.skiko.get()))
            }
        }
    }
}

// Compose 1.8.0 requires JVM 11 only for the JVM target.
applyJvm11OnlyToJvmTarget()
