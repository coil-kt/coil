plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(libs.androidPlugin)
    implementation(libs.kotlinPlugin)
}
