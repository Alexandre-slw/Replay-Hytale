import groovy.json.JsonSlurper

val modManifest = JsonSlurper().parse(file("src/main/resources/manifest.json")) as Map<*, *>

version = modManifest["Version"] as String
group = "gg.alexandre"

tasks.jar {
    var serverVersion = modManifest["ServerVersion"] as String;
    if (serverVersion.contains("=")) {
        serverVersion = serverVersion.split("=")[1].trim()
    }

    archiveFileName.set("${modManifest["Name"]}-${modManifest["Version"]}-Hytale-${serverVersion}.jar")
}