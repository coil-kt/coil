import coil3.androidLibrary

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("app.cash.paparazzi")
    id("org.jetbrains.compose")
}

androidLibrary(name = "coil3.test.paparazzi")

compose {
    kotlinCompilerPlugin = libs.jetbrains.compose.compiler.get().toString()
    experimental.web.application {}
}

dependencies {
    api(projects.coilCore)

    implementation(projects.coilComposeCore)
    implementation(projects.coilTest)

    testImplementation(projects.internal.testUtils)
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
