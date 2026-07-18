plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

val hapiFhirVersion: String by project

dependencies {
    implementation(project(":connector-sdk"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    testImplementation(project(":connectors:pds-example"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    // Newer than the Boot-managed version: required for the Docker Engine 28+ API floor.
    testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.5"))
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-rabbitmq")
}
