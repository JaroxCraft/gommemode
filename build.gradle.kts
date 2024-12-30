plugins {
    id("fabric-loom") version "1.8-SNAPSHOT"
    kotlin("jvm") version "2.1.0"
}

repositories {
    maven { url = uri("https://maven.terraformersmc.com/") }
    maven { url = uri("https://maven.shedaniel.me/") }
    mavenCentral()
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")
    modImplementation("com.terraformersmc:modmenu:${project.property("modmenu_version")}")
    modApi("me.shedaniel.cloth:cloth-config-fabric:${project.property("cloth_version")}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", project.property("minecraft_version"))
    inputs.property("loader_version", project.property("loader_version"))
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to project.property("minecraft_version"),
            "loader_version" to project.property("loader_version")
        )
    }
}

val targetJavaVersion = 21

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.property("archives_base_name")}" }
    }
}

kotlin {
    jvmToolchain(targetJavaVersion)
}