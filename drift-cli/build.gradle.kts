plugins {
    kotlin("jvm") version "2.0.20"

    id("com.github.johnrengelman.shadow") version "8.1.1"

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

    implementation(project(":drift-common"))
    implementation(project(":drift-core"))

    implementation("com.github.ajalt.clikt:clikt:5.0.3")
    implementation("com.github.ajalt.clikt:clikt-markdown:5.0.3")

    val mordantVersion = "3.0.2"

    // Adds all JVM interface modules
    implementation("com.github.ajalt.mordant:mordant:${mordantVersion}")
    // optional extensions for running animations with coroutines
    implementation("com.github.ajalt.mordant:mordant-coroutines:${mordantVersion}")
    // optional widget for rendering Markdown
    implementation("com.github.ajalt.mordant:mordant-markdown:${mordantVersion}")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("drift.cli.DriftRunnerKt")
}