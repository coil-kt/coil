import coil3.VerifySkikoVersionsTask
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.result.ResolvedDependencyResult

// Verify Skiko versions match between coil-core and coil-compose-core.
tasks.register<VerifySkikoVersionsTask>("verifySkikoVersionsMatch") {
    group = "verification"
    description = "Ensures Skiko versions in coil-core and coil-compose-core match."

    // Compute requested versions at configuration time to avoid accessing project at execution time.
    val coreRequested = provider {
        requestedSkikoVersionFromJvmByOrigin(project(":coil-core"), project.group.toString())
    }
    val composeRequested = provider {
        requestedSkikoVersionFromJvmByOrigin(project(":coil-compose-core"), "org.jetbrains.compose")
    }
    coreRequestedSkikoVersion.set(coreRequested)
    composeRequestedSkikoVersion.set(composeRequested)
}

private fun requestedSkikoVersionFromJvmByOrigin(targetProject: Project, originGroupPrefix: String): String {
    val configurationNames = listOf("jvmRuntimeClasspath", "jvmTestRuntimeClasspath")
    for (name in configurationNames) {
        val cfg = targetProject.configurations.findByName(name) ?: continue
        // Force dependency graph calculation.
        cfg.dependencies
        val result = cfg.incoming.resolutionResult
        for (dep in result.allDependencies) {
            val resolved = dep as? ResolvedDependencyResult ?: continue
            val fromId = (resolved.from as? ResolvedComponentResult)?.moduleVersion
            val requested = resolved.requested as? ModuleComponentSelector ?: continue
            if (fromId != null && fromId.group.startsWith(originGroupPrefix) && requested.group == "org.jetbrains.skiko") {
                return requested.version
            }
        }
    }
    error("Couldn't find requested Skiko JVM dependency in ${targetProject.path} from '$originGroupPrefix' (checked ${configurationNames.joinToString()}).")
}

// Attach verification only to the root `check` task.
tasks.matching { it.name == "check" }.configureEach {
    dependsOn(tasks.named("verifySkikoVersionsMatch"))
}
