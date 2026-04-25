import groovy.json.JsonSlurper

val modManifest = JsonSlurper().parse(file("src/main/resources/manifest.json")) as Map<*, *>

version = modManifest["Version"] as String
group = "gg.alexandre"

tasks.jar {
    archiveFileName.set("${modManifest["Name"]}-${modManifest["Version"]}.jar")
}