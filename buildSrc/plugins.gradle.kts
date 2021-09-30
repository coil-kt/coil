rootProject.extra.apply {
    set("androidPlugin", "com.android.tools.build:gradle:7.0.2")
    set("kotlinPlugin", "org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.30")
    set("mavenPublishPlugin", "com.vanniktech:gradle-maven-publish-plugin:0.18.0")
    set("dokkaPlugin", "org.jetbrains.dokka:dokka-gradle-plugin:1.5.30")
    set("dokkaAndroidPlugin", "org.jetbrains.dokka:android-documentation-plugin:1.5.30")
    set("binaryCompatibilityPlugin", "org.jetbrains.kotlinx:binary-compatibility-validator:0.7.1")
    set("ktlintPlugin", "org.jlleitschuh.gradle:ktlint-gradle:10.2.0")
}
