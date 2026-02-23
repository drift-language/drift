plugins {
    kotlin("jvm") version "2.2.0"

    application
}

group = "fr.belic.drift"
version = "2026.0"

repositories {
    mavenCentral()
    mavenLocal()
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    testImplementation(kotlin("test"))

    implementation(project(":drift-common"))
}

tasks.test {
    useJUnitPlatform()
}

sourceSets.main {
    resources.srcDir(layout.buildDirectory.dir("generated/resources"))
}

application {
    mainClass.set("drift.DriftReplKt")
}