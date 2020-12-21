import java.nio.charset.StandardCharsets

rootProject.extra.run {
    set("androidGradlePlugin", "com.android.tools.build:gradle:4.1.1")
    set("kotlinPlugin", "org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.21")
}

repositories {
    google()
    mavenCentral()
    jcenter()
}

tasks.withType(JavaCompile::class) {
    options.encoding = StandardCharsets.UTF_8.toString()
}
