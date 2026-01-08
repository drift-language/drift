import java.io.ByteArrayOutputStream
import java.time.Year

plugins {
    kotlin("jvm") version "2.0.20"

    kotlin("plugin.serialization") version "2.0.20"
}

group = "dev.drift"
version = "2026.0"

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}

tasks.test {
    useJUnitPlatform()
}

fun String.runCommand() : String {
    val stdout = ByteArrayOutputStream()

    project.exec {
        commandLine = this@runCommand.split(" ")
        standardOutput = stdout
    }

    return stdout.toString().trim()
}

tasks.register("generateVersion") {
    val outputDir = layout.buildDirectory.dir("generated/sources/version/kotlin").get().asFile
    val versionFile = File(outputDir, "drift/DriftVersion.kt")
    versionFile.parentFile.mkdirs()

    val gitHash = "git rev-parse --short HEAD".runCommand()
    val projectVersion = project.version.toString()

    versionFile.writeText(
        """
            package drift

            object DriftVersion {
                val fullVersion = "$projectVersion.$gitHash"
            }
        """.trimIndent())
}

sourceSets["main"].java.srcDir(layout.buildDirectory.dir("generated/sources/version/kotlin"))

tasks.named("compileKotlin") {
    dependsOn("generateVersion")
}