import coil3.androidApplication

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("org.jetbrains.kotlin.plugin.compose")
}

androidApplication(name = "sample.compose") {
    buildFeatures {
        buildConfig = true
    }
    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs["debug"]
        }
        create("minifiedRelease") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            matchingFallbacks += "release"
            proguardFiles(
                "../shared/shrinker-rules.pro",
                "../shared/shrinker-rules-android.pro",
            )
            signingConfig = signingConfigs["debug"]
        }
    }
}

dependencies {
    implementation(projects.samples.compose)
    implementation(projects.samples.shared)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
