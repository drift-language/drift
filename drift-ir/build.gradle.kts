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

    implementation(project(":drift-core"))
    implementation(project(":drift-hir"))
    // implementation("fr.belic.drift:qbe-kt:2026.0")  // TODO: Add when qbe-kt module is available

//    testImplementation(platform("org.junit:junit-bom:5.10.0"))
//    testImplementation("org.junit.jupiter:junit-jupiter")
//    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
//    implementation(kotlin("stdlib-jdk8"))
}

tasks.test {
    useJUnitPlatform()
}