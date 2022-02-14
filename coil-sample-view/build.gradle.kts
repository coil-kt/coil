import coil.setupAppModule

plugins {
    id("com.android.application")
    id("kotlin-android")
}

setupAppModule {
    defaultConfig {
        applicationId = "coil.sample"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("shrinker-rules.pro", "shrinker-rules-android.pro")
            signingConfig = signingConfigs["debug"]
        }
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(projects.coilSampleCommon)
    implementation(projects.coilSingleton)
    implementation(projects.coilGif)
    implementation(projects.coilSvg)
    implementation(projects.coilVideo)

    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)
}
