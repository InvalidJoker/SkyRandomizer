
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.papermc.paperweight.userdev.ReobfArtifactConfiguration
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import net.minecrell.pluginyml.paper.PaperPluginDescription
import org.gradle.api.JavaVersion.VERSION_21

plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"
    id("de.eldoria.plugin-yml.paper") version "0.8.0"
    id("com.gradleup.shadow") version "8.3.6"
}

group = "de.joker"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
    maven { url = uri("https://jitpack.io") }
}

paperweight.reobfArtifactConfiguration = ReobfArtifactConfiguration.MOJANG_PRODUCTION

dependencies {
    paperweight.paperDevBundle("1.21.6-R0.1-SNAPSHOT")

    compileOnly("org.jetbrains:annotations:26.0.2")
    implementation("org.xerial:sqlite-jdbc:3.50.2.0")

    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")

    implementation("net.megavex:scoreboard-library-api:2.4.1")
    runtimeOnly("net.megavex:scoreboard-library-implementation:2.4.1")
    runtimeOnly("net.megavex:scoreboard-library-modern:2.4.1:mojmap")

    compileOnly("com.github.retrooper:packetevents-spigot:2.7.0")
    compileOnly("com.github.cytooxien:realms-api:4.0.1")

    implementation("dev.jorel:commandapi-bukkit-shade-mojang-mapped:10.1.2")
}


tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = VERSION_21.toString()
    targetCompatibility = VERSION_21.toString()
    options.encoding = "UTF-8"
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

configurations.implementation {
    exclude("org.bukkit", "bukkit")
}


tasks {
    named<ProcessResources>("processResources") {
        from(sourceSets.main.get().resources.srcDirs) {
            filesMatching("paper-plugin.yml") {
                expand("version" to project.version)
            }
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }

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

        manifest {
            attributes["paperweight-mappings-namespace"] = "mojang"
        }

        relocate("net.megavex.scoreboardlibrary", "de.joker.randomizer.scoreboardlibrary")
    }

}

paper {
    main = "de.joker.randomizer.SkyRandomizer"
    load = BukkitPluginDescription.PluginLoadOrder.STARTUP

    name = "SkyRandomizer"
    description = "A custom Skyblock randomizer plugin"
    website = "https://github.com/InvalidJoker/SkyRandomizer"
    authors = listOf("InvalidJoker")
    apiVersion = "1.21"
    version = project.version.toString()

    serverDependencies {
        register("packetevents") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            required = true
        }
        register("Realms-API") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            required = false
            joinClasspath = true
        }
    }
}