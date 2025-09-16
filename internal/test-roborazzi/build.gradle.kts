import coil3.addAllMultiplatformTargets
import coil3.androidLibrary

plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("io.github.takahirom.roborazzi")
}

addAllMultiplatformTargets(libs.versions.skiko, enableNativeLinux = false)
androidLibrary(name = "coil3.test.roborazzi")

kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(projects.coilCore)
            implementation(projects.coilComposeCore)
            implementation(projects.coilTest)
            implementation(projects.internal.testUtils)
        }
        androidUnitTest.dependencies {
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
