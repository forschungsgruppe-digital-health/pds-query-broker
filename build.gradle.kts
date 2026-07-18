plugins {
    id("org.springframework.boot") version "4.1.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    java
}

// Single repo-wide version, owned by release-please (ADR-010).
val repoVersion = file("version.txt").readText().trim()

allprojects {
    group = "de.tudresden.fgdh.querybroker"
    version = repoVersion
}

subprojects {
    apply(plugin = "java")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    repositories {
        mavenCentral()
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        // docker-java's fallback API version (1.32) is below the minimum of
        // Docker Engine 25+ (1.40+); pin one both local and CI daemons accept.
        systemProperty("api.version", "1.44")
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}
