import coil3.androidApplication

plugins {
    id("com.android.application")
    id("kotlin-android")
}

androidApplication(name = "sample.view") {
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(projects.coilSampleCommon)

    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.google.material)
}
