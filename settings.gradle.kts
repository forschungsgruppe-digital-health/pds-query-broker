plugins {
    // Auto-provisions the Java 21 toolchain when it is not installed locally
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "pds-query-broker"

include("connector-sdk")
include("broker")
include("connectors:pds-example")
