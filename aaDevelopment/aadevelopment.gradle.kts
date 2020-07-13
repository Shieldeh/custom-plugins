version = "0.1"
project.extra["PluginName"] = "aaDevelopment"
//TODO::Add a description
project.extra["PluginDescription"] = "debug plugin"

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