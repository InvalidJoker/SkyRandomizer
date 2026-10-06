
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import net.minecrell.pluginyml.paper.PaperPluginDescription
import org.gradle.api.JavaVersion.VERSION_25

plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.24"
    id("de.eldoria.plugin-yml.paper") version "0.9.0"
    id("com.gradleup.shadow") version "9.6.1"
}

group = "de.joker"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven { url = uri("https://jitpack.io") }
}


dependencies {
    paperweight.paperDevBundle("26.3.build.+")
    compileOnly("com.mojang:brigadier:1.0.18")

    compileOnly("org.jetbrains:annotations:26.0.2")
    paperLibrary("org.xerial:sqlite-jdbc:3.53.4.0")

    compileOnly("org.projectlombok:lombok:1.18.48")
    annotationProcessor("org.projectlombok:lombok:1.18.48")

    implementation("net.megavex:scoreboard-library-api:2.8.2")
    runtimeOnly("net.megavex:scoreboard-library-implementation:2.8.2")

    compileOnly("com.github.cytooxien:realms-api:4.0.1")
}


tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = VERSION_25.toString()
    targetCompatibility = VERSION_25.toString()
    options.encoding = "UTF-8"
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

configurations.implementation {
    exclude("org.bukkit", "bukkit")
}


tasks {
    build {
        dependsOn("shadowJar")
    }

    named<ShadowJar>("shadowJar") {
        archiveFileName.set("${project.name}.jar")
        minimize {
            exclude(dependency("net.megavex:scoreboard-library-.*:.*"))
            exclude(dependency("dev.jorel:commandapi-.*:.*"))
            exclude(dependency("org.xerial:sqlite-jdbc:.*"))
        }

        relocate("net.megavex.scoreboardlibrary", "de.joker.randomizer.scoreboardlibrary")
        relocate("dev.jorel.commandapi", "de.joker.randomizer.commandapi")
    }

}

paper {
    main = "de.joker.randomizer.SkyRandomizer"
    load = BukkitPluginDescription.PluginLoadOrder.STARTUP

    name = "SkyRandomizer"
    description = "A custom Skyblock randomizer plugin"
    website = "https://github.com/InvalidJoker/SkyRandomizer"
    authors = listOf("InvalidJoker")
    apiVersion = "26.3"
    version = project.version.toString()

    generateLibrariesJson = true
    loader = "de.joker.randomizer.LibraryLoader"

    serverDependencies {
        register("Realms-API") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            required = false
            joinClasspath = true
        }
    }
}