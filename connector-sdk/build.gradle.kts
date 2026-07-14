plugins {
    `java-library`
}

val hapiFhirVersion: String by project

dependencies {
    api("ca.uhn.hapi.fhir:hapi-fhir-base:$hapiFhirVersion")
    api("ca.uhn.hapi.fhir:hapi-fhir-structures-r4:$hapiFhirVersion")
    implementation("org.slf4j:slf4j-api:2.0.16")

    // Runtime profile validation (CatalogProfileValidator). implementation scope:
    // available at runtime for connectors, not leaked onto their compile classpath.
    implementation("ca.uhn.hapi.fhir:hapi-fhir-validation:$hapiFhirVersion")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-validation-resources-r4:$hapiFhirVersion")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-caching-caffeine:$hapiFhirVersion")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.26.3")
}
