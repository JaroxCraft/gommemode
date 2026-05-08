import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("net.fabricmc.fabric-loom")
    kotlin("jvm") version "2.3.21"
}

val minecraftVersion = providers.gradleProperty("minecraft_version").get()

version = "${providers.gradleProperty("mod_version").get()}+$minecraftVersion"
group = providers.gradleProperty("maven_group").get()

repositories {
    maven { url = uri("https://maven.terraformersmc.com/") }
    maven { url = uri("https://maven.shedaniel.me/") }
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    implementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")
    implementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_version").get()}")
    implementation("com.terraformersmc:modmenu:${providers.gradleProperty("modmenu_version").get()}")
    implementation("me.shedaniel.cloth:cloth-config-fabric:${providers.gradleProperty("cloth_version").get()}")
    implementation("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version,
        "minecraft_version" to minecraftVersion,
        "loader_version" to providers.gradleProperty("loader_version").get(),
        "fabric_kotlin_version" to providers.gradleProperty("fabric_kotlin_version").get()
    )
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("fabric.mod.json") {
        expand(props)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_25
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
    withSourcesJar()
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.name}" }
    }
}