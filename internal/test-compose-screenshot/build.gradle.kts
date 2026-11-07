import coil3.androidLibrary

plugins {
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

androidLibrary(name = "coil3.test.composescreenshot") {
    buildFeatures {
        compose = true
    }
    testOptions {
        screenshotTests.create("screenshotTest") {
            engineVersion = libs.screenshot.validation.api.get().versionConstraint.requiredVersion
            imageDifferenceThreshold = 0.01f
            targetVariants.add("debug")

            dependencies {
                implementation(libs.androidx.compose.ui.tooling)
                implementation(libs.screenshot.validation.api)
            }
        }
    }
}

dependencies {
    api(projects.coilCore)

    implementation(projects.coilComposeCore)
    implementation(projects.coilTest)
}
