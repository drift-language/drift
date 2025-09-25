plugins {
    kotlin("jvm") version "2.0.20"

    application
}

group = "dev.drift"
version = "0.1"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("generateVersionFile") {
    val outputDirectory = layout.buildDirectory.dir("generated/resources")
    outputs.dir(outputDirectory)

    doLast {
        outputDirectory.get().file("version.txt").asFile.also {
            it.parentFile.mkdirs()
            it.writeText(project.version.toString())
        }
    }
}

tasks.named("processResources") {
    dependsOn("generateVersionFile")
}

sourceSets.main {
    resources.srcDir(layout.buildDirectory.dir("generated/resources"))
}

application {
    mainClass.set("drift.DriftReplKt")
}