version = "0.1"
project.extra["PluginName"] = "Seaweeder"
project.extra["PluginDescription"] = "Dude weed lmao"

dependencies {
    compileOnly(project(":botutils"))
}

tasks {
    jar {
        manifest {
            attributes(mapOf(
                    "Plugin-Version" to project.version,
                    "Plugin-Id" to nameToId(project.extra["PluginName"] as String),
                    "Plugin-Provider" to project.extra["PluginProvider"],
                    "Plugin-Description" to project.extra["PluginDescription"],
                    "Plugin-License" to project.extra["PluginLicense"],
                    "Plugin-Dependencies" to nameToId("botutils")
            ))
        }
    }
}