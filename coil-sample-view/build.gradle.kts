import coil.Library
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
    implementation(project(":coil-sample-common"))
    implementation(project(":coil-singleton"))
    implementation(project(":coil-gif"))
    implementation(project(":coil-svg"))
    implementation(project(":coil-video"))

    implementation(Library.ANDROIDX_ACTIVITY)
    implementation(Library.ANDROIDX_RECYCLER_VIEW)
    implementation(Library.MATERIAL)
}
