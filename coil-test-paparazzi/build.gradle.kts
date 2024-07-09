import coil.setupLibraryModule

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("app.cash.paparazzi")
}

setupLibraryModule(name = "coil.test.paparazzi") {
    buildFeatures {
        compose = true
    }
}

dependencies {
    api(projects.coilBase)

    implementation(projects.coilComposeBase)
    implementation(projects.coilTest)

    testImplementation(projects.coilTestInternal)
    testImplementation(libs.bundles.test.jvm)
}

// https://github.com/diffplug/spotless/issues/1572
afterEvaluate {
    tasks {
        named("spotlessKotlin").configure {
            dependsOn(named("testDebugUnitTest"))
            dependsOn(named("testReleaseUnitTest"))
        }
    }
}
