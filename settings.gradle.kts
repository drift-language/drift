plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "drift"


include("drift-common")
include("drift-cli")
include("drift-core")
