plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "drift"


include("drift-common")
include("drift-core")
include("drift-ir")
include("drift-hir")
include("drift-analysis")

includeBuild("drift-cli")
includeBuild("drift-bootstrap")
// NOTE: bootstrap must lives in its dedicated runtime.