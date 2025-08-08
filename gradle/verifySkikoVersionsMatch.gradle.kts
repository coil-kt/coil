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
        // Force dependency graph calculation
        cfg.dependencies
        val result = cfg.incoming.resolutionResult
        result.allDependencies.forEach { dep ->
            val resolved = dep as? org.gradle.api.artifacts.result.ResolvedDependencyResult ?: return@forEach
            val from = resolved.from
            val fromId = (from as? org.gradle.api.artifacts.result.ResolvedComponentResult)?.moduleVersion
            val requested = resolved.requested as? org.gradle.api.artifacts.component.ModuleComponentSelector ?: return@forEach
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
