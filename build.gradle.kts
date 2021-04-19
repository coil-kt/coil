
import coil.by
import coil.groupId
import coil.versionName
import kotlinx.validation.ApiValidationExtension
import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import java.net.URL

buildscript {
    apply(from = "buildSrc/plugins.gradle.kts")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath(rootProject.extra["androidPlugin"].toString())
        classpath(rootProject.extra["kotlinPlugin"].toString())
        classpath(rootProject.extra["mavenPublishPlugin"].toString())
        classpath(rootProject.extra["dokkaPlugin"].toString())
        classpath(rootProject.extra["binaryCompatibilityPlugin"].toString())
        classpath(rootProject.extra["ktlintPlugin"].toString())
    }
}

apply(plugin = "binary-compatibility-validator")

extensions.configure<ApiValidationExtension> {
    ignoredProjects = mutableSetOf("coil-sample", "coil-test")
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://kotlin.bintray.com/kotlinx") // https://github.com/Kotlin/kotlinx.html/issues/173
    }

    group = project.groupId
    version = project.versionName

    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    extensions.configure<KtlintExtension>("ktlint") {
        version by "0.40.0"
        disabledRules by setOf("indent", "max-line-length")
    }

    apply(plugin = "org.jetbrains.dokka")

    tasks.withType<DokkaMultiModuleTask>().configureEach {
        outputDirectory by file("$rootDir/docs/api")
    }

    tasks.withType<DokkaTaskPartial>().configureEach {
        dokkaSourceSets.configureEach {
            jdkVersion by 8
            reportUndocumented by false
            skipDeprecated by true
            skipEmptyPackages by true
            outputDirectory by file("$rootDir/docs/api")

            externalDocumentationLink {
                url by URL("https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-android/")
            }
            externalDocumentationLink {
                url by URL("https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/")
            }
            externalDocumentationLink {
                url by URL("https://square.github.io/okhttp/4.x/okhttp/")
            }
            externalDocumentationLink {
                url by URL("https://square.github.io/okio/2.x/okio/")
            }
        }
    }
}
