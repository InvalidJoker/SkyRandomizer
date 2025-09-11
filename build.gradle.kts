import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.papermc.paperweight.userdev.ReobfArtifactConfiguration
import org.gradle.api.JavaVersion.VERSION_21
import xyz.jpenilla.runpaper.task.RunServer

plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.6"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"
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

    implementation("org.jetbrains:annotations:26.0.1")
    implementation("org.xerial:sqlite-jdbc:3.50.2.0")

    annotationProcessor("org.projectlombok:lombok:1.18.36")
    implementation("org.projectlombok:lombok:1.18.36")

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
    named<ShadowJar>("shadowJar") {
        archiveFileName.set("${project.name}.jar")
        minimize {
            exclude(dependency("net.megavex:scoreboard-library-.*:.*"))
            exclude(dependency("dev.jorel:commandapi-.*:.*"))
            exclude(dependency("org.xerial:sqlite-jdbc:.*"))
        }

        relocate("net.megavex.scoreboardlibrary", "de.joker.randomizer.scoreboardlibrary")
    }

    named<ProcessResources>("processResources") {
        from(sourceSets.main.get().resources.srcDirs) {
            filesMatching("paper-plugin.yml") {
                expand("version" to project.version)
            }
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }

    named<RunServer>("runServer") {
        minecraftVersion("1.21.6")
    }

    build {
        dependsOn(shadowJar)
    }
}