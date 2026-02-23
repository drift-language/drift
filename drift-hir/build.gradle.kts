plugins {
    kotlin("jvm") version "2.2.0"

    application
    `maven-publish`
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

    implementation(project(":drift-core"))
    implementation(project(":drift-analysis"))
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "fr.belic.drift"
            artifactId = "drift-hir"
            version = "2026.0"

            from(components["java"])
        }
    }
}