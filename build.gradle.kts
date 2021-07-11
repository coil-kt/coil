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

apply(plugin = "org.jetbrains.dokka")

tasks.withType<DokkaMultiModuleTask>().configureEach {
    outputDirectory by file("$rootDir/docs/api")
    removeChildTasks(listOf(project(":coil-sample"), project(":coil-test")))
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }

    group = project.groupId
    version = project.versionName

    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    extensions.configure<KtlintExtension>("ktlint") {
        version by "0.41.0"
        disabledRules by setOf("indent", "max-line-length")
    }

    tasks.withType<DokkaTaskPartial>().configureEach {
        dokkaSourceSets.configureEach {
            jdkVersion by 8
            skipDeprecated by true

            externalDocumentationLink {
                url by URL("https://developer.android.com/reference/")
            }
            externalDocumentationLink {
                url by URL("https://kotlin.github.io/kotlinx.coroutines/")
            }
            externalDocumentationLink {
                url by URL("https://square.github.io/okhttp/3.x/okhttp/")
            }
            externalDocumentationLink {
                url by URL("https://square.github.io/okio/2.x/okio/")
            }
        }
    }
}
