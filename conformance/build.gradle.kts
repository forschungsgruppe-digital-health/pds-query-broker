plugins {
    `java-library`
}

val hapiFhirVersion: String by project

dependencies {
    api(project(":connector-sdk"))
    api("ca.uhn.hapi.fhir:hapi-fhir-validation:$hapiFhirVersion")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-validation-resources-r4:$hapiFhirVersion")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-caching-caffeine:$hapiFhirVersion")
    // The abstract conformance base class is part of the library surface.
    api(platform("org.junit:junit-bom:5.11.4"))
    api("org.junit.jupiter:junit-jupiter")
    api("org.assertj:assertj-core:3.26.3")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
