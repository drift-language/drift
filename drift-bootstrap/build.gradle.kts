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

    implementation(project(":drift-analysis"))
    implementation(project(":drift-core"))
    implementation(project(":drift-hir"))
    implementation(project(":drift-common"))

    implementation("fr.belic.drift:drift-jvm:2026.0")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}