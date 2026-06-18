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
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}