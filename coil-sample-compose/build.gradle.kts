import coil.setupAppModule

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("org.jetbrains.compose")
}

setupAppModule(name = "sample.compose") {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                "../coil-sample-common/shrinker-rules.pro",
                "../coil-sample-common/shrinker-rules-android.pro",
            )
            signingConfig = signingConfigs["debug"]
        }
        create("benchmark") {
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

compose {
    kotlinCompilerPlugin = libs.jetbrains.compose.compiler.get().toString()
}

dependencies {
    implementation(projects.coilSampleComposeCommon)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
