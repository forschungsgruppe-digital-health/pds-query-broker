plugins {
    `java-library`
}

val hapiFhirVersion: String by project

dependencies {
    api("ca.uhn.hapi.fhir:hapi-fhir-base:$hapiFhirVersion")
    api("ca.uhn.hapi.fhir:hapi-fhir-structures-r4:$hapiFhirVersion")
    implementation("org.slf4j:slf4j-api:2.0.16")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.26.3")
}
