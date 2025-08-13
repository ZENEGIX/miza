import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.utils.extendsFrom

plugins {
    kotlin("jvm") version "1.9.24"
    id("fabric-loom") version "1.8-SNAPSHOT"
    id("maven-publish")
    id("com.gradleup.shadow") version "8.3.4"
    id("com.google.protobuf") version("0.9.4")
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

val targetJavaVersion = 21

java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    withSourcesJar()
}

repositories {
    maven {
        name = "wispForestReleases"
        url = uri("https://maven.wispforest.io/releases")
    }
    maven {
        url = uri("https://cursemaven.com")
        content {
            includeGroup("curse.maven")
        }
    }
}

configurations {
    implementation.extendsFrom(shadow)
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${project.property("kotlin_loader_version")}")

    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")
    modImplementation("io.wispforest:owo-lib:${properties["owo_version"]}")
    modCompileOnly("curse.maven:journeymap-32274:5972764")

    shadow("com.fasterxml.jackson.core:jackson-core:2.18.2")
    shadow("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    shadow("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2") {
        exclude("org.jetbrains.kotlin")
    }

    shadow("com.google.protobuf:protobuf-kotlin:4.28.0")
    shadow("io.grpc:grpc-protobuf:1.66.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
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
            "loader_version" to project.property("loader_version"),
            "kotlin_loader_version" to project.property("kotlin_loader_version")
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName}" }
    }
}

tasks.test {
    useJUnitPlatform()
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.28.0"
    }

    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.66.0"
        }
        create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.1:jdk8@jar"
        }
    }

    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
                create("grpckt")
            }
            it.builtins {
                create("kotlin")
            }
        }
    }
}

tasks.shadowJar {
    configurations = listOf(project.configurations.shadow.get())
    relocate("com.fasterxml.jackson", "ru.zenegix.miza.shaded.jackson")
    relocate("com.google.protobuf", "ru.zenegix.miza.shaded.protobuf")
    mergeServiceFiles()
}

tasks.remapJar {
    archiveBaseName.set("Miza")
    archiveVersion.set(project.version.toString())

    dependsOn(tasks.shadowJar)
    mustRunAfter(tasks.shadowJar)

    inputFile = tasks.shadowJar.get().archiveFile
}
