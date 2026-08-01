plugins {
    kotlin("jvm") version "2.2.0"
}

group = "fr.belic.drift"
version = "2026.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("fr.belic.drift:drift-analysis:2026.0")
    implementation("fr.belic.drift:drift-core:2026.0")
    implementation("fr.belic.drift:drift-hir:2026.0")
    implementation("fr.belic.drift:drift-common:2026.0")

    implementation("fr.belic.drift:drift-jvm:2026.0")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}