import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.modrinth.minotaur)
    alias(libs.plugins.ktlint)
}

val minecraftVersion = libs.versions.minecraft.get()

version = "${providers.gradleProperty("mod_version").get()}+$minecraftVersion"
group = providers.gradleProperty("maven_group").get()

repositories {
    maven { url = uri("https://maven.terraformersmc.com/") }
    maven { url = uri("https://maven.shedaniel.me/") }
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:${libs.versions.minecraft.get()}")
    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)
    implementation(libs.modmenu)
    implementation(libs.cloth.config)
    implementation(libs.fabric.language.kotlin)
}

tasks.processResources {
    val props =
        mapOf(
            "version" to project.version,
            "minecraft_version" to minecraftVersion,
            "loader_version" to libs.versions.loader.get(),
            "fabric_kotlin_version" to
                libs.versions.fabric.language.kotlin
                    .get(),
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

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN"))
    projectId.set("gommemode")
    versionNumber.set(project.version.toString())
    versionType.set(if (project.version.toString().contains("-")) "beta" else "release")
    uploadFile.set(tasks.jar)
    gameVersions.addAll(libs.versions.minecraft.get())
    loaders.add("fabric")
    syncBodyFrom.set(project.file("README.md").readText())
    dependencies {
        required.project("fabric-api")
        required.project("fabric-language-kotlin")
    }
}
