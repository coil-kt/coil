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
}

dependencies {
    implementation(project(":coil-sample-common"))
    implementation(project(":coil-compose-singleton"))
    implementation(project(":coil-gif"))
    implementation(project(":coil-svg"))
    implementation(project(":coil-video"))
}
