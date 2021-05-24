rootProject.extra.apply {
    set("androidPlugin", "com.android.tools.build:gradle:4.2.1")
    set("kotlinPlugin", "org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.10")
    set("mavenPublishPlugin", "com.vanniktech:gradle-maven-publish-plugin:0.15.1")
    set("dokkaPlugin", "org.jetbrains.dokka:dokka-gradle-plugin:1.4.32")
    set("dokkaAndroidPlugin", "org.jetbrains.dokka:android-documentation-plugin:1.4.32")
    set("binaryCompatibilityPlugin", "org.jetbrains.kotlinx:binary-compatibility-validator:0.5.0")
    set("ktlintPlugin", "org.jlleitschuh.gradle:ktlint-gradle:10.0.0")
}
