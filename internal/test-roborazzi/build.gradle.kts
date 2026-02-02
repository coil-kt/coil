import coil3.addAllMultiplatformTargets
import coil3.compileSdk
import coil3.kmpAndroidLibrary
import coil3.minSdk

plugins {
    id("com.android.kotlin.multiplatform.library")
    id("kotlin-multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("io.github.takahirom.roborazzi")
}

addAllMultiplatformTargets(libs.versions.skiko, enableNativeLinux = false)
kmpAndroidLibrary()

kotlin {
    androidLibrary {
        namespace = "coil3.test.roborazzi"
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
        commonTest.dependencies {
            implementation(projects.coilCore)
            implementation(projects.coilComposeCore)
            implementation(projects.coilTest)
            implementation(projects.internal.testUtils)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.bundles.test.jvm)
            implementation(libs.roborazzi.compose)
            implementation(libs.roborazzi.core)
            implementation(libs.roborazzi.junit)
            implementation(compose.desktop.uiTestJUnit4)
        }
        jvmTest.dependencies {
            implementation(libs.roborazzi.compose.desktop)
            implementation(compose.desktop.currentOs)
            implementation(compose.desktop.uiTestJUnit4)
        }
    }
}

roborazzi {
    // RoborazziOptions.RecordOptions.outputDirectoryPath and roborazzi.output.dir seems to be
    // ignored on desktop. To workaround that this is used.
    outputDir = layout.projectDirectory.dir("src/jvmTest/snapshots/images")
}
