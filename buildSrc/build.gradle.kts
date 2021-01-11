plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    jcenter()
}

apply(from = "extra.gradle.kts")

dependencies {
    implementation(rootProject.extra.get("androidPlugin").toString())
    implementation(rootProject.extra.get("kotlinPlugin").toString())
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}
