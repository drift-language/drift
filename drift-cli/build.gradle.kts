plugins {
    kotlin("jvm") version "2.2.0"

    id("com.gradleup.shadow") version "8.3.6"

    application
}

group = "fr.belic.drift"
version = "2026.0"

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

    implementation("fr.belic.drift:drift-common:2026.0")
    implementation("fr.belic.drift:drift-core:2026.0")
    implementation("fr.belic.drift:drift-analysis:2026.0")
    implementation("fr.belic.drift:drift-hir:2026.0")
    implementation("fr.belic.drift:drift-ir:2026.0")
    implementation("fr.belic.drift:drift-bootstrap:2026.0")

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

tasks.register<JavaExec>("runDebugger") {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("drift.cli.DriftRunnerTestKt")
    workingDir = rootProject.projectDir.parentFile
    args = listOf("${workingDir}/examples/src/main.drift")
}

application {
    mainClass.set("drift.cli.DriftRunnerKt")
}