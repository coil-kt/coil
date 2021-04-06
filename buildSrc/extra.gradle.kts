repositories {
    google()
    mavenCentral()
}

rootProject.extra.apply {
    set("androidPlugin", "com.android.tools.build:gradle:4.1.3")
    set("kotlinPlugin", "org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.32")
}
