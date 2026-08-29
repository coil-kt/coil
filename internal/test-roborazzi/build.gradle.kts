import coil3.addAllMultiplatformTargets
import coil3.composeDesktopCurrentOsDependency
import coil3.multiplatformAndroidLibrary

plugins {
    id("com.android.kotlin.multiplatform.library")
    id("kotlin-multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("io.github.takahirom.roborazzi")
}

addAllMultiplatformTargets(libs.versions.skiko, enableNativeLinux = false)
multiplatformAndroidLibrary(name = "coil3.test.roborazzi")

kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(projects.coilCore)
            implementation(projects.coilComposeCore)
            implementation(projects.coilGif)
            implementation(projects.coilTest)
            implementation(projects.internal.testUtils)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.bundles.test.jvm)
            implementation(libs.roborazzi.compose)
            implementation(libs.roborazzi.core)
            implementation(libs.roborazzi.junit)
            implementation(libs.compose.ui.test.junit4)
        }
        jvmTest.dependencies {
            implementation(libs.roborazzi.compose.desktop)
            implementation(composeDesktopCurrentOsDependency())
            implementation(libs.compose.ui.test.junit4)
        }
        getByName("jvmTest").resources.srcDir(
            project(":coil-gif").projectDir.resolve("src/jvmTest/resources"),
        )
    }
}

roborazzi {
    // RoborazziOptions.RecordOptions.outputDirectoryPath and roborazzi.output.dir seems to be
    // ignored on desktop. To work around that this is used.
    outputDir = layout.projectDirectory.dir("src/jvmTest/snapshots/images")
}
