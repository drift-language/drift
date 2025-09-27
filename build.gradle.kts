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