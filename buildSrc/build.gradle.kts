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
    implementation(rootProject.extra["androidPlugin"].toString())
    implementation(rootProject.extra["kotlinPlugin"].toString())
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}
