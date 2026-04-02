plugins {
    kotlin("jvm")
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
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}