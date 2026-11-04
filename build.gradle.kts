plugins {
    kotlin("jvm") version "2.5.0-Beta2"
    id("com.gradleup.shadow") version "9.6.1"
}

repositories {
    google()
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    // everything we require
    compileOnly("org.spigotmc:spigot-api:26.1.1-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.github.classgraph:classgraph:4.8.199")
    compileOnly("com.github.retrooper:packetevents-spigot:2.14.0")

    // integrations
    compileOnly("me.clip:placeholderapi:2.12.3")
}

kotlin {
    jvmToolchain(25)
}

tasks {
    test {
        useJUnitPlatform()
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        destinationDirectory = file("/home/inparsian/262_test_server/plugins/")
    }
}
